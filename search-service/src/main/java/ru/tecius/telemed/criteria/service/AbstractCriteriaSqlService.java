package ru.tecius.telemed.criteria.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.tecius.telemed.configuration.common.AttributeType.SIMPLE;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tecius.telemed.common.criteria.CriteriaInfoInterface;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.criteria.JoinInfo;
import ru.tecius.telemed.criteria.context.JoinContext;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.PathWithValue;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;

public abstract class AbstractCriteriaSqlService<E> {

  private static final Logger log = LoggerFactory.getLogger(AbstractCriteriaSqlService.class);

  protected final EntityManager entityManager;
  protected final CriteriaInfoInterface<E> criteriaInfoInterface;

  protected AbstractCriteriaSqlService(
      EntityManager entityManager,
      CriteriaInfoInterface<E> criteriaInfoInterface
  ) {
    this.entityManager = entityManager;
    this.criteriaInfoInterface = criteriaInfoInterface;
  }

  protected Long executeCountQuery(CriteriaBuilder cb, List<SearchDataDto> searchData) {
    var criteriaQuery = cb.createQuery(Long.class);
    var root = criteriaQuery.from(criteriaInfoInterface.getEntityClass());
    var joinContext = new JoinContext();

    // Добавляем необходимые joins для фильтрации
    addJoinsForSearch(root, searchData, joinContext);

    criteriaQuery.select(cb.count(root));

    // Добавляем условия поиска
    var predicates = buildPredicates(cb, root, searchData, joinContext);
    if (!predicates.isEmpty()) {
      criteriaQuery.where(predicates.toArray(new Predicate[0]));
    }

    return entityManager.createQuery(criteriaQuery).getSingleResult();
  }

  protected List<E> executeSearchQuery(
      CriteriaBuilder cb,
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination
  ) {
    var criteriaQuery = cb.createQuery(criteriaInfoInterface.getEntityClass());
    var root = criteriaQuery.from(criteriaInfoInterface.getEntityClass());
    var joinContext = new JoinContext();

    // Добавляем необходимые joins для фильтрации и сортировки
    addJoinsForSearch(root, searchData, joinContext);
    addJoinsForSort(root, sort, joinContext);

    // Используем DISTINCT только если есть joins к коллекциям
    boolean needDistinct = joinContext.hasCollectionJoins();
    criteriaQuery.distinct(needDistinct);
    criteriaQuery.select(root);

    // Добавляем условия поиска
    var predicates = buildPredicates(cb, root, searchData, joinContext);
    if (!predicates.isEmpty()) {
      criteriaQuery.where(predicates.toArray(new Predicate[0]));
    }

    // Добавляем сортировку
    if (isNotEmpty(sort)) {
      var orders = buildOrders(cb, root, sort, joinContext, needDistinct);
      if (!orders.isEmpty()) {
        criteriaQuery.orderBy(orders);
      }
    }

    // Создаем запрос
    var query = entityManager.createQuery(criteriaQuery);

    // Добавляем пагинацию
    addPagination(query, pagination);

    return query.getResultList();
  }



  private void addJoinsForSearch(Root<E> root, List<SearchDataDto> searchData,
      JoinContext joinContext) {
    if (isNotEmpty(searchData)) {
      searchData.forEach(
          dto -> criteriaInfoInterface.getMultipleAttributeByJsonKey(dto.attribute())
              .ifPresent(attr -> addJoinsFromAttribute(root, attr, joinContext)));
    }
  }

  private void addJoinsForSort(Root<E> root, LinkedList<SortDto> sort, JoinContext joinContext) {
    if (isNotEmpty(sort)) {
      sort.forEach(dto -> criteriaInfoInterface.getMultipleAttributeByJsonKey(dto.attribute())
          .ifPresent(attr -> addJoinsFromAttribute(root, attr, joinContext)));
    }
  }

  private void addJoinsFromAttribute(Root<E> root, CriteriaSearchAttribute attr,
      JoinContext joinContext) {
    From<?, ?> currentFrom = root;
    var currentPath = EMPTY;

    for (var joinInfo : attr.db().joinInfo()) {
      currentPath = getCurrentPath(currentPath, joinInfo);
      var joinPath = joinInfo.path();
      // Проверяем, не добавляли ли мы уже такой join
      if (!joinContext.hasJoin(currentPath)) {
        var join = currentFrom.join(joinPath, joinInfo.type());
        joinContext.addJoin(currentPath, join);
        // Определяем, является ли этот join коллекцией (@OneToMany, @ManyToMany, @ElementCollection)
        if (isCollectionJoin(currentFrom, joinPath)) {
          joinContext.markAsCollectionJoin(currentPath);
        }

        currentFrom = join;
      } else {
        // Используем уже созданный join
        currentFrom = joinContext.getJoin(currentPath);
      }
    }
  }

