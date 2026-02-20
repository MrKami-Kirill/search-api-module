package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;

public record DbData(
    @NotBlank(message = "Поле attributes.db.column не может быть пустым")
    String column,
    @NotNull(message = "Поле attributes.db.type не может быть null")
    Class<?> type,
    @Valid
    LinkedHashSet<JoinInfo> joinInfo
) {

}
