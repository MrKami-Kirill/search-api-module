package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ru.tecius.telemed.configuration.common.AttributeType;

public record CriteriaSearchAttribute(
    @NotNull(message = "Поле attributes.type не может быть null")
    AttributeType type,
    @NotNull(message = "Поле attributes.json не может быть null")
    @Valid
    JsonData json,
    @NotNull(message = "Поле attributes.db не может быть null")
    @Valid
    DbData db
) {

}
