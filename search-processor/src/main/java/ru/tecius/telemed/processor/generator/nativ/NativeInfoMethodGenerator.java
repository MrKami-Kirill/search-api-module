package ru.tecius.telemed.processor.generator.nativ;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.nativ.JoinInfo;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttribute;

public class NativeInfoMethodGenerator {

  public void addInterfaceMethods(TypeSpec.Builder classBuilder) {
    addGetSchemaNameMethod(classBuilder);
    addGetTableNameMethod(classBuilder);
    addGetTableAliasMethod(classBuilder);
    addGetFullTableNameMethod(classBuilder);
    addGetSimpleAttributesMethod(classBuilder);
    addGetSimpleAttributeByJsonFieldMethod(classBuilder);
    addGetMultipleAttributesMethod(classBuilder);
    addGetMultipleAttributeByJsonKeyMethod(classBuilder);
    addCreateJoinStringMethod(classBuilder);
    addGetFullColumnNameByAttributeMethod(classBuilder);
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
        .returns(ParameterizedTypeName.get(Set.class, NativeSearchAttribute.class))
        .addStatement("return SIMPLE_ATTRIBUTES")
        .build());
  }

  private void addGetSimpleAttributeByJsonFieldMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributeByJsonKey")
        .addParameter(String.class, "jsonField")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, NativeSearchAttribute.class))
        .addStatement("""
            return SIMPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.json().key(), jsonField))
                    .findAny()""")
        .build());
  }

  private void addGetMultipleAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, NativeSearchAttribute.class))
        .addStatement("return MULTIPLE_ATTRIBUTES")
        .build());
  }

  private void addGetMultipleAttributeByJsonKeyMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributeByJsonKey")
        .addParameter(String.class, "key")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, NativeSearchAttribute.class))
        .addStatement("""
            return MULTIPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.json().key(), key))
                    .findAny()""")
        .build());
  }

  private void addCreateJoinStringMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("createJoinString")
        .addParameter(JoinInfo.class, "joinInfo")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("""
            return "%s %s.%s AS %s ON %s.%s = %s.%s".formatted(joinInfo.type().getValue(),
                    SCHEMA_NAME,
                    joinInfo.join().table(), joinInfo.join().alias(),
                    joinInfo.reference().alias(), joinInfo.reference().column(),
                    joinInfo.join().alias(), joinInfo.join().column())""")
        .build());
  }

  private void addGetFullColumnNameByAttributeMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getFullColumnNameByAttribute")
        .addParameter(NativeSearchAttribute.class, "attribute")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(String.class)
        .addStatement("""
            return switch (attribute.type()) {
              case SIMPLE -> FULL_DB_COLUMN_NAME_TEMPLATE
                  .formatted(getTableAlias(), attribute.db().column());
              case MULTIPLE -> FULL_DB_COLUMN_NAME_TEMPLATE
                  .formatted(attribute.db().joinInfo().getLast().join().alias(),
                      attribute.db().column());
            }""")
        .build());
  }

}
