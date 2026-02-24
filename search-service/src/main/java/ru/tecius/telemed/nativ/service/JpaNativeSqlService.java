package ru.tecius.telemed.nativ.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.LinkedList;
import java.util.List;
import ru.tecius.telemed.common.nativ.SearchInfoInterface;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class JpaNativeSqlService<E> extends AbstractNativeSqlService<E> {

  private final Class<E> cls;
  private final EntityManager entityManager;

  public JpaNativeSqlService(
      Class<E> cls,
      EntityManager entityManager,
      SearchInfoInterface<E> searchInfoInterface,
      Long defaultPageSize
  ) {
    super(searchInfoInterface, defaultPageSize);
    this.cls = cls;
    this.entityManager = entityManager;
  }

  @SuppressWarnings("unchecked")
  public SearchResponseDto<E> search(List<SearchDataDto> searchData, LinkedList<SortDto> sort,
      PaginationDto pagination, boolean needCalculateCount) {
    return search(searchData, sort, pagination,
        (countSql, params) -> {
          var countQuery = entityManager.createNativeQuery(countSql, Long.class);
          setQueryParameters(countQuery, params);
          return (Long) countQuery.getSingleResult();
        },
        (sql, params) -> {
          var query = entityManager.createNativeQuery(sql, cls);
          setQueryParameters(query, params);
          return query.getResultList();
        }, needCalculateCount);
  }

  private void setQueryParameters(Query query, List<Object> params) {
    for (int i = 0; i < params.size(); i++) {
      query.setParameter(i + 1, params.get(i));
    }
  }
}
