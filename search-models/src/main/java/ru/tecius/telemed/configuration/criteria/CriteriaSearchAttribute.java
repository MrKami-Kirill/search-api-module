package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Атрибут поиска для Criteria API.
 * Описывает поле сущности для построения динамических запросов через JPA Criteria API.
 */
public record CriteriaSearchAttribute(
    @NotBlank(message = "Поле criteriaAttribute.jsonField не может быть пустым")
    String jsonField,

    @NotBlank(message = "Поле criteriaAttribute.entityPath не может быть пустым")
    String entityPath,

    @NotNull(message = "Поле criteriaAttribute.fieldType не может быть null")
    Class<?> fieldType,

    @Valid
    LinkedHashSet<CriteriaJoinInfo> joinInfo
) {

  /**
   * Проверяет валидность конфигурации join.
   * Для коллекций (@OneToMany) entityPath начинается с имени поля-коллекции.
   */
  @AssertTrue(message = "entityPath должен начинаться с поля связи из joinInfo")
  public boolean isJoinInfoValid() {
    if (joinInfo == null || joinInfo.isEmpty()) {
      return true;
    }

    // entityPath должен начинаться с первого поля в joinInfo
    // Например: joinInfo=[document, attachments], entityPath="attachments.fileName"
    // Это валидно, так как attachments - это последнее поле в цепочке join
    var segments = entityPath.split("\\.");
    if (segments.length == 1) {
      // Простой путь без точек - joinInfo не нужен
      return true;
    }

    // Проверяем, что путь начинается с одного из полей в joinInfo
    var firstSegment = segments[0];
    return joinInfo.stream()
        .anyMatch(join -> Objects.equals(join.path(), firstSegment));
  }

  /**
   * Проверяет, требуется ли join для этого атрибута.
   */
  public boolean requiresJoin() {
    return joinInfo != null && !joinInfo.isEmpty();
  }
}
