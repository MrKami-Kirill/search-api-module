package ru.tecius.telemed.configuration.criteria;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static ru.tecius.telemed.configuration.nativ.AttributeType.MULTIPLE;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;
import ru.tecius.telemed.configuration.nativ.AttributeType;

public record CriteriaSearchAttribute(
    @NotNull(message = "Поле attributes.attributeType не может быть null")
    AttributeType attributeType,

    @NotBlank(message = "Поле attribute.jsonField не может быть пустым")
    String jsonField,

    @NotBlank(message = "Поле attribute.entityPath не может быть пустым")
    String entityPath,

    @NotNull(message = "Поле attribute.fieldType не может быть null")
    Class<?> fieldType,
    @Valid
    LinkedHashSet<CriteriaJoinInfo> joinInfo
) {

  @AssertTrue(message = "Поле attributes.joinInfo не может быть null или пустым, "
      + "если attributeType = 'MULTIPLE'")
  public boolean isValidJoinInfo() {
    if (!Objects.equals(attributeType(), MULTIPLE)) {
      return true;
    }

    return isNotEmpty(joinInfo());
  }

  @AssertTrue(message = "entityPath должен начинаться с поля связи из joinInfo")
  public boolean isJoinInfoValid() {
    if (isEmpty(joinInfo())) {
      return true;
    }

    var segments = entityPath().split("\\.");
    if (segments.length == 1) {
      return true;
    }

    var firstSegment = segments[0];
    return joinInfo().stream()
        .anyMatch(join -> Objects.equals(join.path(), firstSegment));
  }

  public boolean requiresJoin() {
    return isNotEmpty(joinInfo());
  }
}
