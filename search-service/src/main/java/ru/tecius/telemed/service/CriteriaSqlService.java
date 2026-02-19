package ru.tecius.telemed.service;

import jakarta.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class CriteriaSqlService<E> {

  private final Class<E> cls;
  private final EntityManager entityManager;

  public CriteriaSqlService(
      Class<E> cls,
      EntityManager entityManager
  ) {
    this.cls = cls;
    this.entityManager = entityManager;
  }

  public SearchResponseDto<E> search(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination) {

  }
}
