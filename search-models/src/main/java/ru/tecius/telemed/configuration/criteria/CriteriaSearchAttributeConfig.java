package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CriteriaSearchAttributeConfig(
    @NotNull(message = "Поле attributes не может быть null")
    @NotEmpty(message = "Список attributes не может быть пустым")
    @Valid
    List<CriteriaSearchAttribute> attributes
) {
}
