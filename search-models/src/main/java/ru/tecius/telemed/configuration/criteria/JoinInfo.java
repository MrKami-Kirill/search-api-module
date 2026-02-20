package ru.tecius.telemed.configuration.criteria;

import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinInfo(
    @NotNull(message = "Поле attributes.db.joinInfo.order не может быть null")
    Integer order,

    @NotBlank(message = "Поле attributes.db.joinInfo.path не может быть пустым")
    String path,

    @NotNull(message = "Поле attributes.db.joinInfo.type не может быть null")
    JoinType type
) {
}
