package ru.tecius.telemed.util;

import static java.time.format.DateTimeFormatter.ofPattern;
import static util.Constants.BIRTHDAY_DATE_FORMAT;
import static util.Constants.ISO_DATE_FORMAT;
import static util.Constants.LOCAL_DATE_TIME_FORMAT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String INVALID_VALUE_FORMAT_ERROR_TEMPLATE =
      "Переданное значение %s не соответствует формату %s";

  public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ofPattern(ISO_DATE_FORMAT);

  public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
      ofPattern(LOCAL_DATE_TIME_FORMAT);

  public static final DateTimeFormatter BIRTHDAY_DATE_FORMATTER = ofPattern(BIRTHDAY_DATE_FORMAT);

  public static final List<Class<?>> DATE_CLASSES = List.of(
      OffsetDateTime.class, LocalDateTime.class, LocalDate.class);

}
