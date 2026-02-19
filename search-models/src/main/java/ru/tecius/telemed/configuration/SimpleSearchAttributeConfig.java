package ru.tecius.telemed.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record SimpleSearchAttributeConfig(
    @NotNull(message = "Поле simpleAttributes не может быть null")
    @NotEmpty(message = "Поле simpleAttributes не может быть пустым")
    @Valid
    Set<SimpleSearchAttribute> simpleAttributes) {

}
