package ru.tecius.telemed.configuration;

import jakarta.validation.constraints.NotBlank;

public record JoinReferenceData(
    @NotBlank(message = "Поле multipleAttributes.joinInfo.reference.table не может быть пустым")
    String table,
    @NotBlank(message = "Поле multipleAttributes.joinInfo.reference.alias не может быть пустым")
    String alias,
    @NotBlank(message = "Поле multipleAttributes.joinInfo.reference.column не может быть пустым")
    String column
) {

}
