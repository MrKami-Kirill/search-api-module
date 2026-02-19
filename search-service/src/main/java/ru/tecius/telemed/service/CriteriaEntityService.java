package ru.tecius.telemed.service;

import jakarta.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;
import ru.tecius.telemed.common.CriteriaInfoInterface;
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
    return search(searchData, sort, pagination, null);
  }

}
