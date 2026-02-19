package ru.tecius.telemed.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record MultipleSearchAttributeConfig(
    @NotNull(message = "Поле multipleAttributes не может быть null")
    @NotEmpty(message = "Поле multipleAttributes не может быть пустым")
    @Valid
    Set<MultipleSearchAttribute> multipleAttributes
) {

}
