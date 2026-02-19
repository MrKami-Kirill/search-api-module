package ru.tecius.telemed.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimpleSearchAttribute(
    @NotBlank(message = "Поле simpleAttributes.jsonField не может быть пустым")
    String jsonField,
    @NotBlank(message = "Поле simpleAttributes.dbFiled не может быть пустым")
    String dbField,
    @NotNull(message = "Поле simpleAttributes.fieldType не может быть null")
    FieldType fieldType
) {


}
