package ru.tecius.telemed.processor.generator.nativ;

import static ru.tecius.telemed.configuration.common.AttributeType.MULTIPLE;
import static ru.tecius.telemed.configuration.common.AttributeType.SIMPLE;
import static ru.tecius.telemed.processor.util.ProcessorStaticUtils.getTableAlias;

import com.squareup.javapoet.CodeBlock;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.tecius.telemed.configuration.common.AttributeType;
import ru.tecius.telemed.configuration.nativ.DbData;
import ru.tecius.telemed.configuration.nativ.JoinData;
import ru.tecius.telemed.configuration.nativ.JoinInfo;
import ru.tecius.telemed.configuration.nativ.JoinReferenceData;
import ru.tecius.telemed.configuration.nativ.JoinTypeEnum;
import ru.tecius.telemed.configuration.nativ.JsonData;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttribute;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttributeConfig;

public class NativeInfoCodeBlockGenerator {

  public CodeBlock generateSimpleAttributesBlock(List<NativeSearchAttributeConfig> configs) {
    return generateAttributesBlockByType(configs, SIMPLE);
  }

  public CodeBlock generateMultipleAttributesBlock(List<NativeSearchAttributeConfig> configs) {
    return generateAttributesBlockByType(configs, MULTIPLE);
  }

  /**
   * Универсальный метод для генерации блока Set<NativeSearchAttribute>
   */
  private CodeBlock generateAttributesBlockByType(List<NativeSearchAttributeConfig> configs, AttributeType type) {
    var attributes = configs.stream()
        .map(NativeSearchAttributeConfig::attributes)
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
   * Генерирует: new NativeSearchAttribute(...)
   */
  private CodeBlock generateAttributeConstructorBlock(NativeSearchAttribute attr) {
    var db = attr.db();
    return CodeBlock.builder()
        .add("new $T(\n", NativeSearchAttribute.class)
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
        .map(join -> {
          var ref = join.reference();
          var jd = join.join();
          return CodeBlock.of("new $T($L, new $T($S, $S, $S), new $T($S, $S, $S), $T.$L)",
              JoinInfo.class,
              join.order(),
              JoinReferenceData.class, ref.table(), getTableAlias(ref.table(), ref.alias()), ref.column(),
              JoinData.class, jd.table(), getTableAlias(jd.table(), jd.alias()), jd.column(),
              JoinTypeEnum.class, join.type());
        })
        .collect(CodeBlock.joining(",\n"));

    return CodeBlock.builder()
        .add("new $T<>($T.asList(\n", LinkedHashSet.class, Arrays.class)
        .indent().add(joins).unindent()
        .add("))")
        .build();
  }

}
