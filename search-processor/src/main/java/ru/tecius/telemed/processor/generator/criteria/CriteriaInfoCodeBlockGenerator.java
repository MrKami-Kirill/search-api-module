package ru.tecius.telemed.processor.generator.criteria;

import static ru.tecius.telemed.configuration.common.AttributeType.MULTIPLE;
import static ru.tecius.telemed.configuration.common.AttributeType.SIMPLE;

import com.squareup.javapoet.CodeBlock;
import jakarta.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.criteria.JoinInfo;
import ru.tecius.telemed.configuration.common.AttributeType;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttributeConfig;
import ru.tecius.telemed.configuration.criteria.DbData;
import ru.tecius.telemed.configuration.criteria.JsonData;

public class CriteriaInfoCodeBlockGenerator {

  public CodeBlock generateSimpleAttributesBlock(List<CriteriaSearchAttributeConfig> configs) {
    return generateAttributesBlockByType(configs, SIMPLE);
  }

  public CodeBlock generateMultipleAttributesBlock(List<CriteriaSearchAttributeConfig> configs) {
    return generateAttributesBlockByType(configs, MULTIPLE);
  }

  /**
   * Универсальный метод для генерации блока Set<CriteriaSearchAttribute>
   */
  private CodeBlock generateAttributesBlockByType(List<CriteriaSearchAttributeConfig> configs, AttributeType type) {
    var attributes = configs.stream()
        .map(CriteriaSearchAttributeConfig::attributes)
        .flatMap(Collection::stream)
        .filter(attr -> Objects.equals(attr.type(), type))
        .toList();

    // Собираем атрибуты, разделяя их запятой с новой строкой
    CodeBlock attributesJoined = attributes.stream()
        .map(this::generateAttributeConstructorBlock)
        .collect(CodeBlock.joining(",\n"));

    return CodeBlock.builder()
        .add("new $T<>($T.asList(\n", LinkedHashSet.class, Arrays.class)
        .indent()
        .add(attributesJoined)
        .unindent()
        .add("\n))")
        .build();
  }

  /**
   * Генерирует: new CriteriaSearchAttribute(...)
   */
  private CodeBlock generateAttributeConstructorBlock(CriteriaSearchAttribute attr) {
    var db = attr.db();
    return CodeBlock.builder()
        .add("new $T(\n", CriteriaSearchAttribute.class)
        .indent()
        .add("$T.$L,\n", AttributeType.class, attr.type())
        .add("new $T(\n$S\n),\n", JsonData.class, attr.json().key())
        .add("new $T(\n$S,\n$T.class,\n$L\n)",
            DbData.class, db.column(), db.type(), generateJoinInfoBlock(db.joinInfo()))
        .unindent()
        .add("\n)")
        .build();
  }

  /**
   * Генерирует блок для JoinInfo (Set или null)
   */
  private CodeBlock generateJoinInfoBlock(Set<JoinInfo> joinInfo) {
    if (joinInfo == null || joinInfo.isEmpty()) {
      return CodeBlock.of("null");
    }

    CodeBlock joins = joinInfo.stream()
        .map(join -> CodeBlock.of("new $T($L, $S, $T.$L)",
            JoinInfo.class,
            join.order(),
            join.path(),
            JoinType.class, join.type()))
        .collect(CodeBlock.joining(",\n"));

    return CodeBlock.builder()
        .add("new $T<>($T.asList(\n", LinkedHashSet.class, Arrays.class)
        .indent().add(joins).unindent()
        .add("))")
        .build();
  }

}
