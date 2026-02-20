package ru.tecius.telemed.service.nativ;

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
import java.util.function.BiFunction;
import ru.tecius.telemed.common.nativ.SearchInfoInterface;
import ru.tecius.telemed.configuration.nativ.JoinInfo;
import ru.tecius.telemed.dto.request.Operator;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public abstract class AbstractNativeSqlService<E> {

  private final SearchInfoInterface<E> searchInfoInterface;

  protected AbstractNativeSqlService(SearchInfoInterface<E> searchInfoInterface) {
    this.searchInfoInterface = searchInfoInterface;
  }

  protected SearchResponseDto<E> search(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination,
      BiFunction<String, LinkedList<Object>, Integer> totalElementsFunction,
      BiFunction<String, LinkedList<Object>, List<E>> contentFunction) {
    var params = new LinkedList<>();
    var sqlBuilder = buildBaseQuery(searchData, sort, params);

    var countSql = buildCountQuery(sqlBuilder.toString());
    var totalElements = totalElementsFunction.apply(countSql, params);

    addOrderBy(sqlBuilder, sort);

    var pageSize = getPageSize(pagination, 10);
    addPagination(sqlBuilder, params, pagination, pageSize);

    var content = contentFunction.apply(sqlBuilder.toString(), params);

    var totalPages = calculateTotalPages(totalElements, content.size());
    Boolean moreRows = calculateMoreRows(pagination, totalPages);

    return new SearchResponseDto<>(totalElements, totalPages, moreRows, content);
  }

  private StringBuilder buildBaseQuery(List<SearchDataDto> searchData, LinkedList<SortDto> sort,
      LinkedList<Object> params) {
    var sqlBuilder = new StringBuilder();

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

    var whereConditions = new ArrayList<String>();
    if (isNotEmpty(searchData)) {
      searchData.forEach(data -> whereConditions.add(buildCondition(data, params)));
    }

    if (isNotEmpty(whereConditions)) {
      sqlBuilder.append("WHERE ")
          .append(LF)
          .append(join("\nAND ", whereConditions))
          .append(LF);
    }

    return sqlBuilder;
  }

  private String buildCountQuery(String sql) {
    return "SELECT COUNT(%s.*) %s".formatted(searchInfoInterface.getTableAlias(),
        extractFromWithJoinsAndWhere(sql));
  }

  private Integer getPageSize(PaginationDto pagination, Integer defaultPageSize) {
    return nonNull(pagination) && nonNull(pagination.page()) ? pagination.size() : defaultPageSize;
  }

  private void addPagination(StringBuilder sqlBuilder, LinkedList<Object> params,
      PaginationDto pagination, Integer limit) {
    if (nonNull(pagination)) {
      var offset = nonNull(pagination.page()) ? pagination.page() * limit : 0;
      sqlBuilder.append("LIMIT ? OFFSET ?");
      params.add(limit);
      params.add(offset);
    }
  }

  private void addOrderBy(StringBuilder sqlBuilder, LinkedList<SortDto> sort) {
    if (isNotEmpty(sort)) {
      var order = buildOrder(sort);
      if (nonNull(order)) {
        sqlBuilder.append(order)
            .append(LF);
      }
    }
  }

  private int calculateTotalPages(Integer totalElements, int pageSize) {
    return totalElements != null
        ? (int) Math.ceil((double) totalElements / pageSize)
        : 0;
  }

  private boolean calculateMoreRows(PaginationDto pagination, int totalPages) {
    return pagination != null && pagination.page() != null
        && (pagination.page() + 1) < totalPages;
  }

  private Set<JoinInfo> collectUniqueJoins(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort) {
    var joins = new LinkedHashSet<JoinInfo>();
    if (isNotEmpty(searchData)) {
      searchData.forEach(dto -> searchInfoInterface.getMultipleAttributeByJsonKey(
              dto.attribute())
          .ifPresent(attr -> joins.addAll(attr.db().joinInfo())));
    }

    if (isNotEmpty(sort)) {
      sort.forEach(dto -> searchInfoInterface.getMultipleAttributeByJsonKey(
              dto.attribute())
          .ifPresent(attr -> joins.addAll(attr.db().joinInfo())));
    }

    return joins;
  }

  private String buildCondition(SearchDataDto searchData, List<Object> params) {
    var attribute = searchData.attribute();
    var attr = searchInfoInterface.getAttributeByJsonKey(attribute,
        "Фильтрация по атрибуту %s запрещена".formatted(attribute));
    return buildCondition(
        searchInfoInterface.getFullColumnNameByAttribute(attr),
        searchData.operator(), searchData.value(), params, attr.db().type());
  }

  private String buildCondition(String dbField, Operator operator, List<String> values,
      List<Object> params, Class<?> fieldType) {
    operator.checkValue(values);

    var condition = operator.buildNativeCondition(dbField, values);
    params.addAll(operator.getNativeTransformValueFunction().apply(values, fieldType));
    return condition;
  }

  private String buildOrder(LinkedList<SortDto> sort) {
    var orderByParts = sort.stream()
        .map(this::buildOrder)
        .filter(Objects::nonNull)
        .toList();

    if (orderByParts.isEmpty()) {
      return null;
    }

    return "ORDER BY " + join(", ", orderByParts);
  }

  private String buildOrder(SortDto sort) {
    var attribute = sort.attribute();
    var attr = searchInfoInterface.getAttributeByJsonKey(attribute,
        "Сортировка по атрибуту %s запрещена".formatted(attribute));
    return "%s %s".formatted(
        searchInfoInterface.getFullColumnNameByAttribute(attr),
        sort.direction());
  }

  private String extractFromWithJoinsAndWhere(String sql) {
    int fromIndex = sql.indexOf(" FROM ");
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
