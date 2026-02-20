package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CriteriaSearchAttributeConfig(
    @NotEmpty(message = "Список attributes не может быть пустым")
    @Valid
    List<CriteriaSearchAttribute> attributes
) {
}
