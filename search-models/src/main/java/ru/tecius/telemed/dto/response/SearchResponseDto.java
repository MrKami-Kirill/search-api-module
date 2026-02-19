package ru.tecius.telemed.dto.response;

import java.util.List;

public record SearchResponseDto<E>(Integer totalElements,
                                Integer totalPages,
                                Boolean moreRows,
                                List<E> content) {

}
