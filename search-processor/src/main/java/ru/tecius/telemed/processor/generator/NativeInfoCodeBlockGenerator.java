package ru.tecius.telemed.processor.generator;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.configuration.AttributeType;
import ru.tecius.telemed.configuration.JoinData;
import ru.tecius.telemed.configuration.JoinInfo;
import ru.tecius.telemed.configuration.JoinReferenceData;
import ru.tecius.telemed.configuration.NativeSearchAttribute;
import ru.tecius.telemed.configuration.NativeSearchAttributeConfig;
import ru.tecius.telemed.enumeration.JoinTypeEnum;

public class NativeInfoCodeBlockGenerator {

  public CodeBlock generateSimpleAttributesBlock(List<NativeSearchAttributeConfig> configs) {
    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var simpleAttributes = configs.stream()
        .map(NativeSearchAttributeConfig::attributes)
        .flatMap(Collection::stream)
        .filter(attr -> isEmpty(attr.joinInfo()))
        .toList();

    for (var i = 0; i < simpleAttributes.size(); i++) {
      var attr = simpleAttributes.get(i);
      initializer.add("\tnew $T($T.$L, $S, $S, null, $T.class, null)",
          NativeSearchAttribute.class,
          AttributeType.class,
          attr.attributeType(),
          attr.jsonField(),
          attr.dbField(),
          attr.fieldType());
      if (i < simpleAttributes.size() - 1) {
        initializer.add(",\n");
      }
    }

    initializer.add(")");
    return initializer.build();
  }

  public CodeBlock generateMultipleAttributesBlock(List<NativeSearchAttributeConfig> configs) {
    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var multipleAttributes = configs.stream()
        .map(NativeSearchAttributeConfig::attributes)
        .flatMap(Collection::stream)
        .filter(attr -> isNotEmpty(attr.joinInfo()))
        .toList();

    for (var i = 0; i < multipleAttributes.size(); i++) {
      var attr = multipleAttributes.get(i);
      var joinsBlock = generateJoinInfoBlock(attr.joinInfo());

      initializer.add("\tnew $T($T.$L, $S, $S, $S, $T.class, $L)",
          NativeSearchAttribute.class,
          AttributeType.class,
          attr.attributeType(),
          attr.jsonField(),
          attr.dbField(),
          attr.dbTableAlias(),
          attr.fieldType(),
          joinsBlock);

      if (i < multipleAttributes.size() - 1) {
        initializer.add(",\n");
      }
    }

    return initializer.add(")").build();
  }

  private CodeBlock generateJoinInfoBlock(LinkedHashSet<JoinInfo> joinInfos) {
    return CodeBlock.builder()
        .add("new $T<>($T.of(\n", LinkedHashSet.class, List.class)
        .indent()
        .add(joinInfos.stream()
            .sorted(comparing(JoinInfo::order))
            .map(j -> CodeBlock.of(
                "new $T($L, new $T($S, $S, $S), new $T($S, $S, $S), $T.$L)",
                JoinInfo.class,
                j.order(),
                JoinReferenceData.class,
                j.reference().table(),
                j.reference().alias(),
                j.reference().column(),
                JoinData.class,
                j.join().table(),
                j.join().alias(),
                j.join().column(),
                JoinTypeEnum.class,
                j.type().name()
            ))
            .collect(CodeBlock.joining(",\n")))
        .unindent()
        .add("\n))")
        .build();
  }

}
