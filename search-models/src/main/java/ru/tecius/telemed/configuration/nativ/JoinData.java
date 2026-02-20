package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.constraints.NotBlank;

public record JoinData(
    @NotBlank(message = "Поле attributes.db.joinInfo.join.table не может быть пустым")
    String table,

    String alias,

    @NotBlank(message = "Поле attributes.db.joinInfo.join.column не может быть пустым")
    String column
) {

}
