package ru.tecius.telemed.service.criteria;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static ru.tecius.telemed.configuration.common.AttributeType.SIMPLE;
import static ru.tecius.telemed.dto.request.Direction.ASC;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import ru.tecius.telemed.common.criteria.CriteriaInfoInterface;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.PathWithValue;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;

public abstract class AbstractCriteriaSqlService<E> {

  protected final EntityManager entityManager;
  protected final CriteriaInfoInterface<E> criteriaInfoInterface;

  protected static class JoinContext {

    private final Map<String, Join<?, ?>> joins = new LinkedHashMap<>();
    private final Set<String> processedPaths = new LinkedHashSet<>();

    public Join<?, ?> getJoin(String path) {
      return joins.get(path);
    }

    public void addJoin(String path, Join<?, ?> join) {
      joins.put(path, join);
      processedPaths.add(path);
    }

    public boolean hasJoin(String path) {
      return processedPaths.contains(path);
    }

    public boolean hasJoinInJoins(String path) {
      return joins.containsKey(path);
    }

  }

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

    criteriaQuery.select(root);

    // Добавляем условия поиска
    var predicates = buildPredicates(cb, root, searchData, joinContext);
    if (!predicates.isEmpty()) {
      criteriaQuery.where(predicates.toArray(new Predicate[0]));
    }

    // Используем DISTINCT если есть joins к коллекциям (избегаем дубликатов)
    if (hasCollectionJoins(joinContext)) {
      criteriaQuery.distinct(true);
    }

    // Добавляем сортировку
    if (isNotEmpty(sort)) {
      var orders = buildOrders(cb, root, sort, joinContext);
      criteriaQuery.orderBy(orders);
    }

    // Создаем запрос
    var query = entityManager.createQuery(criteriaQuery);

    // Добавляем пагинацию
    addPagination(query, pagination);

    return query.getResultList();
  }

  private boolean hasCollectionJoins(JoinContext joinContext) {
    var metamodel = entityManager.getMetamodel();
    var entityType = metamodel.entity(criteriaInfoInterface.getEntityClass());

    return joinContext.joins.entrySet().stream()
        .anyMatch(entry -> isCollectionJoin(entityType, entry.getKey()));
  }

  private boolean isCollectionJoin(EntityType<?> entityType, String joinPath) {
    var segments = joinPath.split("\\.");

    // Для простого пути (без точек) проверяем только последний сегмент
    if (segments.length == 1) {
      var attribute = entityType.getAttribute(segments[0]);
      return attribute instanceof PluralAttribute<?, ?, ?>;
    }

    // Для составного пути проверяем каждый сегмент
    EntityType<?> currentType = entityType;

    for (int i = 0; i < segments.length; i++) {
      var segment = segments[i];
      var attribute = currentType.getAttribute(segment);

      // Если атрибут коллекция и это последний сегмент - найден collection join
      if (attribute instanceof PluralAttribute<?, ?, ?>) {
        if (i == segments.length - 1) {
          return true;
        }
        // Получаем тип элемента коллекции для продолжения навигации
        var elementType = ((PluralAttribute<?, ?, ?>) attribute).getElementType();
        if (elementType instanceof EntityType<?> et) {
          currentType = et;
        } else {
          // Если элемент коллекции не сущность — дальнейшая навигация невозможна
          return false;
        }
      } else if (attribute instanceof SingularAttribute<?, ?> singularAttr) {
        // Для singular атрибута получаем тип для продолжения навигации
        var type = singularAttr.getType();
        if (type instanceof EntityType<?> et) {
          currentType = et;
        } else {
          // Если тип не сущность — дальнейшая навигация невозможна
          return false;
        }
      }
    }
    return false;
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
      var joinPath = joinInfo.path();
      currentPath = isBlank(currentPath) ? joinPath : currentPath + "." + joinPath;
      // Проверяем, не добавляли ли мы уже такой join
      if (!joinContext.hasJoin(currentPath)) {
        var join = currentFrom.join(joinPath, joinInfo.type());
        joinContext.addJoin(currentPath, join);
        currentFrom = join;
      } else {
        // Используем уже созданный join
        currentFrom = joinContext.getJoin(currentPath);
      }
    }
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

    // Строим путь к последнему join
    var currentPath = EMPTY;
    for (var joinInfo : db.joinInfo()) {
      currentPath = currentPath.isEmpty() ? joinInfo.path() : currentPath + "." + joinInfo.path();
    }

    var join = joinContext.getJoin(currentPath);
    return join.get(db.column());
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
      JoinContext joinContext
  ) {
    var orders = new ArrayList<Order>();

    for (var sortDto : sort) {
      var attribute = sortDto.attribute();
      var attr = criteriaInfoInterface.getAttributeByJsonKey(attribute,
              "Сортировка по атрибуту %s запрещена".formatted(attribute));

      var path = buildPathFromAttribute(root, attr, joinContext);

      var order = Objects.equals(sortDto.direction(), ASC)
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
}
