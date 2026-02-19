package ru.tecius.telemed.configuration.nativ;

import static java.util.stream.Stream.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static ru.tecius.telemed.configuration.nativ.AttributeType.MULTIPLE;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;

public record NativeSearchAttribute(
    @NotNull(message = "Поле attributes.attributeType не может быть null")
    AttributeType attributeType,
    @NotBlank(message = "Поле attributes.jsonField не может быть пустым")
    String jsonField,
    @NotBlank(message = "Поле attributes.dbField не может быть пустым")
    String dbField,
    String dbTableAlias,
    @NotNull(message = "Поле attributes.fieldType не может быть null")
    Class<?> fieldType,
    @Valid
    LinkedHashSet<JoinInfo> joinInfo) {

  @AssertTrue(message = "Поле attributes.joinInfo не может быть null или пустым, "
      + "если attributeType = 'MULTIPLE'")
  public boolean isValidJoinInfo() {
    if (!Objects.equals(attributeType(), MULTIPLE)) {
      return true;
    }

    return isNotEmpty(joinInfo);
  }


  @AssertTrue(message = "Поле attributes.dbTableAlias должно совпадать с последним "
      + "attributes.joinInfo.join.alias")
  public boolean isDbTableAliasValid() {
    if (!Objects.equals(attributeType(), MULTIPLE)) {
      return true;
    }

    return ofNullable(joinInfo())
        .filter(Objects::nonNull)
        .map(LinkedHashSet::getLast)
        .filter(Objects::nonNull)
        .map(JoinInfo::join)
        .anyMatch(join -> Objects.equals(join.alias(), dbTableAlias()));
  }

  public String getFullDbFieldName() {
    return "%s.%s".formatted(dbTableAlias(), dbField());
  }

}
