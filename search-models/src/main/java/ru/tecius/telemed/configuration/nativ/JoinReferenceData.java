package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.constraints.NotBlank;

public record JoinReferenceData(
    @NotBlank(message = "Поле attributes.joinInfo.reference.table не может быть пустым")
    String table,
    @NotBlank(message = "Поле attributes.joinInfo.reference.alias не может быть пустым")
    String alias,
    @NotBlank(message = "Поле attributes.joinInfo.reference.column не может быть пустым")
    String column
) {

}
