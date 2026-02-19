package ru.tecius.telemed.dto.request;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.join;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@RequiredArgsConstructor
@Getter
public enum Operator {

  EQUAL(
      (field, values) -> "%s = ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  NOT_EQUAL(
      (field, values) -> "%s != ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  IN(
      (field, values) -> "%s IN (%s)".formatted(field,
          join(", ", nCopies(values.size(), "?"))),
      CollectionUtils::isNotEmpty),

  CONTAIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  EXCLUDE(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  BEGIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  NOT_BEGIN(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  END(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  NOT_END(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  IS_NULL(
      (field, values) -> "%s IS NULL".formatted(field),
      CollectionUtils::isEmpty),

  IS_NOT_NULL(
      (field, values) -> "%s IS NOT NULL".formatted(field),
      CollectionUtils::isEmpty),

  BETWEEN(
      (field, values) -> "%s BETWEEN ? AND ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(2, values.size())),

  MORE_OR_EQUAL(
      (field, values) -> "%s >= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size())),

  LESS_OR_EQUAL(
      (field, values) -> "%s <= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()));

  private final BiFunction<String, List<String>, String> sqlTemplateFunction;
  private final Predicate<List<String>> valuePredicate;

  public String buildCondition(String dbField, List<String> values) {
    valuePredicate.test(values);
    return sqlTemplateFunction.apply(dbField, values);
  }

  public List<String> transformValues(List<String> values) {
    return switch (this) {
      case IN, BETWEEN -> values;
      case CONTAIN, EXCLUDE -> List.of("%" + values.getFirst() + "%");
      case BEGIN, NOT_BEGIN -> List.of(values.getFirst() + "%");
      case END, NOT_END -> List.of("%" + values.getFirst());
      case IS_NULL, IS_NOT_NULL -> emptyList();
      default -> List.of(values.getFirst());
    };
  }

}
