package ru.tecius.telemed.configuration.criteria;

import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Информация о join для Criteria API.
 * Описывает путь к связанной сущности в JPA модели.
 */
public record CriteriaJoinInfo(
    @NotBlank(message = "Поле attribute.joinInfo.path не может быть пустым")
    String path,

    @NotBlank(message = "Поле attribute.joinInfo.alias не может быть пустым")
    String alias,

    @NotNull(message = "Поле attribute.joinInfo.type не может быть null")
    JoinType type
) {
}
