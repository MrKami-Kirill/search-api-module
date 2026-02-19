package ru.tecius.telemed.service;

import jakarta.persistence.Query;
import java.util.LinkedList;
import java.util.List;
import ru.tecius.telemed.common.SearchInfoInterface;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class NativeEntitySqlService<E> extends AbstractNativeSqlSqlService<E> {

  private final Class<E> cls;
  private final jakarta.persistence.EntityManager entityManager;

  public NativeEntitySqlService(
      Class<E> cls,
      jakarta.persistence.EntityManager entityManager,
      SearchInfoInterface<E> searchInfoInterface
  ) {
    super(searchInfoInterface);
    this.cls = cls;
    this.entityManager = entityManager;
  }

  @SuppressWarnings("unchecked")
  public SearchResponseDto<E> search(List<SearchDataDto> searchData,
      LinkedList<SortDto> sort,
      PaginationDto pagination) {
    return search(searchData, sort, pagination,
        (countSql, params) -> {
          var countQuery = entityManager.createNativeQuery(countSql, Integer.class);
          setQueryParameters(countQuery, params);
          return (Integer) countQuery.getSingleResult();
        },
        (sql, params) -> {
          var query = entityManager.createNativeQuery(sql, cls);
          setQueryParameters(query, params);
          return query.getResultList();
        });
  }

  private void setQueryParameters(Query query, List<Object> params) {
    for (int i = 0; i < params.size(); i++) {
      query.setParameter(i + 1, params.get(i));
    }
  }
}
