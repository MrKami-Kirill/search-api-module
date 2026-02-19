package ru.tecius.telemed.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import ru.tecius.telemed.common.SearchInfoInterface;
import ru.tecius.telemed.dto.request.PaginationDto;
import ru.tecius.telemed.dto.request.SearchDataDto;
import ru.tecius.telemed.dto.request.SortDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;

@Service
@RequiredArgsConstructor
public class NativeSqlService<E> {

  private final Class<E> cls;
  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<E> rowMapper;
  private final SearchInfoInterface<E> searchInfoInterface;


  public SearchResponseDto<E> search(List<SearchDataDto> searchData, SortDto sort,
      PaginationDto pagination) {
    return null;
  }

}
