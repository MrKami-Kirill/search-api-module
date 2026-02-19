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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import ru.tecius.telemed.exception.ValidationException;

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

  private static final String INVALID_VALUE_FORMAT_ERROR_TEMPLATE =
      "Переданное значение %s не соответствует формату %s";
  private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ofPattern(ISO_DATE_FORMAT);
  private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
      ofPattern(LOCAL_DATE_TIME_FORMAT);
  private static final DateTimeFormatter BIRTHDAY_DATE_FORMATTER = ofPattern(BIRTHDAY_DATE_FORMAT);

  private final BiFunction<String, List<String>, String> sqlTemplateFunction;
  private final Predicate<List<String>> valuePredicate;
  private final BiFunction<List<String>, Class<?>, List<String>> nativeTransformValueFunction;
//  private final BiFunction<List<String>, Pair<FieldType, CriteriaBuilder>, Object>
//      criteriaTransformFunction;

  public String buildNativeCondition(String dbField, List<String> values) {
    valuePredicate.test(values);
    return sqlTemplateFunction.apply(dbField, values);
  }

  public Object buildCriteriaCondition(List<String> values) {
    valuePredicate.test(values);
    return null;
  }

  public static List<String> transformValues(List<String> values, Class<?> fieldType) {
    return Stream.of(fieldType)
        .filter(cls -> List.of(OffsetDateTime.class, LocalDateTime.class, LocalDate.class)
            .contains(cls))
        .map(cls -> parseDateValues(values, cls))
        .findAny()
        .orElse(values);
  }

  private static List<String> parseDateValues(List<String> values, Class<?> fieldType) {
    return values.stream()
        .map(value -> parseSingleDateValue(value, fieldType))
        .toList();
  }

  private static String parseSingleDateValue(String value, Class<?> fieldType) {
    return switch (fieldType) {
      case Class<?> c when c == OffsetDateTime.class -> parseSingleDateValue(() ->
              OffsetDateTime.parse(value, ISO_DATE_TIME_FORMATTER).toString(),
          INVALID_VALUE_FORMAT_ERROR_TEMPLATE.formatted(value, ISO_DATE_FORMAT));
      case Class<?> c when c == LocalDateTime.class -> parseSingleDateValue(() ->
              LocalDateTime.parse(value, LOCAL_DATE_TIME_FORMATTER).format(ISO_LOCAL_DATE_TIME),
          INVALID_VALUE_FORMAT_ERROR_TEMPLATE.formatted(value, LOCAL_DATE_TIME_FORMAT));
      case Class<?> c when c == LocalDate.class -> parseSingleDateValue(() ->
              LocalDate.parse(value, BIRTHDAY_DATE_FORMATTER).format(ISO_LOCAL_DATE),
          INVALID_VALUE_FORMAT_ERROR_TEMPLATE.formatted(value, BIRTHDAY_DATE_FORMAT));
      default -> value;
    };
  }

  private static String parseSingleDateValue(Supplier<String> supplier, String errorMessage) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      throw new ValidationException(errorMessage, ex);
    }
  }

}
