package ru.tecius.telemed.criteria.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static ru.tecius.telemed.configuration.common.AttributeType.SIMPLE;
import static ru.tecius.telemed.dto.request.Direction.DESC;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.tecius.telemed.common.criteria.CriteriaInfoInterface;
import ru.tecius.telemed.common.criteria.HintName;
import ru.tecius.telemed.common.criteria.PathWithValue;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.criteria.JoinInfo;
import ru.tecius.telemed.criteria.context.JoinContext;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.exception.ValidationException;

public abstract class AbstractCriteriaSqlService<E> {

  protected final EntityManager entityManager;
  private final CriteriaInfoInterface<E> criteriaInfoInterface;
  private final Long defaultPageSize;

  protected AbstractCriteriaSqlService(
      EntityManager entityManager,
      CriteriaInfoInterface<E> criteriaInfoInterface,
      Long defaultPageSize
  ) {
    this.entityManager = entityManager;
    this.criteriaInfoInterface = criteriaInfoInterface;
    this.defaultPageSize = defaultPageSize;
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
      PaginationDto pagination,
      HintName hintName,
      Set<String> entityGraphs
  ) {
    var criteriaQuery = cb.createQuery(criteriaInfoInterface.getEntityClass());
    var root = criteriaQuery.from(criteriaInfoInterface.getEntityClass());
    var joinContext = new JoinContext();

    // 1. Сначала добавляем FETCH-джойны для сортировки (они попадут в SELECT)
    addFetchJoinsForSort(root, sort, joinContext);

    // 2. Затем добавляем обычные джойны для поиска (те, которых еще нет в контексте)
    addJoinsForSearch(root, searchData, joinContext);

    // Используем DISTINCT только если есть joins к коллекциям
    criteriaQuery.distinct(joinContext.hasCollectionJoins());
    criteriaQuery.select(root);

    // Добавляем условия поиска
    var predicates = buildPredicates(cb, root, searchData, joinContext);
    if (!predicates.isEmpty()) {
      criteriaQuery.where(predicates.toArray(new Predicate[0]));
    }

    // Добавляем сортировку
    if (isNotEmpty(sort)) {
      var orders = buildOrders(cb, root, sort, joinContext);
      if (!orders.isEmpty()) {
        criteriaQuery.orderBy(orders);
      }
    }

    // Создаем запрос
    var query = entityManager.createQuery(criteriaQuery);

    // Применяем entity graph для загрузки связанных сущностей
    applyEntityGraph(query, hintName, entityGraphs, joinContext);

    // Добавляем пагинацию
    addPagination(query, pagination);
    return query.getResultList();
  }

  protected Long getPageSize(PaginationDto pagination) {
    return nonNull(pagination) && nonNull(pagination.page()) ? pagination.size() : defaultPageSize;
  }

  protected Long calculateTotalPages(Long totalElements, Long pageSize) {
    return nonNull(totalElements)
        ? (long) Math.ceil((double) totalElements / pageSize)
        : 0L;
  }

  protected boolean calculateMoreRows(PaginationDto pagination, Long totalPages) {
    return nonNull(pagination) && nonNull(pagination.page())
        && (pagination.page() + 1) < totalPages;
  }

  private void addFetchJoinsForSort(Root<E> root, LinkedList<SortDto> sort,
      JoinContext joinContext) {
    if (isNotEmpty(sort)) {
      sort.forEach(dto -> criteriaInfoInterface.getMultipleAttributeByJsonKey(dto.attribute())
          .ifPresent(attr -> addFetchFromAttribute(root, attr, joinContext))); //
    }
  }

  private void addFetchFromAttribute(Root<E> root, CriteriaSearchAttribute attr,
      JoinContext joinContext) {
    FetchParent<?, ?> currentFetch = root;
    var currentPath = EMPTY;

    for (var joinInfo : attr.db().joinInfo()) {
      // Проверка, что атрибут не является коллекцией
      if (isCollectionJoin((From<?, ?>) currentFetch, joinInfo.path())) {
        throw new ValidationException("Сортировка по атрибуту %s запрещена"
            .formatted(attr.json().key()));
      }

      currentPath = getCurrentPath(currentPath, joinInfo);

      // ВАЖНО: Мы НЕ проверяем joinContext.hasJoin(currentPath).
      // Мы всегда вызываем fetch(), чтобы поля попали в SELECT.
      // Hibernate объединит fetch с существующим join, если это возможно.
      var fetch = currentFetch.fetch(joinInfo.path(), JoinType.LEFT);

      if (fetch instanceof Join<?, ?> join) {
        // Обновляем/добавляем в контекст, чтобы buildOrders мог найти этот Path
        joinContext.addJoin(currentPath, join);
      }

      currentFetch = fetch;
    }
  }

