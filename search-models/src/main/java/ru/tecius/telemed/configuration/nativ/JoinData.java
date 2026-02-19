package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.constraints.NotBlank;

public record JoinData(
    @NotBlank(message = "Поле attributes.joinInfo.join.table не может быть пустым")
    String table,
    @NotBlank(message = "Поле attributes.joinInfo.join.alias не может быть пустым")
    String alias,
    @NotBlank(message = "Поле attributes.joinInfo.join.column не может быть пустым")
    String column
) {

}
