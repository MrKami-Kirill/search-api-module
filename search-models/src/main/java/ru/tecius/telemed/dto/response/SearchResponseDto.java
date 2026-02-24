package ru.tecius.telemed.dto.response;

import java.util.List;

public record SearchResponseDto<E>(Long totalElements,
                                   Long totalPages,
                                Boolean moreRows,
                                List<E> content) {

}
