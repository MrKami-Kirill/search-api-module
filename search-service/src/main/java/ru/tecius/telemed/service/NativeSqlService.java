package ru.tecius.telemed.service;

import static java.lang.String.join;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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

public class NativeSqlService<E> {

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
    var params = new LinkedList<>();

    sqlBuilder.append("SELECT ")
        .append(searchInfoInterface.getTableAlias())
        .append(".* FROM ")
        .append(searchInfoInterface.getFullTableName());
    var uniqueJoins = collectUniqueJoins(searchData, sort);

    if (!uniqueJoins.isEmpty()) {
      var joinsSql = uniqueJoins.stream()
          .sorted(Comparator.comparingInt(JoinInfo::order))
          .map(JoinInfo::createJoinString)
          .collect(joining("\n"));
      sqlBuilder.append(" ").append(joinsSql);
    }

    // WHERE clause
    var whereConditions = new ArrayList<String>();
    if (isNotEmpty(searchData)) {
      searchData.forEach(data -> {
        var attribute = data.attribute();
        var operator = data.operator();
        var values = data.value();

        var condition = buildCondition(attribute, operator, values, params);
        if (nonNull(condition)) {
          whereConditions.add(condition);
        }
      });
    }

    if (isNotEmpty(whereConditions)) {
      sqlBuilder.append(" WHERE ")
          .append(join("\n  AND ", whereConditions));
    }

    // Get total count
    var countSql = "SELECT COUNT(mi.*) " + extractFromAndJoins(sqlBuilder.toString());
    var totalElements = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

    // ORDER BY clause
    if (nonNull(sort) && nonNull(sort.attribute())) {
      var orderByClause = buildOrderByClause(sort);
      if (nonNull(orderByClause)) {
        sqlBuilder.append(" ").append(orderByClause);
      }
    }

    // Pagination
    if (nonNull(pagination)) {
      int offset = nonNull(pagination.page()) ? pagination.page() * pagination.size() : 0;
      int limit = nonNull(pagination.page()) ? pagination.size() : 10;
      sqlBuilder.append(" LIMIT ? OFFSET ?");
      params.add(String.valueOf(limit));
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
    Optional<SimpleSearchAttribute> simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(
        attribute);
    if (simpleAttr.isPresent()) {
      var fullDbField = searchInfoInterface.getTableAlias() + "." + simpleAttr.get().dbField();
      return buildSimpleCondition(fullDbField, operator, values, params);
    }

    // Check multiple attributes
    Optional<MultipleSearchAttribute> multipleAttr = searchInfoInterface.getMultipleAttributeByJsonField(
        attribute);
    return multipleAttr.map(multipleSearchAttribute -> buildSimpleCondition(
        multipleSearchAttribute.getFullDbFieldName(), operator, values, params)).orElse(null);

  }

  private String buildSimpleCondition(String dbField, Operator operator, List<String> values,
      List<Object> params) {

    // Build SQL condition template
    var condition = operator.buildCondition(dbField, values);

    // Transform values and add to params
    var transformedValues = operator.transformValues(values);
    params.addAll(transformedValues);

    return condition;
  }

  private String buildOrderByClause(SortDto sort) {
    String attribute = sort.attribute();

    // Check simple attributes
    Optional<SimpleSearchAttribute> simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(
        attribute);
    if (simpleAttr.isPresent()) {
      return "ORDER BY " + searchInfoInterface.getTableAlias() + "." + simpleAttr.get().dbField()
          + " " + sort.direction();
    }

    // Check multiple attributes
    Optional<MultipleSearchAttribute> multipleAttr = searchInfoInterface.getMultipleAttributeByJsonField(
        attribute);
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
