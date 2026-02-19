package ru.tecius.telemed.processor.generator.nativ;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.RequiredArgsConstructor;
import ru.tecius.telemed.annotation.SearchInfo;
import ru.tecius.telemed.common.nativ.SearchInfoInterface;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttribute;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttributeConfig;

@RequiredArgsConstructor
public class NativeInfoClassGenerator {

  private final NativeInfoMethodGenerator nativeInfoMethodGenerator = new NativeInfoMethodGenerator();
  private final NativeInfoCodeBlockGenerator nativeInfoCodeBlockGenerator = new NativeInfoCodeBlockGenerator();

  public TypeSpec generateClassSpec(
      TypeElement typeElement,
      SearchInfo annotation,
      List<NativeSearchAttributeConfig> configs
  ) {

    var entityClassName = ClassName.get(typeElement);
    var searchInfoInterface = ParameterizedTypeName.get(
        ClassName.get(SearchInfoInterface.class),
        entityClassName);

    var classBuilder = TypeSpec.classBuilder(getClassName(typeElement))
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(org.springframework.stereotype.Component.class)
        .addSuperinterface(searchInfoInterface);

    addStaticConstants(classBuilder, annotation, configs);
    nativeInfoMethodGenerator.addInterfaceMethods(classBuilder);

    return classBuilder.build();
  }

  private String getClassName(TypeElement typeElement) {
    return typeElement.getSimpleName() + "SearchInfo";
  }

  private void addStaticConstants(
      TypeSpec.Builder classBuilder,
      SearchInfo annotation,
      List<NativeSearchAttributeConfig> configs
  ) {

    classBuilder.addField(createStaticStringField("SCHEMA_NAME", annotation.schema()));
    classBuilder.addField(createStaticStringField("TABLE_NAME", annotation.table()));
    classBuilder.addField(createStaticStringField("TABLE_ALIAS", annotation.alias()));

    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, NativeSearchAttribute.class),
            "SIMPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(nativeInfoCodeBlockGenerator.generateSimpleAttributesBlock(
            configs))
        .build());

    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, NativeSearchAttribute.class),
            "MULTIPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(nativeInfoCodeBlockGenerator.generateMultipleAttributesBlock(
            configs))
        .build());
  }

  private FieldSpec createStaticStringField(String name, String value) {
    return FieldSpec.builder(String.class, name)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$S", value)
        .build();
  }

}
