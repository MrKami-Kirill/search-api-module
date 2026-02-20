package ru.tecius.telemed.configuration.criteria;

import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinInfo(
    @NotBlank(message = "Поле attribute.joinInfo.path не может быть пустым")
    String path,

    @NotNull(message = "Поле attribute.joinInfo.type не может быть null")
    JoinType type
) {
}
