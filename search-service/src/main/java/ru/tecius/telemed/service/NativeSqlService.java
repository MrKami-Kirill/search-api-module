package ru.tecius.telemed.service;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.tecius.telemed.common.SearchInfoInterface;
import ru.tecius.telemed.configuration.JoinInfo;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public abstract class NativeSqlService<E> {

  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<E> rowMapper;
  private final SearchInfoInterface<E> searchInfoInterface;

  public NativeSqlService(JdbcTemplate jdbcTemplate,
      RowMapper<E> rowMapper,
      SearchInfoInterface<E> searchInfoInterface) {
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = rowMapper;
    this.searchInfoInterface = searchInfoInterface;
  }

  protected SearchResponseDto<E> search(List<SearchDataDto> searchData, SortDto sort,
      PaginationDto pagination) {

    var sqlBuilder = new StringBuilder();
    List<Object> params = new LinkedList<>();

    // SELECT clause
    sqlBuilder.append("SELECT ");

    // FROM clause with joins
    sqlBuilder.append(searchInfoInterface.getFullTableName());

    // Get unique joins for all attributes
    Set<JoinInfo> uniqueJoins = collectUniqueJoins(searchData, sort);

    // Add JOINs
    if (!uniqueJoins.isEmpty()) {
      String joinsSql = uniqueJoins.stream()
          .sorted(Comparator.comparingInt(JoinInfo::order))
          .map(JoinInfo::createJoinString)
          .collect(joining(" "));
      sqlBuilder.append(" ").append(joinsSql);
    }

    // WHERE clause
    List<String> whereConditions = new ArrayList<>();
    if (searchData != null && !searchData.isEmpty()) {
      for (SearchDataDto data : searchData) {
        String attribute = data.attribute();
        Operator operator = data.operator();
        List<String> values = data.value();

        String condition = buildCondition(attribute, operator, values, params);
        if (condition != null) {
          whereConditions.add(condition);
        }
      }
    }

    if (!whereConditions.isEmpty()) {
      sqlBuilder.append(" WHERE ").append(String.join("\nAND", whereConditions));
    }

    // Get total count
    String countSql = "SELECT COUNT(*) " + extractFromAndJoins(sqlBuilder.toString());
    Integer totalElements = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

    // ORDER BY clause
    if (sort != null && sort.attribute() != null) {
      String orderByClause = buildOrderByClause(sort);
      if (orderByClause != null) {
        sqlBuilder.append(" ").append(orderByClause);
      }
    }

    // Pagination
    if (pagination != null) {
      int offset = pagination.page() != null ? pagination.page() * pagination.size() : 0;
      int limit = pagination.size() != null ? pagination.size() : 10;
      sqlBuilder.append(" LIMIT ? OFFSET ?");
      params.add(limit);
      params.add(offset);
    }

    // Execute query
    List<E> content = jdbcTemplate.query(sqlBuilder.toString(), rowMapper, params.toArray());

    // Calculate pagination info
    int totalPages = totalElements != null && pagination != null && pagination.size() != null
        ? (int) Math.ceil((double) totalElements / pagination.size())
        : 0;
    Boolean moreRows =
        pagination != null && pagination.page() != null && (pagination.page() + 1) < totalPages;

    return new SearchResponseDto<>(totalElements, totalPages, moreRows, content);
  }

  private Set<JoinInfo> collectUniqueJoins(List<SearchDataDto> searchData, SortDto sort) {
    var joins = new LinkedHashSet<JoinInfo>();

    // Collect joins from search filters
    if (searchData != null) {
      for (SearchDataDto data : searchData) {
        String attribute = data.attribute();
        searchInfoInterface.getMultipleAttributeByJsonField(attribute)
            .ifPresent(attr -> joins.addAll(attr.joinInfo()));
      }
    }

    // Collect joins from sort
    if (sort != null && sort.attribute() != null) {
      searchInfoInterface.getMultipleAttributeByJsonField(sort.attribute())
          .ifPresent(attr -> joins.addAll(attr.joinInfo()));
    }

    return joins;
  }

  private String buildCondition(String attribute, Operator operator, List<String> values,
      List<Object> params) {

    // Check simple attributes
    Optional<SimpleSearchAttribute> simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(attribute);
    if (simpleAttr.isPresent()) {
      return buildSimpleCondition(simpleAttr.get().dbField(), operator, values, params);
    }

    // Check multiple attributes
    Optional<MultipleSearchAttribute> multipleAttr = searchInfoInterface.getMultipleAttributeByJsonField(attribute);
    return multipleAttr.map(multipleSearchAttribute -> buildSimpleCondition(
        multipleSearchAttribute.getFullDbFieldName(), operator, values, params)).orElse(null);

  }

  private String buildSimpleCondition(String dbField, Operator operator, List<String> values,
      List<Object> params) {

    String sqlOperator = operator.getSqlOperator();
    String placeholderType = operator.getJpaValuePlaceholderType();

    // Handle operators that don't need values
    if (sqlOperator.equals("IS NULL")) {
      return dbField + " IS NULL";
    }
    if (sqlOperator.equals("IS NOT NULL")) {
      return dbField + " IS NOT NULL";
    }

    // Handle operators that need values
    if (values == null || values.isEmpty()) {
      return null;
    }

    String value = values.get(0);

    // Handle IN operator
    if (sqlOperator.equals("IN")) {
      Object transformedValue = operator.getJpaValuePalceholderFunction().apply(value);
      if (transformedValue instanceof List<?> list) {
        String placeholders = list.stream()
            .map(v -> "?")
            .collect(joining(", "));
        params.addAll(list);
        return dbField + " IN (" + placeholders + ")";
      }
    }

    // Handle BETWEEN operator
    if (sqlOperator.equals("BETWEEN") && values.size() >= 2) {
      Object value1 = operator.getJpaValuePalceholderFunction().apply(values.get(0));
      Object value2 = operator.getJpaValuePalceholderFunction().apply(values.get(1));
      params.add(value1);
      params.add(value2);
      return dbField + " BETWEEN ? AND ?";
    }

    // Handle standard operators
    Object transformedValue = operator.getJpaValuePalceholderFunction().apply(value);
    params.add(transformedValue);

    return String.format("%s %s %s", dbField, sqlOperator, "?");
  }

  private String buildOrderByClause(SortDto sort) {
    String attribute = sort.attribute();

    // Check simple attributes
    Optional<SimpleSearchAttribute> simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(attribute);
    if (simpleAttr.isPresent()) {
      return "ORDER BY " + searchInfoInterface.getTableAlias() + "." + simpleAttr.get().dbField() + " " + sort.direction();
    }

    // Check multiple attributes
    Optional<MultipleSearchAttribute> multipleAttr = searchInfoInterface.getMultipleAttributeByJsonField(attribute);
    return multipleAttr.map(
        multipleSearchAttribute -> "ORDER BY " + multipleSearchAttribute.getFullDbFieldName() + " "
            + sort.direction()).orElse(null);

  }

  private String extractFromAndJoins(String sql) {
    int fromIndex = sql.indexOf(" FROM ");
    if (fromIndex == -1) {
      return "";
    }

    int whereIndex = sql.indexOf(" WHERE ", fromIndex);
    int orderByIndex = sql.indexOf(" ORDER BY ", fromIndex);
    int limitIndex = sql.indexOf(" LIMIT ", fromIndex);

    int endIndex = sql.length();
    if (whereIndex != -1) {
      endIndex = Math.min(endIndex, whereIndex);
    }
    if (orderByIndex != -1) {
      endIndex = Math.min(endIndex, orderByIndex);
    }
    if (limitIndex != -1) {
      endIndex = Math.min(endIndex, limitIndex);
    }

    return sql.substring(fromIndex, endIndex);
  }

}
