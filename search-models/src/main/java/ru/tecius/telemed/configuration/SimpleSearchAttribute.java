package ru.tecius.telemed.configuration;

import jakarta.validation.constraints.NotBlank;

public record SimpleSearchAttribute(
    @NotBlank(message = "Поле simpleAttributes.jsonField не может быть пустым")
    String jsonField,
    @NotBlank(message = "Поле simpleAttributes.dbFiled не может быть пустым")
    String dbField
) {

}
