package ru.tecius.telemed.processor.util;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ProcessorStaticUtils {

  public static String getTableAlias(String tableName, String tableAlias) {

    if (isNoneBlank(tableAlias)) {
      return tableAlias;
    }

    if (tableName.contains("_")) {
      return Arrays.stream(tableName.split("_"))
          .filter(StringUtils::isNoneBlank)
          .map(part -> String.valueOf(part.charAt(0)))
          .collect(joining())
          .toLowerCase();
    }

    return String.valueOf(tableName.trim().charAt(0)).toLowerCase();
  }

}
