package ru.tecius.telemed.processor.generator;

import static java.util.Comparator.comparing;

import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.configuration.FieldType;
import ru.tecius.telemed.configuration.JoinData;
import ru.tecius.telemed.configuration.JoinInfo;
import ru.tecius.telemed.configuration.JoinReferenceData;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.MultipleSearchAttributeConfig;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttributeConfig;
import ru.tecius.telemed.enumeration.JoinTypeEnum;

public class CodeBlockGenerator {

  public CodeBlock generateSimpleAttributesBlock(List<SimpleSearchAttributeConfig> configs) {
    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var simpleAttributes = configs.stream()
        .map(SimpleSearchAttributeConfig::simpleAttributes)
        .flatMap(Collection::stream)
        .toList();

    for (var i = 0; i < simpleAttributes.size(); i++) {
      var attr = simpleAttributes.get(i);
      initializer.add("\tnew $T($S, $S, $T.$L)",
          SimpleSearchAttribute.class,
          attr.jsonField(),
          attr.dbField(),
          FieldType.class,
          attr.fieldType());
      if (i < simpleAttributes.size() - 1) {
        initializer.add(",\n");
      }
    }

    initializer.add(")");
    return initializer.build();
  }

  public CodeBlock generateMultipleAttributesBlock(List<MultipleSearchAttributeConfig> configs) {
    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var multipleAttributes = configs.stream()
        .map(MultipleSearchAttributeConfig::multipleAttributes)
        .flatMap(Collection::stream)
        .toList();

    for (var i = 0; i < multipleAttributes.size(); i++) {
      var attr = multipleAttributes.get(i);
      var joinsBlock = generateJoinInfoBlock(attr.joinInfo());

      initializer.add("\tnew $T($S, $S, $S, $T.$L, $L)",
          MultipleSearchAttribute.class,
          attr.jsonField(),
          attr.dbField(),
          attr.dbTableAlias(),
          FieldType.class,
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
