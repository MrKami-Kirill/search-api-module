package ru.tecius.telemed.dto.request;

import java.util.LinkedList;
import java.util.List;

public record SearchRequestDto(PaginationDto pagination,
                               LinkedList<SortDto> sort,
                               List<SearchDataDto> searchData) {

}
