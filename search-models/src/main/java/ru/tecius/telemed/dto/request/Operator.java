package ru.tecius.telemed.dto.request;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.join;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@RequiredArgsConstructor
@Getter
public enum Operator {

  EQUAL(
      (field, values) -> "%s = ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> values),

  NOT_EQUAL(
      (field, values) -> "%s != ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> values),

  IN(
      (field, values) -> "%s IN (%s)".formatted(field,
          join(", ", nCopies(values.size(), "?"))),
      CollectionUtils::isNotEmpty,
      values -> values),

  CONTAIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of("%" + values.getFirst() + "%")),

  EXCLUDE(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of("%" + values.getFirst() + "%")),

  BEGIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of(values.getFirst() + "%")),

  NOT_BEGIN(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of(values.getFirst() + "%")),

  END(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of("%" + values.getFirst())),

  NOT_END(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> List.of("%" + values.getFirst())),

  IS_NULL(
      (field, values) -> "%s IS NULL".formatted(field),
      CollectionUtils::isEmpty,
      values -> emptyList()),

  IS_NOT_NULL(
      (field, values) -> "%s IS NOT NULL".formatted(field),
      CollectionUtils::isEmpty,
      values -> emptyList()),

  BETWEEN(
      (field, values) -> "%s BETWEEN ? AND ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(2, values.size()),
      values -> values),

  MORE_OR_EQUAL(
      (field, values) -> "%s >= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> values),

  LESS_OR_EQUAL(
      (field, values) -> "%s <= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      values -> values);

  private final BiFunction<String, List<String>, String> sqlTemplateFunction;
  private final Predicate<List<String>> valuePredicate;
  private final Function<List<String>, List<String>> transformValueFunction;

  public String buildCondition(String dbField, List<String> values) {
    valuePredicate.test(values);
    return sqlTemplateFunction.apply(dbField, values);
  }

}
