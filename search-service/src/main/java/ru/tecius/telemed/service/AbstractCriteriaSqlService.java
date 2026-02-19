package ru.tecius.telemed.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.tecius.telemed.common.CriteriaInfoInterface;
import ru.tecius.telemed.configuration.CriteriaJoinType;
import ru.tecius.telemed.configuration.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.FieldType;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;
import ru.tecius.telemed.exception.ValidationException;

/**
 * Абстрактный базовый класс для построения запросов через JPA Criteria API.
 * Предоставляет общую логику для создания динамических запросов с поддержкой:
 * - Динамических условий поиска
 * - Сортировки
 * - Пагинации
 * - Fetch joins для оптимизации загрузки связанных сущностей
 * - Поддержки коллекций через явные JOIN
 */
public abstract class AbstractCriteriaSqlService<E> {

  protected final EntityManager entityManager;
  protected final CriteriaInfoInterface<E> criteriaInfo;

  /**
   * Контекст для хранения созданных joins и fetches в рамках выполнения запроса.
   */
  protected static class JoinContext {
    private final Map<String, Join<?, ?>> joins = new LinkedHashMap<>();
    private final Map<String, Fetch<?, ?>> fetches = new LinkedHashMap<>();
    private final Set<String> processedPaths = new LinkedHashSet<>();

    public Join<?, ?> getJoin(String path) {
      return joins.get(path);
    }

    public Fetch<?, ?> getFetch(String path) {
      return fetches.get(path);
    }

    public void addJoin(String path, Join<?, ?> join) {
      joins.put(path, join);
      processedPaths.add(path);
    }

    public void addFetch(String path, Fetch<?, ?> fetch) {
      fetches.put(path, fetch);
      processedPaths.add(path);
    }

    public boolean hasJoin(String path) {
      return processedPaths.contains(path);
    }

    public boolean hasJoinInJoins(String path) {
      return joins.containsKey(path);
    }

    public boolean hasJoinInFetches(String path) {
      return fetches.containsKey(path);
    }

    public Map<String, Join<?, ?>> getJoins() {
      return joins;
    }
  }

  protected AbstractCriteriaSqlService(
      EntityManager entityManager,
      CriteriaInfoInterface<E> criteriaInfo
  ) {
    this.entityManager = entityManager;
    this.criteriaInfo = criteriaInfo;
  }

  /**
   * Выполняет поиск с заданными параметрами.
   *
   * @param searchData    условия поиска
   * @param sort          сортировка
   * @param pagination    пагинация
   * @param fetchPaths    пути к полям для fetch join (например, ["document", "document.attachments"])
   * @return результат поиска с пагинацией
   */
  protected SearchResponseDto<E> search(
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination,
      List<String> fetchPaths
  ) {
    var cb = entityManager.getCriteriaBuilder();

    // Сначала считаем общее количество
    var count = executeCountQuery(cb, searchData);

    // Затем выполняем основной запрос
    var content = executeSearchQuery(cb, searchData, sort, pagination, fetchPaths);

    var pageSize = getPageSize(pagination, 10);
    var totalPages = calculateTotalPages(count, pageSize);
    Boolean moreRows = calculateMoreRows(pagination, totalPages);

    return new SearchResponseDto<>(count.intValue(), totalPages, moreRows, content);
  }