  private void addJoinsForSearch(Root<E> root, List<SearchDataDto> searchData,
      JoinContext joinContext) {
    if (isNotEmpty(searchData)) {
      searchData.forEach(
          dto -> criteriaInfoInterface.getMultipleAttributeByJsonKey(dto.attribute())
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

      // Проверяем, не добавляли ли мы уже такой join (включая fetched joins)
      if (joinContext.hasNotJoin(currentPath)) {
        var join = currentFrom.join(joinPath, joinInfo.type());
        joinContext.addJoin(currentPath, join);
        // Определяем, является ли этот join коллекцией (@OneToMany, @ManyToMany, @ElementCollection)
        if (isCollectionJoin(currentFrom, joinPath)) {
          joinContext.markAsCollectionJoin(currentPath);
        }

        currentFrom = join;
      } else {
        // Используем уже созданный join (он может быть fetch join)
        currentFrom = joinContext.getJoin(currentPath);
      }
    }
  }

  /**
   * Проверяет, является ли join к полю коллекцией. Коллекциями считаются ассоциации @OneToMany,
   *
   * @ManyToMany и @ElementCollection.
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

  private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<E> root,
      List<SearchDataDto> searchData, JoinContext joinContext) {
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

  private Predicate buildPredicate(CriteriaBuilder cb, Root<E> root, SearchDataDto searchData,
      JoinContext joinContext) {
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

  private Predicate buildPredicateForOperator(CriteriaBuilder cb, Path<?> path, Operator operator,
      List<String> values, Class<?> fieldType) {
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
    var orders = new LinkedList<Order>();

    for (var sortDto : sort) {
      var attribute = sortDto.attribute();
      var attr = criteriaInfoInterface.getAttributeByJsonKey(attribute,
          "Сортировка по атрибуту %s запрещена".formatted(attribute));

      var path = buildPathFromAttribute(root, attr, joinContext);

      var order = Objects.equals(sortDto.direction(), DESC)
          ? cb.desc(path)
          : cb.asc(path);

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

  protected void applyEntityGraph(TypedQuery<E> query, HintName hintName,
      Set<String> entityGraphs,
      JoinContext joinContext) {
    if (isNotEmpty(entityGraphs)) {
      var graph = entityManager.createEntityGraph(
          criteriaInfoInterface.getEntityClass());

      entityGraphs.forEach(entityGraph -> {
        // Проверяем, не был ли этот путь уже загружен через fetch join
        if (joinContext.hasNotJoin(entityGraph)) {
          addSubgraphToEntityGraph(graph, entityGraph);
        }
      });

      query.setHint(nonNull(hintName) ? hintName.getValue() : HINT_SPEC_FETCH_GRAPH, graph);
    }
  }

  private void addSubgraphToEntityGraph(EntityGraph<?> graph, String graphPath) {
    var parts = graphPath.split("\\.");

    if (Objects.equals(parts.length, 1)) {
      graph.addAttributeNodes(parts[0]);
    } else {
      Object currentGraph = graph;
      for (var i = 0; i < parts.length - 1; i++) {
        if (currentGraph instanceof EntityGraph<?>) {
          currentGraph = ((EntityGraph<?>) currentGraph).addSubgraph(parts[i]);
        } else {
          currentGraph = ((Subgraph<?>) currentGraph).addSubgraph(parts[i]);
        }
      }

      if (currentGraph instanceof EntityGraph<?>) {
        ((EntityGraph<?>) currentGraph).addAttributeNodes(parts[parts.length - 1]);
      } else {
        ((Subgraph<?>) currentGraph).addAttributeNodes(parts[parts.length - 1]);
      }
    }
  }
}
