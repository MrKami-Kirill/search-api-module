package ru.tecius.telemed.configuration;

import static java.util.stream.Stream.ofNullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;

public record MultipleSearchAttribute(
    @NotBlank(message = "Поле multipleAttributes.jsonField не может быть пустым")
    String jsonField,
    @NotBlank(message = "Поле multipleAttributes.dbField не может быть пустым")
    String dbField,
    @NotBlank(message = "Поле multipleAttributes.dbTableAlias не может быть пустым")
    String dbTableAlias,
    @NotNull(message = "Поле multipleAttributes.fieldType не может быть null")
    FieldType fieldType,
    @NotNull(message = "Поле multipleAttributes.joinInfo не может быть null")
    @NotEmpty(message = "Поле multipleAttributes.joinInfo не может быть пустым")
    @Valid
    LinkedHashSet<JoinInfo> joinInfo) {

  @AssertTrue(message = "Поле multipleAttributes.dbTableAlias должно совпадать с последним "
      + "multipleAttributes.joinInfo.join.alias")
  public boolean isDbTableAliasValid() {
    return ofNullable(joinInfo())
        .map(LinkedHashSet::getLast)
        .filter(Objects::nonNull)
        .map(JoinInfo::join)
        .anyMatch(join -> Objects.equals(join.alias(), dbTableAlias()));
  }

  public String getFullDbFieldName() {
    return "%s.%s".formatted(dbTableAlias(), dbField());
  }

}
