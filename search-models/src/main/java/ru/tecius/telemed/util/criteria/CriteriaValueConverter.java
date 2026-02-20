package ru.tecius.telemed.util.criteria;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import ru.tecius.telemed.exception.ValidationException;

public final class CriteriaValueConverter {

  private CriteriaValueConverter() {
    // Utility class - prevent instantiation
  }

  public static Object convertValue(String value, Class<?> fieldType) {
    if (value == null) {
      return null;
    }

    return switch (fieldType) {
      case Class<?> c when c == Long.class -> Long.parseLong(value);
      case Class<?> c when c == Integer.class -> Integer.parseInt(value);
      case Class<?> c when c == Double.class -> Double.parseDouble(value);
      case Class<?> c when c == Float.class -> Float.parseFloat(value);
      case Class<?> c when c == Boolean.class -> Boolean.parseBoolean(value);
      default -> value;
    };
  }

  public static Object[] convertValues(List<String> values, Class<?> fieldType) {
    return values.stream()
        .map(v -> convertValue(v, fieldType))
        .toArray();
  }

  @SuppressWarnings("unchecked,rawtypes")
  public static Comparable parseDateValue(String value, Class<?> fieldType) {
    try {
      return switch (fieldType) {
        case Class<?> c when c == OffsetDateTime.class -> OffsetDateTime.parse(value,
            ISO_OFFSET_DATE_TIME);
        case Class<?> c when c == LocalDateTime.class -> LocalDateTime.parse(value,
            ISO_LOCAL_DATE_TIME);
        case Class<?> c when c == LocalDate.class -> LocalDate.parse(value,
            ISO_LOCAL_DATE);
        default -> throw new IllegalArgumentException("Unsupported date type: " + fieldType);
      };
    } catch (Exception e) {
      throw new ValidationException(
          "Ошибка парсинга даты: %s для типа %s".formatted(value, fieldType), e);
    }
  }
}
