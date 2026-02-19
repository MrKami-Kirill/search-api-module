package ru.tecius.telemed.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ru.tecius.telemed.enumeration.JoinTypeEnum;

public record JoinInfo(
    @NotNull(message = "Поле multipleAttributes.joinInfo.order не может быть null")
    Integer order,
    @NotNull(message = "Поле multipleAttributes.joinInfo.reference не может быть null")
    @Valid
    JoinReferenceData reference,
    @NotNull(message = "Поле multipleAttributes.joinInfo.join не может быть null")
    @Valid
    JoinData join,
    @NotNull(message = "Поле multipleAttributes.joinInfo.type не может быть null")
    JoinTypeEnum type
) {

  public String createJoinString() {
    return "%s %s AS %s ON %s.%s = %s.%s".formatted(type().getValue(),
        join().table(), join().alias(),
        reference().alias(), reference().column(),
        join().alias(), join().column());
  }

}
