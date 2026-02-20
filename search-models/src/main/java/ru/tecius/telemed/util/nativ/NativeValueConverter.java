package ru.tecius.telemed.util.nativ;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static ru.tecius.telemed.util.Constants.BIRTHDAY_DATE_FORMATTER;
import static ru.tecius.telemed.util.Constants.DATE_CLASSES;
import static ru.tecius.telemed.util.Constants.INVALID_VALUE_FORMAT_ERROR_TEMPLATE;
import static ru.tecius.telemed.util.Constants.ISO_DATE_TIME_FORMATTER;
import static ru.tecius.telemed.util.Constants.LOCAL_DATE_TIME_FORMATTER;
import static util.Constants.BIRTHDAY_DATE_FORMAT;
import static util.Constants.ISO_DATE_FORMAT;
import static util.Constants.LOCAL_DATE_TIME_FORMAT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import ru.tecius.telemed.exception.ValidationException;

@UtilityClass
public final class NativeValueConverter {

  public static List<String> transformValues(List<String> values, Class<?> fieldType) {
    return Stream.of(fieldType)
        .filter(DATE_CLASSES::contains)
        .map(cls -> parseDateValues(values, cls))
        .findAny()
        .orElse(values);
  }

  public static List<String> transformValuesForLike(List<String> values) {
    return List.of("%" + values.getFirst() + "%");
  }

  public static List<String> transformValuesForBegin(List<String> values) {
    return List.of(values.getFirst() + "%");
  }

  public static List<String> transformValuesForEnd(List<String> values) {
    return List.of("%" + values.getFirst());
  }

  private static List<String> parseDateValues(List<String> values, Class<?> fieldType) {
    return values.stream()
        .map(value -> parseSingleDateValue(value, fieldType))
        .toList();
  }

  private static String parseSingleDateValue(String value, Class<?> fieldType) {
    return switch (fieldType) {
      case Class<?> c when c == OffsetDateTime.class -> parseSingleDateValue(() ->
              OffsetDateTime.parse(value, ISO_DATE_TIME_FORMATTER).format(ISO_OFFSET_DATE_TIME),
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