  /**
   * Проверяет, является ли join к полю коллекцией.
   * Коллекциями считаются ассоциации @OneToMany, @ManyToMany и @ElementCollection.
   */
  private boolean isCollectionJoin(From<?, ?> from, String attributeName) {
      var metamodel = entityManager.getMetamodel();
      // Получаем тип сущности, от которой делаем join
      var javaType = from.getJavaType();
      var entityType = metamodel.entity(javaType);
      // Проверяем атрибут
      var attribute = entityType.getAttribute(attributeName);
      return attribute.isCollection();
  }

  private List<Predicate> buildPredicates(
      CriteriaBuilder cb,
      Root<E> root,
      List<SearchDataDto> searchData,
      JoinContext joinContext
  ) {
    var predicates = new ArrayList<Predicate>();

    if (isNotEmpty(searchData)) {
      for (var data : searchData) {
        var predicate = buildPredicate(cb, root, data, joinContext);
        if (predicate != null) {
          predicates.add(predicate);
        }
      }
    }

    return predicates;
  }

  private Predicate buildPredicate(
      CriteriaBuilder cb,
      Root<E> root,
      SearchDataDto searchData,
      JoinContext joinContext
  ) {
    var attribute = searchData.attribute();
    var attr = criteriaInfoInterface.getAttributeByJsonKey(attribute,
            "Фильтрация по атрибуту %s запрещена".formatted(searchData.attribute()));

    var path = buildPathFromAttribute(root, attr, joinContext);

    return buildPredicateForOperator(cb, path, searchData.operator(), searchData.value(),
        attr.db().type());
  }

  private Path<?> buildPathFromAttribute(Root<E> root, CriteriaSearchAttribute attribute,
      JoinContext joinContext) {
    var db = attribute.db();
    if (Objects.equals(attribute.type(), SIMPLE)) {
      return root.get(db.column());
    }

    return joinContext.getJoin(createCurrentPath(db.joinInfo())).get(db.column());
  }

  private Predicate buildPredicateForOperator(
      CriteriaBuilder cb,
      Path<?> path,
      Operator operator,
      List<String> values,
      Class<?> fieldType
  ) {
    operator.checkValue(values);

    var pathWithValue = new PathWithValue(path, values, fieldType);
    return (Predicate) operator.getCriteriaPredicateFunction().apply(cb, pathWithValue);
  }

  private List<Order> buildOrders(
      CriteriaBuilder cb,
      Root<E> root,
      LinkedList<SortDto> sort,
      JoinContext joinContext,
      boolean needDistinct
  ) {
    var orders = new ArrayList<Order>();

    for (var sortDto : sort) {
      var attribute = sortDto.attribute();
      var attr = criteriaInfoInterface.getAttributeByJsonKey(attribute,
              "Сортировка по атрибуту %s запрещена".formatted(attribute));

      // Если нужен DISTINCT и сортировка по MULTIPLE атрибуту (join),
      // пропускаем эту сортировку, так как PostgreSQL требует,
      // чтобы все поля в ORDER BY присутствовали в SELECT при DISTINCT
      if (needDistinct && !Objects.equals(attr.type(), SIMPLE)) {
        log.warn("Сортировка по '{}' пропущена, так как используется DISTINCT (есть joins к коллекциям). "
            + "При DISTINCT сортировка по полям из joined таблиц не поддерживается PostgreSQL", attribute);
        continue;
      }

      var path = buildPathFromAttribute(root, attr, joinContext);

      var order = Objects.equals(sortDto.direction(), ru.tecius.telemed.dto.request.Direction.ASC)
          ? cb.asc(path)
          : cb.desc(path);

      orders.add(order);
    }

    return orders;
  }

  private void addPagination(TypedQuery<E> query, PaginationDto pagination) {
    if (nonNull(pagination) && nonNull(pagination.page())) {
      var pageSize = pagination.size();
      var offset = pagination.page() * pageSize;

      query.setFirstResult(offset);
      query.setMaxResults(pageSize);
    }
  }

  protected Integer getPageSize(PaginationDto pagination, Integer defaultPageSize) {
    return nonNull(pagination) && nonNull(pagination.page()) ? pagination.size() : defaultPageSize;
  }

  protected int calculateTotalPages(Long totalElements, int pageSize) {
    return nonNull(totalElements)
        ? (int) Math.ceil((double) totalElements / pageSize)
        : 0;
  }

  protected boolean calculateMoreRows(PaginationDto pagination, int totalPages) {
    return nonNull(pagination) && nonNull(pagination.page())
        && (pagination.page() + 1) < totalPages;
  }

  private String createCurrentPath(LinkedHashSet<JoinInfo> joinInfo) {
    var currentPath = EMPTY;
    for (var join : joinInfo) {
      currentPath = getCurrentPath(currentPath, join);
    }

    return currentPath;
  }

  private String getCurrentPath(String currentPath, JoinInfo join) {
    return isBlank(currentPath) ? join.path() : currentPath + "." + join.path();
  }
}
