package ru.tecius.telemed.processor;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import ru.tecius.telemed.annotation.SearchInfo;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttributeConfig;

@AutoService(Processor.class)
@SupportedAnnotationTypes("ru.tecius.telemed.annotation.SearchInfo")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SearchInfoProcessor extends AbstractProcessor {

  private static final String SEARCH_INFO_PATH_TEMPLATE = "search-info/%s";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(SearchInfo.class)) {
      if (element.getKind() == ElementKind.CLASS) {
        generateFile((TypeElement) element);
      }
    }
    return true;
  }

  private void generateFile(TypeElement typeElement) {
    var annotation = typeElement.getAnnotation(SearchInfo.class);
    var className = typeElement.getSimpleName() + "SearchInfo";
    var packageName = processingEnv.getElementUtils().getPackageOf(typeElement).toString();

    // 1. Создаем основной класс
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Component.class)
        .addAnnotation(RequiredArgsConstructor.class)
        .addSuperinterface(getInterfaceTypeName(typeElement));

    // 2. Базовые поля: SCHEMA_NAME, TABLE_NAME, TABLE_LIAS
    addStaticConstants(classBuilder, annotation);

    // 3. Добавляем методы интерфейса
    addInterfaceMethods(classBuilder);

    var objectMapper = new ObjectMapper();
    var simpleConfigs = new ArrayList<SimpleSearchAttributeConfig>();
    for (var simplePath : annotation.simpleAttributePaths()) {
      simpleConfigs.add(
          getSearchAttributeConfig(objectMapper, SEARCH_INFO_PATH_TEMPLATE.formatted(simplePath),
              SimpleSearchAttributeConfig.class));
    }

    CodeBlock.Builder listInitializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    simpleConfigs.stream()
        .map(SimpleSearchAttributeConfig::simpleAttributes)
        .flatMap(Collection::stream)
        .forEach(attr -> {
          listInitializer.add("    new $T($S, $S)", SimpleSearchAttribute.class, attr.jsonField(),
              attr.dbField());
        });
    listInitializer.add(")");
    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(List.class, SimpleSearchAttribute.class),
            "simpleAttributes",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(listInitializer.build())
        .build());
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(List.class, SimpleSearchAttribute.class))
        .addStatement("return simpleAttributes")
        .build());

/*    var multipleConfigs = new ArrayList<MultipleSearchAttributeConfig>();
    for (var multiplePath : annotation.multipleAttributePaths()) {

    }*/

    // 3. Запись файла
    try {
      JavaFile.builder(packageName, classBuilder.build())
          //.indent("    ")
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (IOException e) {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR, "Filer Error: " + e.getMessage());
    }
  }

  private TypeName getInterfaceTypeName(TypeElement typeElement) {
    return ParameterizedTypeName.get(
        ClassName.get("ru.tecius.telemed.common", "SearchInfoInterface"),
        TypeName.get(typeElement.asType())
    );
  }

  private void addInterfaceMethods(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSchemaName")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return SCHEMA_NAME")
        .build());

    classBuilder.addMethod(MethodSpec.methodBuilder("getTablaName")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return TABLE_NAME")
        .build());

    classBuilder.addMethod(MethodSpec.methodBuilder("getTableAlias")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return TABLE_LIAS") // Используем ваше имя поля из кода
        .build());
  }

  private void addStaticConstants(TypeSpec.Builder classBuilder, SearchInfo annotation) {
    classBuilder.addField(createStaticStringField("SCHEMA_NAME", annotation.schema()));
    classBuilder.addField(createStaticStringField("TABLE_NAME", annotation.table()));
    classBuilder.addField(createStaticStringField("TABLE_LIAS", annotation.alias()));
  }

  private FieldSpec createStaticStringField(String name, String value) {
    return FieldSpec.builder(String.class, name)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$S", value)
        .build();
  }

  private <T> T getSearchAttributeConfig(ObjectMapper objectMapper, String path,
      Class<T> configClass) {
    return objectMapper.convertValue(
        new Yaml().load(this.getClass().getClassLoader().getResourceAsStream(path)),
        configClass);
  }

}
