package ru.tecius.telemed.service.criteria;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.exception.ValidationException;

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
          dto -> criteriaInfoInterface.getCriteriaAttributeByJsonField(
                  dto.attribute())
              .filter(CriteriaSearchAttribute::requiresJoin)
              .ifPresent(attr -> addJoinsFromAttribute(root, attr, joinContext)));
    }
  }

  private void addJoinsForSort(Root<E> root, LinkedList<SortDto> sort, JoinContext joinContext) {
    if (isNotEmpty(sort)) {
      sort.forEach(dto -> criteriaInfoInterface.getCriteriaAttributeByJsonField(
              dto.attribute())
          .filter(CriteriaSearchAttribute::requiresJoin)
          .ifPresent(attr -> addJoinsFromAttribute(root, attr, joinContext)));
    }
  }

  private void addJoinsFromAttribute(Root<E> root, CriteriaSearchAttribute attr,
      JoinContext joinContext) {
    From<?, ?> currentFrom = root;
    var currentPath = EMPTY;

    for (var joinInfo : attr.joinInfo()) {
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
    var attribute = criteriaInfoInterface.getCriteriaAttributeByJsonField(searchData.attribute())
        .orElseThrow(() -> new ValidationException(
            "Фильтрация по атрибуту %s запрещена".formatted(searchData.attribute())));

    var path = buildPathFromAttribute(root, attribute, joinContext);
    var operator = searchData.operator();
    var values = searchData.value();

    return buildPredicateForOperator(cb, path, operator, values, attribute.fieldType());
  }

  @SuppressWarnings("unchecked")
  private Path<?> buildPathFromAttribute(Root<E> root, CriteriaSearchAttribute attribute,
      JoinContext joinContext) {
    var entityPath = attribute.entityPath();
    var segments = entityPath.split("\\.");

    if (Objects.equals(segments.length, 1)) {
      // Простой путь без joins
      return root.get(segments[0]);
    }

    // Если атрибут имеет joinInfo, используем последний join для получения поля
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

    // Fallback: пробуем обычную навигацию для @ManyToOne/@OneToOne
    Path<?> path = root;
    for (var i = 0; i < segments.length - 1; i++) {
      path = path.get(segments[i]);
    }
    return path.get(segments[segments.length - 1]);
  }

  @SuppressWarnings("unchecked,rawtypes")
  private Predicate buildPredicateForOperator(
      CriteriaBuilder cb,
      Path<?> path,
      Operator operator,
      List<String> values,
      Class<?> fieldType
  ) {
    operator.checkValue(values);

    var transformedValues = operator.getTransformValueFunction().apply(values, fieldType);

    return switch (operator) {
      case EQUAL ->
          cb.equal(path, convertValueForCriteria(transformedValues.getFirst(), fieldType));
      case NOT_EQUAL ->
          cb.notEqual(path, convertValueForCriteria(transformedValues.getFirst(), fieldType));
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
        if (Objects.equals(fieldType, String.class)) {
          yield cb.between(path.as(String.class), transformedValues.get(0),
              transformedValues.get(1));
        }
        // Для дат парсим в объекты даты
        if (List.of(OffsetDateTime.class, LocalDateTime.class, LocalDate.class)
            .contains(fieldType)) {
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
        if (Objects.equals(fieldType, String.class)) {
          yield cb.greaterThanOrEqualTo(path.as(String.class), transformedValues.getFirst());
        }
        // Для дат парсим в объекты даты
        if (List.of(OffsetDateTime.class, LocalDateTime.class, LocalDate.class)
            .contains(fieldType)) {
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
        if (Objects.equals(fieldType, String.class)) {
          yield cb.lessThanOrEqualTo(path.as(String.class), transformedValues.getFirst());
        }
        // Для дат парсим в объекты даты
        if (List.of(OffsetDateTime.class, LocalDateTime.class, LocalDate.class)
            .contains(fieldType)) {
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

  private Object convertValueForCriteria(String value, Class<?> fieldType) {
    if (value == null) {
      return null;
    }

    return switch (fieldType) {
      case Class<?> c when c == Long.class -> Long.parseLong(value);
      case Class<?> c when c == Integer.class -> Integer.parseInt(value);
      case Class<?> c when c == Double.class -> Double.parseDouble(value);
      case Class<?> c when c == Float.class -> Float.parseFloat(value);
      case Class<?> c when c == Boolean.class -> Boolean.parseBoolean(value);
      default -> value;
    };
  }

  private Object[] convertedValuesForCriteria(List<String> values, Class<?> fieldType) {
    return values.stream()
        .map(v -> convertValueForCriteria(v, fieldType))
        .toArray();
  }

  @SuppressWarnings("unchecked,rawtypes")
  private Comparable parseDateValue(String value, Class<?> fieldType) {
    try {
      return switch (fieldType) {
        case Class<?> c when c == OffsetDateTime.class -> {
          // Пытаемся распарсить дату с timezone
          try {
            // Сначала пробуем стандартный формат ISO 8601
            yield OffsetDateTime.parse(value);
          } catch (Exception e) {
            // Если не получилось, пробуем формат с двоеточием в timezone
            var formatter = ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            // Заменяем двоеточие в timezone для парсинга
            var normalized = value.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            yield OffsetDateTime.parse(normalized, formatter);
          }
        }
        case Class<?> c when c == LocalDateTime.class -> LocalDateTime.parse(value,
            ISO_LOCAL_DATE_TIME);
        case Class<?> c when c == LocalDate.class -> LocalDate.parse(value, ISO_LOCAL_DATE);
        default -> throw new IllegalArgumentException("Unsupported date type: " + fieldType);
      };
    } catch (Exception e) {
      throw new ValidationException(
          "Ошибка парсинга даты: %s для типа %s".formatted(value, fieldType), e);
    }
  }

  private List<Order> buildOrders(
      CriteriaBuilder cb,
      Root<E> root,
      LinkedList<SortDto> sort,
      JoinContext joinContext
  ) {
    var orders = new ArrayList<Order>();

    for (var sortDto : sort) {
      var attribute = criteriaInfoInterface.getCriteriaAttributeByJsonField(sortDto.attribute())
          .orElseThrow(() -> new ValidationException(
              "Сортировка по атрибуту %s запрещена".formatted(sortDto.attribute())));

      var path = buildPathFromAttribute(root, attribute, joinContext);

      var order = sortDto.direction() == ASC
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
