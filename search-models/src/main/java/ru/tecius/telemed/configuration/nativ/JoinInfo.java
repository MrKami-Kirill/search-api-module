package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record JoinInfo(
    @NotNull(message = "Поле attributes.joinInfo.order не может быть null")
    Integer order,

    @NotNull(message = "Поле attributes.joinInfo.reference не может быть null")
    @Valid
    JoinReferenceData reference,

    @NotNull(message = "Поле attributes.joinInfo.join не может быть null")
    @Valid
    JoinData join,

    @NotNull(message = "Поле attributes.joinInfo.type не может быть null")
    JoinTypeEnum type
) {

}
