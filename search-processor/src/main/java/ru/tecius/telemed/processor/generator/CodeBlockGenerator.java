package ru.tecius.telemed.processor.generator;

import static java.util.Comparator.comparing;

import com.squareup.javapoet.CodeBlock;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.configuration.JoinInfo;
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
      var simpleAttribute = simpleAttributes.get(i);
      initializer.add("\tnew $T($S, $S)", SimpleSearchAttribute.class,
          simpleAttribute.jsonField(), simpleAttribute.dbField());
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

      initializer.add("\tnew $T($S, $S, $L)",
          MultipleSearchAttribute.class,
          attr.jsonField(),
          attr.dbField(),
          joinsBlock);

      if (i < multipleAttributes.size() - 1) {
        initializer.add(",\n");
      }
    }

    return initializer.add("\n)").build();
  }

  private CodeBlock generateJoinInfoBlock(Set<JoinInfo> joinInfos) {
    return CodeBlock.builder()
        .add("$T.of(\n", Set.class)
        .add(joinInfos.stream()
            .sorted(comparing(JoinInfo::order))
            .map(j -> CodeBlock.of("\t\tnew $T($L, $S, $S, $S, $S, $T.$L)",
                JoinInfo.class,
                j.order(),
                j.referenceJoinColumn(),
                j.joinTable(),
                j.joinTableAlias(),
                j.joinColumn(),
                JoinTypeEnum.class,
                j.joinType().name()
            ))
            .collect(CodeBlock.joining(",\n")))
        .add("\n)")
        .build();
  }

}
