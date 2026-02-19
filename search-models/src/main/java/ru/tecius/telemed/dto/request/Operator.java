package ru.tecius.telemed.dto.request;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.join;
import static util.Constants.BIRTHDAY_DATE_FORMAT;
import static util.Constants.ISO_DATE_FORMAT;
import static util.Constants.LOCAL_DATE_TIME_FORMAT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import ru.tecius.telemed.configuration.FieldType;

@RequiredArgsConstructor
@Getter
public enum Operator {

  EQUAL(
      (field, values) -> "%s = ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      Operator::transformValues),

  NOT_EQUAL(
      (field, values) -> "%s != ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      Operator::transformValues),

  IN(
      (field, values) -> "%s IN (%s)".formatted(field,
          join(", ", nCopies(values.size(), "?"))),
      CollectionUtils::isNotEmpty,
      Operator::transformValues),

  CONTAIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of("%" + values.getFirst() + "%")),

  EXCLUDE(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of("%" + values.getFirst() + "%")),

  BEGIN(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of(values.getFirst() + "%")),

  NOT_BEGIN(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of(values.getFirst() + "%")),

  END(
      (field, values) -> "%s LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of("%" + values.getFirst())),

  NOT_END(
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (values, fieldType) -> List.of("%" + values.getFirst())),

  IS_NULL(
      (field, values) -> "%s IS NULL".formatted(field),
      CollectionUtils::isEmpty,
      (field, values) -> emptyList()),

  IS_NOT_NULL(
      (field, values) -> "%s IS NOT NULL".formatted(field),
      CollectionUtils::isEmpty,
      (values, fieldType) -> emptyList()),

  BETWEEN(
      (field, values) -> "%s BETWEEN ? AND ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(2, values.size()),
      Operator::transformValues),

  MORE_OR_EQUAL(
      (field, values) -> "%s >= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      Operator::transformValues),

  LESS_OR_EQUAL(
      (field, values) -> "%s <= ?".formatted(field),
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      Operator::transformValues);

  private final BiFunction<String, List<String>, String> sqlTemplateFunction;
  private final Predicate<List<String>> valuePredicate;
  private final BiFunction<List<String>, FieldType, List<String>> transformValueFunction;

  public String buildCondition(String dbField, List<String> values) {
    valuePredicate.test(values);
    return sqlTemplateFunction.apply(dbField, values);
  }

  public static List<String> transformValues(List<String> values, FieldType fieldType
      ) {
    return switch (fieldType) {
      case LOCAL_DATE, LOCAL_DATE_TIME, OFFSET_DATE_TIME -> parseDateValues(values);
      default -> values;
    };
  }

  private static List<String> parseDateValues(List<String> values) {
    return values.stream()
        .map(Operator::parseSingleDateValue)
        .toList();
  }

  private static String parseSingleDateValue(String value) {
    try {
      return OffsetDateTime.parse(value, ofPattern(ISO_DATE_FORMAT)).toString();
    } catch (Exception ignored) {

    }

    try {
      return LocalDateTime.parse(value, ofPattern(LOCAL_DATE_TIME_FORMAT))
          .format(ISO_LOCAL_DATE_TIME);
    } catch (Exception ignored) {

    }

    try {
      return LocalDate.parse(value, ofPattern(BIRTHDAY_DATE_FORMAT))
          .format(ISO_LOCAL_DATE);
    } catch (Exception ignored) {

    }

    return value;
  }

}
