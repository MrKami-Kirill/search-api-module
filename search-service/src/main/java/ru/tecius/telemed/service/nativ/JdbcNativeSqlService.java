package ru.tecius.telemed.service.nativ;

import java.util.LinkedList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.tecius.telemed.common.nativ.SearchInfoInterface;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

public class JdbcNativeSqlService<E> extends AbstractNativeSqlSqlService<E> {

  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<E> rowMapper;

  public JdbcNativeSqlService(
      JdbcTemplate jdbcTemplate,
      RowMapper<E> rowMapper,
      SearchInfoInterface<E> searchInfoInterface
  ) {
    super(searchInfoInterface);
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = rowMapper;
  }

  public SearchResponseDto<E> search(List<SearchDataDto> searchData, LinkedList<SortDto> sort,
      PaginationDto pagination) {
    return search(searchData, sort, pagination,
        (countSql, params) -> jdbcTemplate.queryForObject(countSql,
            Integer.class, params.toArray()),
        (sql, params) -> jdbcTemplate.query(sql,
            rowMapper, params.toArray()));
  }

}
