package ru.tecius.telemed.processor.generator;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;

public class MethodGenerator {

  public void addInterfaceMethods(TypeSpec.Builder classBuilder) {
    addGetSchemaNameMethod(classBuilder);
    addGetTableNameMethod(classBuilder);
    addGetTableAliasMethod(classBuilder);
    addGetFullTableNameMethod(classBuilder);
    addGetSimpleAttributesMethod(classBuilder);
    addGetSimpleAttributeByJsonFieldMethod(classBuilder);
    addGetMultipleAttributesMethod(classBuilder);
    addGetMultipleAttributeByJsonFieldMethod(classBuilder);
  }

  private void addGetSchemaNameMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSchemaName")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("return SCHEMA_NAME")
        .build());
  }

  private void addGetTableNameMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getTablaName")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("return TABLE_NAME")
        .build());
  }

  private void addGetTableAliasMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getTableAlias")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("return TABLE_ALIAS")
        .build());
  }

  private void addGetFullTableNameMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getFullTableName")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("""
            return "%s.%s AS %s".formatted(SCHEMA_NAME, TABLE_NAME, TABLE_ALIAS)""")
        .build());
  }

  private void addGetSimpleAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, SimpleSearchAttribute.class))
        .addStatement("return SIMPLE_ATTRIBUTES")
        .build());
  }

  private void addGetSimpleAttributeByJsonFieldMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributeByJsonField")
        .addParameter(String.class, "jsonField")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, SimpleSearchAttribute.class))
        .addStatement("""
            return SIMPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.jsonField(), jsonField))
                    .findAny()""")
        .build());
  }

  private void addGetMultipleAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, MultipleSearchAttribute.class))
        .addStatement("return MULTIPLE_ATTRIBUTES")
        .build());
  }

  private void addGetMultipleAttributeByJsonFieldMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributeByJsonField")
        .addParameter(String.class, "jsonField")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, MultipleSearchAttribute.class))
        .addStatement("""
            return MULTIPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.jsonField(), jsonField))
                    .findAny()""")
        .build());
  }

}
