package ru.tecius.telemed.configuration.nativ;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static ru.tecius.telemed.configuration.nativ.AttributeType.MULTIPLE;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public record NativeSearchAttribute(
    @NotNull(message = "Поле attributes.type не может быть null")
    AttributeType type,
    @NotNull(message = "Поле attributes.json не может быть null")
    @Valid
    JsonData json,
    @NotNull(message = "Поле attributes.db не может быть null")
    @Valid
    DbData db) {

  @AssertTrue(message =
      "Поле attributes.db.joinInfo не может быть null или пустым, если attributeType = 'MULTIPLE'")
  public boolean isValidJoinInfo() {
    if (!Objects.equals(type(), MULTIPLE)) {
      return true;
    }

    return nonNull(db()) && isNotEmpty(db().joinInfo());
  }

}
