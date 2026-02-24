package ru.tecius.telemed.criteria.service;

import static java.util.Collections.emptySet;

import jakarta.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.common.criteria.CriteriaInfoInterface;
import ru.tecius.telemed.common.criteria.HintName;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class CriteriaEntityService<E> extends AbstractCriteriaSqlService<E> {

  public CriteriaEntityService(
      EntityManager entityManager,
      CriteriaInfoInterface<E> criteriaInfo,
      Long defaultPageSize
  ) {
    super(entityManager, criteriaInfo, defaultPageSize);
  }

  public SearchResponseDto<E> search(
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination,
      boolean needCalculateCount
  ) {
    return search(searchData, sort, pagination, null, emptySet(), needCalculateCount);
  }

  public SearchResponseDto<E> search(
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination,
      HintName hintName,
      Set<String> entityGraphs,
      boolean needCalculateCount
  ) {
    var cb = entityManager.getCriteriaBuilder();


    // Сначала считаем общее количество
    var totalElements = 0L;
    if (needCalculateCount) {
      totalElements = executeCountQuery(cb, searchData);
    }

    // Затем выполняем основной запрос с entity graph
    var content = executeSearchQuery(cb, searchData, sort, pagination, hintName, entityGraphs);

    var pageSize = getPageSize(pagination);
    var totalPages = calculateTotalPages(totalElements, pageSize);
    Boolean moreRows = calculateMoreRows(pagination, totalPages);

    return new SearchResponseDto<>(totalElements, totalPages, moreRows, content);
  }

}
