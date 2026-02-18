package ru.tecius.telemed.processor.generator;

import static ru.tecius.telemed.processor.util.TypeResolver.createStaticStringField;
import static ru.tecius.telemed.processor.util.TypeResolver.getInterfaceTypeName;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.RequiredArgsConstructor;
import ru.tecius.telemed.annotation.SearchInfo;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.MultipleSearchAttributeConfig;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttributeConfig;

@RequiredArgsConstructor
public class ClassGenerator {

  private final MethodGenerator methodGenerator = new MethodGenerator();
  private final CodeBlockGenerator codeBlockGenerator = new CodeBlockGenerator();

  public TypeSpec generateClassSpec(
      TypeElement typeElement,
      SearchInfo annotation,
      List<SimpleSearchAttributeConfig> simpleConfigs,
      List<MultipleSearchAttributeConfig> multipleConfigs
  ) {

    var classBuilder = TypeSpec.classBuilder(getClassName(typeElement))
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(org.springframework.stereotype.Component.class)
        .addSuperinterface(getInterfaceTypeName(typeElement));

    addStaticConstants(classBuilder, annotation, simpleConfigs, multipleConfigs);
    methodGenerator.addInterfaceMethods(classBuilder);

    return classBuilder.build();
  }

  private String getClassName(TypeElement typeElement) {
    return typeElement.getSimpleName() + "SearchInfo";
  }

  private void addStaticConstants(
      TypeSpec.Builder classBuilder,
      SearchInfo annotation,
      List<SimpleSearchAttributeConfig> simpleConfigs,
      List<MultipleSearchAttributeConfig> multipleConfigs
  ) {

    classBuilder.addField(createStaticStringField("SCHEMA_NAME", annotation.schema()));
    classBuilder.addField(createStaticStringField("TABLE_NAME", annotation.table()));
    classBuilder.addField(createStaticStringField("TABLE_ALIAS", annotation.alias()));

    var simpleAttributesBlock = codeBlockGenerator.generateSimpleAttributesBlock(
        simpleConfigs);
    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, SimpleSearchAttribute.class),
            "SIMPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(simpleAttributesBlock)
        .build());

    CodeBlock multipleAttributesBlock = codeBlockGenerator.generateMultipleAttributesBlock(
        multipleConfigs);
    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, MultipleSearchAttribute.class),
            "MULTIPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(multipleAttributesBlock)
        .build());
  }

}
