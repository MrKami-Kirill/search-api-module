package ru.tecius.telemed.criteria.service;

import jakarta.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;
import ru.tecius.telemed.common.criteria.CriteriaInfoInterface;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class CriteriaEntityService<E> extends AbstractCriteriaSqlService<E> {

  public CriteriaEntityService(
      EntityManager entityManager,
      CriteriaInfoInterface<E> criteriaInfo
  ) {
    super(entityManager, criteriaInfo);
  }

  public SearchResponseDto<E> search(
      List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination
  ) {
    var cb = entityManager.getCriteriaBuilder();

    // Сначала считаем общее количество
    var count = executeCountQuery(cb, searchData);

    // Затем выполняем основной запрос
    var content = executeSearchQuery(cb, searchData, sort, pagination);

    var pageSize = getPageSize(pagination, 10);
    var totalPages = calculateTotalPages(count, pageSize);
    Boolean moreRows = calculateMoreRows(pagination, totalPages);

    return new SearchResponseDto<>(count.intValue(), totalPages, moreRows, content);
  }

}