  /**
   * Выполняет count запрос для подсчета общего количества записей.
   */
  private Long executeCountQuery(CriteriaBuilder cb, List<SearchDataDto> searchData) {
    var criteriaQuery = cb.createQuery(Long.class);
    var root = criteriaQuery.from(criteriaInfo.getEntityClass());
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

  /**
   * Выполняет основной поисковый запрос.
   */
  private List<E> executeSearchQuery(
      CriteriaBuilder cb,
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination,
      List<String> fetchPaths
  ) {
    var criteriaQuery = cb.createQuery(criteriaInfo.getEntityClass());
    var root = criteriaQuery.from(criteriaInfo.getEntityClass());
    var joinContext = new JoinContext();

    // Добавляем fetch joins для оптимизации
    if (isNotEmpty(fetchPaths)) {
      addFetchJoins(root, fetchPaths, joinContext);
    }

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

  /**
   * Добавляет fetch joins для оптимизации загрузки связанных сущностей.
   * Fetch joins используются для загрузки связанных данных в одном запросе,
   * избегая проблемы N+1.
   */
  private void addFetchJoins(Root<E> root, List<String> fetchPaths, JoinContext joinContext) {
    for (var fetchPath : fetchPaths) {
      // Разбиваем путь на сегменты: "document.attachments" -> ["document", "attachments"]
      var segments = fetchPath.split("\\.");

      FetchParent<?, ?> currentFetchParent = root;
      var currentPath = "";

      for (var i = 0; i < segments.length; i++) {
        var segment = segments[i];
        currentPath = currentPath.isEmpty() ? segment : currentPath + "." + segment;

        // Проверяем, не добавляли ли мы уже такой fetch
        if (!joinContext.hasJoin(currentPath)) {
          // Добавляем fetch join
          var fetch = currentFetchParent.fetch(segment, JoinType.LEFT);
          joinContext.addFetch(currentPath, fetch);
          currentFetchParent = fetch;
        } else {
          // Используем уже созданный fetch
          var existingFetch = joinContext.getFetch(currentPath);
          if (existingFetch != null) {
            currentFetchParent = existingFetch;
          } else {
            // Если это join (не fetch), используем его как FetchParent
            var existingJoin = joinContext.getJoin(currentPath);
            if (existingJoin != null) {
              currentFetchParent = existingJoin;
            }
          }
        }
      }
    }
  }

  /**
   * Проверяет, есть ли среди joins joins к коллекциям (@OneToMany, @ManyToMany).
   * Такие joins могут привести к дублированию результатов.
   */
  private boolean hasCollectionJoins(JoinContext joinContext) {
    // Для упрощения проверяем, есть ли joins по определенным путям
    // В реальном приложении можно использовать метаданные JPA для точной проверки
    return joinContext.joins.keySet().stream()
        .anyMatch(path -> path.contains("attachments") || path.contains("comments") || path.contains("children"));
  }

  /**
   * Добавляет joins, необходимые для условий поиска.
   */
  private void addJoinsForSearch(Root<E> root, List<SearchDataDto> searchData, JoinContext joinContext) {
    if (isNotEmpty(searchData)) {
      searchData.forEach(data -> {
        var attr = criteriaInfo.getCriteriaAttributeByJsonField(data.attribute())
            .orElse(null);
        if (attr != null && attr.requiresJoin()) {
          addJoinsFromAttribute(root, attr, joinContext);
        }
      });
    }
  }

  /**
   * Добавляет joins, необходимые для сортировки.
   */
  private void addJoinsForSort(Root<E> root, LinkedList<SortDto> sort, JoinContext joinContext) {
    if (isNotEmpty(sort)) {
      for (var sortDto : sort) {
        var attr = criteriaInfo.getCriteriaAttributeByJsonField(sortDto.attribute())
            .orElse(null);
        if (attr != null && attr.requiresJoin()) {
          addJoinsFromAttribute(root, attr, joinContext);
        }
      }
    }
  }

  /**
   * Добавляет joins, описанные в атрибуте, и сохраняет их в контексте.
   * Поддерживает цепочку joins для навигации по коллекциям.
   */
  private void addJoinsFromAttribute(Root<E> root, CriteriaSearchAttribute attr, JoinContext joinContext) {
    if (!attr.requiresJoin()) {
      return;
    }

    From<?, ?> currentFrom = root;
    var currentPath = "";

    for (var joinInfo : attr.joinInfo()) {
      var joinPath = joinInfo.path();
      currentPath = currentPath.isEmpty() ? joinPath : currentPath + "." + joinPath;

      // Проверяем, не добавляли ли мы уже такой join
      if (!joinContext.hasJoin(currentPath)) {
        var joinType = mapJoinType(joinInfo.type());
        var join = currentFrom.join(joinPath, joinType);
        joinContext.addJoin(currentPath, join);
        currentFrom = join;
      } else {
        // Используем уже созданный join
        currentFrom = joinContext.getJoin(currentPath);
      }
    }
  }

  /**
   * Преобразует CriteriaJoinType в JPA JoinType.
   */
  private JoinType mapJoinType(CriteriaJoinType type) {
    return switch (type) {
      case INNER -> JoinType.INNER;
      case LEFT -> JoinType.LEFT;
      case RIGHT -> JoinType.RIGHT;
    };
  }

  /**
   * Строит предикаты для условий поиска.
   */
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

  /**
   * Строит одиночный предикат для условия поиска.
   */
  private Predicate buildPredicate(
      CriteriaBuilder cb,
      Root<E> root,
      SearchDataDto searchData,
      JoinContext joinContext
  ) {
    var attribute = criteriaInfo.getCriteriaAttributeByJsonField(searchData.attribute())
        .orElseThrow(() -> new ValidationException(
            "Фильтрация по атрибуту %s запрещена".formatted(searchData.attribute())));

    var path = buildPathFromAttribute(root, attribute, joinContext);
    var operator = searchData.operator();
    var values = searchData.value();

    return buildPredicateForOperator(cb, path, operator, values, attribute.fieldType());
  }

  /**
   * Строит путь к полю для Criteria API на основе атрибута и созданных joins.
   * Использует информацию о joins из конфигурации атрибута.
   */
  @SuppressWarnings("unchecked")
  private Path<?> buildPathFromAttribute(Root<E> root, CriteriaSearchAttribute attribute, JoinContext joinContext) {
    var fullPath = attribute.getFullPath();
    var segments = fullPath.split("\\.");

    if (segments.length == 1) {
      // Простой путь без joins
      return root.get(segments[0]);
    }

    // Если атрибут имеет joinInfo, используем последний join для получения поля
    if (attribute.requiresJoin()) {
      var joinsList = new ArrayList<>(attribute.joinInfo());
      if (!joinsList.isEmpty()) {
        // Берем последний join (он соответствует самой глубокой вложенности)
        var lastJoinInfo = joinsList.getLast();

        // Строим путь к последнему join
        var currentPath = "";
        for (var joinInfo : joinsList) {
          currentPath = currentPath.isEmpty() ? joinInfo.path() : currentPath + "." + joinInfo.path();
        }

        // Если такой join был создан, используем его
        if (joinContext.hasJoinInJoins(currentPath)) {
          var join = joinContext.getJoin(currentPath);
          // Получаем поле из join'а
          return join.get(segments[segments.length - 1]);
        }
      }
    }

    // Fallback: пробуем обычную навигацию для @ManyToOne/@OneToOne
    Path<?> path = root;
    for (var i = 0; i < segments.length - 1; i++) {
      path = path.get(segments[i]);
    }
    return path.get(segments[segments.length - 1]);
  }

  /**
   * Строит предикат для оператора.
   */
  @SuppressWarnings("unchecked,rawtypes")
  private Predicate buildPredicateForOperator(
      CriteriaBuilder cb,
      Path<?> path,
      Operator operator,
      List<String> values,
      FieldType fieldType
  ) {
    var transformedValues = operator.getTransformValueFunction().apply(values, fieldType);

    return switch (operator) {
      case EQUAL -> cb.equal(path, convertValueForCriteria(transformedValues.getFirst(), fieldType));
      case NOT_EQUAL -> cb.notEqual(path, convertValueForCriteria(transformedValues.getFirst(), fieldType));
      case IN -> path.in(convertedValuesForCriteria(transformedValues, fieldType));
      case CONTAIN -> cb.like(path.as(String.class), transformedValues.getFirst().toString());
      case EXCLUDE -> cb.notLike(path.as(String.class), transformedValues.getFirst().toString());
      case BEGIN -> cb.like(path.as(String.class), transformedValues.getFirst().toString());
      case NOT_BEGIN -> cb.notLike(path.as(String.class), transformedValues.getFirst().toString());
      case END -> cb.like(path.as(String.class), transformedValues.getFirst().toString());
      case NOT_END -> cb.notLike(path.as(String.class), transformedValues.getFirst().toString());
      case IS_NULL -> cb.isNull(path);
      case IS_NOT_NULL -> cb.isNotNull(path);
      case BETWEEN -> {
        if (fieldType == FieldType.STRING) {
          yield cb.between(path.as(String.class), transformedValues.get(0), transformedValues.get(1));
        }
        // Для дат парсим в объекты даты
        if (fieldType == FieldType.OFFSET_DATE_TIME || fieldType == FieldType.LOCAL_DATE_TIME || fieldType == FieldType.LOCAL_DATE) {
          yield cb.between(
              (Path) path,
              parseDateValue(transformedValues.get(0), fieldType),
              parseDateValue(transformedValues.get(1), fieldType)
          );
        }
        // Для чисел используем преобразованные значения
        yield cb.between(
            (Path) path,
            (Comparable) convertValueForCriteria(transformedValues.get(0), fieldType),
            (Comparable) convertValueForCriteria(transformedValues.get(1), fieldType)
        );
      }
      case MORE_OR_EQUAL -> {
        if (fieldType == FieldType.STRING) {
          yield cb.greaterThanOrEqualTo(path.as(String.class), transformedValues.getFirst());
        }
        // Для дат парсим в объекты даты
        if (fieldType == FieldType.OFFSET_DATE_TIME || fieldType == FieldType.LOCAL_DATE_TIME || fieldType == FieldType.LOCAL_DATE) {
          yield cb.greaterThanOrEqualTo(
              (Path) path,
              parseDateValue(transformedValues.getFirst(), fieldType)
          );
        }
        // Для чисел используем преобразованные значения
        yield cb.greaterThanOrEqualTo(
            (Path) path,
            (Comparable) convertValueForCriteria(transformedValues.getFirst(), fieldType)
        );
      }
      case LESS_OR_EQUAL -> {
        if (fieldType == FieldType.STRING) {
          yield cb.lessThanOrEqualTo(path.as(String.class), transformedValues.getFirst());
        }
        // Для дат парсим в объекты даты
        if (fieldType == FieldType.OFFSET_DATE_TIME || fieldType == FieldType.LOCAL_DATE_TIME || fieldType == FieldType.LOCAL_DATE) {
          yield cb.lessThanOrEqualTo(
              (Path) path,
              parseDateValue(transformedValues.getFirst(), fieldType)
          );
        }
        // Для чисел используем преобразованные значения
        yield cb.lessThanOrEqualTo(
            (Path) path,
            (Comparable) convertValueForCriteria(transformedValues.getFirst(), fieldType)
        );
      }
    };
  }

  /**
   * Преобразует строковое значение в соответствующий тип для Criteria API.
   */
  private Object convertValueForCriteria(String value, FieldType fieldType) {
    if (value == null) {
      return null;
    }

    return switch (fieldType) {
      case NUMERIC -> {
        try {
          yield Long.parseLong(value);
        } catch (NumberFormatException e) {
          yield Double.parseDouble(value);
        }
      }
      case BOOLEAN -> Boolean.parseBoolean(value);
      case STRING -> value;
      case OFFSET_DATE_TIME, LOCAL_DATE_TIME, LOCAL_DATE -> value; // Даты преобразуются в transformValues
    };
  }

  /**
   * Преобразует список строковых значений в соответствующие типы для Criteria API.
   */
  private Object[] convertedValuesForCriteria(List<String> values, FieldType fieldType) {
    return values.stream()
        .map(v -> convertValueForCriteria(v, fieldType))
        .toArray();
  }

  /**
   * Парсит строковое значение даты в объект даты для Criteria API.
   */
  @SuppressWarnings("unchecked,rawtypes")
  private Comparable parseDateValue(String value, FieldType fieldType) {
    try {
      return switch (fieldType) {
        case OFFSET_DATE_TIME -> {
          // Пытаемся распарсить дату с timezone
          try {
            // Сначала пробуем стандартный формат ISO 8601
            yield (Comparable) OffsetDateTime.parse(value);
          } catch (Exception e) {
            // Если не получилось, пробуем формат с двоеточием в timezone
            var formatter = ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            // Заменяем двоеточие в timezone для парсинга
            var normalized = value.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            yield (Comparable) OffsetDateTime.parse(normalized, formatter);
          }
        }
        case LOCAL_DATE_TIME -> (Comparable) LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME);
        case LOCAL_DATE -> (Comparable) LocalDate.parse(value, ISO_LOCAL_DATE);
        default -> throw new IllegalArgumentException("Unsupported date type: " + fieldType);
      };
    } catch (Exception e) {
      throw new ValidationException("Ошибка парсинга даты: %s для типа %s".formatted(value, fieldType), e);
    }
  }

  /**
   * Строит условия сортировки.
   */
  private List<Order> buildOrders(
      CriteriaBuilder cb,
      Root<E> root,
      LinkedList<SortDto> sort,
      JoinContext joinContext
  ) {
    var orders = new ArrayList<Order>();

    for (var sortDto : sort) {
      var attribute = criteriaInfo.getCriteriaAttributeByJsonField(sortDto.attribute())
          .orElseThrow(() -> new ValidationException(
              "Сортировка по атрибуту %s запрещена".formatted(sortDto.attribute())));

      var path = buildPathFromAttribute(root, attribute, joinContext);

      var order = sortDto.direction() == ru.tecius.telemed.dto.request.Direction.ASC
          ? cb.asc(path)
          : cb.desc(path);

      orders.add(order);
    }

    return orders;
  }

  /**
   * Добавляет пагинацию к запросу.
   */
  private void addPagination(TypedQuery<E> query, PaginationDto pagination) {
    if (nonNull(pagination) && nonNull(pagination.page())) {
      var pageSize = pagination.size();
      var offset = pagination.page() * pageSize;

      query.setFirstResult(offset);
      query.setMaxResults(pageSize);
    }
  }

  private Integer getPageSize(PaginationDto pagination, Integer defaultPageSize) {
    return nonNull(pagination) && nonNull(pagination.page()) ? pagination.size() : defaultPageSize;
  }

  private int calculateTotalPages(Long totalElements, int pageSize) {
    return totalElements != null
        ? (int) Math.ceil((double) totalElements / pageSize)
        : 0;
  }

  private boolean calculateMoreRows(PaginationDto pagination, int totalPages) {
    return pagination != null && pagination.page() != null
        && (pagination.page() + 1) < totalPages;
  }
}
