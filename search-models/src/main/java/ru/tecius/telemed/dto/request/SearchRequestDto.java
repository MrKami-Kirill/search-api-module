package ru.tecius.telemed.dto.request;

import java.util.List;

public record SearchRequestDto(PaginationDto pagination,
                               SortDto sort,
                               List<SearchDataDto> searchData) {

}
