package ru.tecius.telemed.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Конфигурация для загрузки Criteria атрибутов из YML.
 */
public record CriteriaSearchAttributeConfig(
    @NotEmpty(message = "Список attributes не может быть пустым")
    @Valid
    List<CriteriaSearchAttribute> attributes
) {
}
