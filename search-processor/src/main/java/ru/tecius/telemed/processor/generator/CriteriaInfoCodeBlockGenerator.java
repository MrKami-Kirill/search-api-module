package ru.tecius.telemed.processor.generator;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.configuration.CriteriaJoinInfo;
import ru.tecius.telemed.configuration.CriteriaJoinType;
import ru.tecius.telemed.configuration.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.CriteriaSearchAttributeConfig;
import ru.tecius.telemed.configuration.FieldType;

/**
 * Генератор блоков кода для CriteriaInfoInterface.
 */
public class CriteriaInfoCodeBlockGenerator {

  /**
   * Генерирует блок инициализации CRITERIA_ATTRIBUTES.
   */
  public CodeBlock generateCriteriaAttributesBlock(List<CriteriaSearchAttributeConfig> configs) {
    var builder = CodeBlock.builder();

    var allAttributes = new ArrayList<CriteriaSearchAttribute>();
    for (var config : configs) {
      allAttributes.addAll(config.attributes());
    }

    if (allAttributes.isEmpty()) {
      builder.add("$T.of()", Set.class);
      return builder.build();
    }

    // Создаем через Arrays.asList и new LinkedHashSet
    builder.add("new $T<$T>($T.asList(\n", LinkedHashSet.class, CriteriaSearchAttribute.class, Arrays.class);

    for (int i = 0; i < allAttributes.size(); i++) {
      var attr = allAttributes.get(i);
      builder.add("  new $T(\n", CriteriaSearchAttribute.class);
      builder.add("    $S,\n", attr.jsonField());
      builder.add("    $S,\n", attr.entityPath());
      builder.add("    $T.$L,\n", FieldType.class, attr.fieldType());

      if (attr.joinInfo() != null && !attr.joinInfo().isEmpty()) {
        builder.add("    new $T<$T>($T.asList(\n", LinkedHashSet.class, CriteriaJoinInfo.class, Arrays.class);
        var joins = new ArrayList<>(attr.joinInfo());
        for (int j = 0; j < joins.size(); j++) {
          var join = joins.get(j);
          builder.add("      new $T(\n", CriteriaJoinInfo.class);
          builder.add("        $S,\n", join.path());
          builder.add("        $S,\n", join.alias());
          builder.add("        $T.$L\n", CriteriaJoinType.class, join.type());
          builder.add(j < joins.size() - 1 ? "      ),\n" : "      )\n");
        }
        builder.add("    ))\n");
      } else {
        builder.add("    null\n");
      }

      builder.add(i < allAttributes.size() - 1 ? "  ),\n" : "  )\n");
    }

    builder.add("))");
    return builder.build();
  }

  /**
   * Генерирует блок инициализации ALL_JOINS.
   */
  public CodeBlock generateAllJoinsBlock(List<CriteriaSearchAttributeConfig> configs) {
    var builder = CodeBlock.builder();

    var allJoins = configs.stream()
        .flatMap(config -> config.attributes().stream())
        .filter(attr -> attr.joinInfo() != null && !attr.joinInfo().isEmpty())
        .flatMap(attr -> attr.joinInfo().stream())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    if (allJoins.isEmpty()) {
      builder.add("$T.of()", Set.class);
      return builder.build();
    }

    // Создаем через Arrays.asList и new HashSet
    builder.add("new $T<$T>($T.asList(\n", LinkedHashSet.class, CriteriaJoinInfo.class, Arrays.class);

    var joinsList = new ArrayList<>(allJoins);
    for (int i = 0; i < joinsList.size(); i++) {
      var join = joinsList.get(i);
      builder.add("  new $T(\n", CriteriaJoinInfo.class);
      builder.add("    $S,\n", join.path());
      builder.add("    $S,\n", join.alias());
      builder.add("    $T.$L\n", CriteriaJoinType.class, join.type());
      builder.add(i < joinsList.size() - 1 ? "  ),\n" : "  )\n");
    }

    builder.add("))");
    return builder.build();
  }
}
