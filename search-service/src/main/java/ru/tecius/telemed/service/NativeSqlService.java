package ru.tecius.telemed.service;

import static java.lang.String.join;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.LF;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.tecius.telemed.common.SearchInfoInterface;
import ru.tecius.telemed.configuration.JoinInfo;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;
import ru.tecius.telemed.exception.ValidationException;

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

  protected SearchResponseDto<E> search(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination) {
    var sqlBuilder = new StringBuilder();
    var params = new LinkedList<>();

    sqlBuilder.append("SELECT ")
        .append(searchInfoInterface.getTableAlias())
        .append(".* FROM ")
        .append(searchInfoInterface.getFullTableName())
        .append(LF);
    var uniqueJoins = collectUniqueJoins(searchData, sort);

    if (isNotEmpty(uniqueJoins)) {
      var joinsSql = uniqueJoins.stream()
          .sorted(Comparator.comparingInt(JoinInfo::order))
          .map(searchInfoInterface::createJoinString)
          .collect(joining(LF));
      sqlBuilder.append(joinsSql)
          .append(LF);
    }

    // WHERE clause
    var whereConditions = new ArrayList<String>();
    if (isNotEmpty(searchData)) {
      searchData.forEach(data -> whereConditions.add(buildCondition(data, params)));
    }

    if (isNotEmpty(whereConditions)) {
      sqlBuilder.append("WHERE ")
          .append(join("\nAND ", whereConditions))
          .append(LF);
    }

    // Get total count
    var countSql = "SELECT COUNT(%s.*) %s".formatted(searchInfoInterface.getTableAlias(),
        extractFromWithJoinsAndWhere(sqlBuilder.toString()));
    var totalElements = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

    // ORDER BY clause
    if (isNotEmpty(sort)) {
      var orderByClause = buildOrderByClauses(sort);
      if (nonNull(orderByClause)) {
        sqlBuilder.append(orderByClause)
            .append(LF);
      }
    }

    // Pagination
    if (nonNull(pagination)) {
      var offset = nonNull(pagination.page()) ? pagination.page() * pagination.size() : 0;
      var limit = nonNull(pagination.page()) ? pagination.size() : 10;
      sqlBuilder.append("LIMIT ? OFFSET ?");
      params.add(String.valueOf(limit));
      params.add(offset);
    }

    // Execute query
    List<E> content = jdbcTemplate.query(sqlBuilder.toString(), rowMapper, params.toArray());

    var totalPages = totalElements != null && pagination != null && pagination.size() != null
        ? (int) Math.ceil((double) totalElements / pagination.size())
        : 0;
    Boolean moreRows =
        pagination != null && pagination.page() != null && (pagination.page() + 1) < totalPages;

    return new SearchResponseDto<>(totalElements, totalPages, moreRows, content);
  }

  private Set<JoinInfo> collectUniqueJoins(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort) {
    var joins = new LinkedHashSet<JoinInfo>();

    if (nonNull(searchData)) {
      searchData.forEach(dto -> searchInfoInterface.getMultipleAttributeByJsonField(
              dto.attribute())
          .ifPresent(attr -> joins.addAll(attr.joinInfo())));
    }

    if (isNotEmpty(sort)) {
      sort.forEach(dto -> searchInfoInterface.getMultipleAttributeByJsonField(
              dto.attribute())
          .ifPresent(attr -> joins.addAll(attr.joinInfo())));
    }

    return joins;
  }

  private String buildCondition(SearchDataDto searchData, List<Object> params) {
    var attribute = searchData.attribute();
    var operator = searchData.operator();
    var values = searchData.value();
    var simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(attribute);
    if (simpleAttr.isPresent()) {
      var fullDbFieldName = searchInfoInterface.getTableAlias() + "." + simpleAttr.get().dbField();
      return buildSimpleCondition(fullDbFieldName, operator, values, params);
    }

    return searchInfoInterface.getMultipleAttributeByJsonField(attribute)
        .map(multipleSearchAttribute -> buildSimpleCondition(
            multipleSearchAttribute.getFullDbFieldName(), operator, values, params))
        .orElseThrow(() -> new ValidationException(
            "Фильтрация по атрибуту %s запрещена".formatted(attribute)));

  }

  private String buildSimpleCondition(String dbField, Operator operator, List<String> values,
      List<Object> params) {
    var condition = operator.buildCondition(dbField, values);
    params.addAll(operator.getTransformValueFunction().apply(values));
    return condition;
  }

  private String buildOrderByClauses(LinkedList<SortDto> sort) {
    var orderByParts = sort.stream()
        .map(this::buildSingleOrderByClause)
        .filter(Objects::nonNull)
        .toList();

    if (orderByParts.isEmpty()) {
      return null;
    }

    return "ORDER BY " + join(", ", orderByParts);
  }

  private String buildSingleOrderByClause(SortDto sort) {
    var attribute = sort.attribute();
    var simpleAttr = searchInfoInterface.getSimpleAttributeByJsonField(attribute);
    if (simpleAttr.isPresent()) {
      return "%s %s".formatted(
          searchInfoInterface.getTableAlias() + "." + simpleAttr.get().dbField(),
          sort.direction());
    }

    var multipleAttr = searchInfoInterface.getMultipleAttributeByJsonField(attribute);
    return multipleAttr.map(attr -> "%s %s".formatted(attr.getFullDbFieldName(), sort.direction()))
        .orElseThrow(() -> new ValidationException(
            "Сортировка по атрибуту %s запрещена".formatted(attribute)));
  }

  private String extractFromWithJoinsAndWhere(String sql) {
    int fromIndex = sql.indexOf(" FROM ");
    if (fromIndex == -1) {
      throw new RuntimeException("Ошибка составления sql: %s".formatted(sql));
    }

    var orderByIndex = sql.indexOf(" ORDER BY ", fromIndex);
    var limitIndex = sql.indexOf(" LIMIT ", fromIndex);

    int endIndex = sql.length();
    if (orderByIndex != -1) {
      endIndex = Math.min(endIndex, orderByIndex);
    }

    if (limitIndex != -1) {
      endIndex = Math.min(endIndex, limitIndex);
    }

    return sql.substring(fromIndex, endIndex);
  }

}
