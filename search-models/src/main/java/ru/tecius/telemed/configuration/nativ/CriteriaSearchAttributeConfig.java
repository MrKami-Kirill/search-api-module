package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;

/**
 * Конфигурация для загрузки Criteria атрибутов из YML.
 */
public record CriteriaSearchAttributeConfig(
    @NotEmpty(message = "Список attributes не может быть пустым")
    @Valid
    List<CriteriaSearchAttribute> attributes
) {
}
