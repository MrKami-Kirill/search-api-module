package ru.tecius.telemed.processor;

import static java.nio.file.Files.exists;
import static java.util.Objects.nonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import ru.tecius.telemed.annotation.SearchInfo;
import ru.tecius.telemed.configuration.JoinInfo;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.MultipleSearchAttributeConfig;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttributeConfig;
import ru.tecius.telemed.enumeration.JoinTypeEnum;

@AutoService(Processor.class)
@SupportedAnnotationTypes("ru.tecius.telemed.annotation.SearchInfo")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SearchInfoProcessor extends AbstractProcessor {

  private static final String SEARCH_INFO_PATH_TEMPLATE = "search-info/%s";
  private static final String RESOURCES_DIR_OPTION = "search.info.resources.dir";

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of(RESOURCES_DIR_OPTION);
  }

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

    // 2. Базовые поля: SCHEMA_NAME, TABLE_NAME, TABLE_ALIAS
    addStaticConstants(classBuilder, annotation, new ObjectMapper());

    // 3. Добавляем методы интерфейса
    addInterfaceMethods(classBuilder);

    // 4. Запись файла
    try {
      JavaFile.builder(packageName, classBuilder.build())
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (IOException e) {
      processingEnv.getMessager()
          .printMessage(ERROR, "Filer Error: " + e.getMessage());
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
        .addStatement("return TABLE_ALIAS")
        .build());

    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, SimpleSearchAttribute.class))
        .addStatement("return SIMPLE_ATTRIBUTES")
        .build());

    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, MultipleSearchAttribute.class))
        .addStatement("return MULTIPLE_ATTRIBUTES")
        .build());
  }

  private void addStaticConstants(TypeSpec.Builder classBuilder, SearchInfo annotation,
      ObjectMapper objectMapper) {
    classBuilder.addField(createStaticStringField("SCHEMA_NAME", annotation.schema()));
    classBuilder.addField(createStaticStringField("TABLE_NAME", annotation.table()));
    classBuilder.addField(createStaticStringField("TABLE_ALIAS", annotation.alias()));
    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, SimpleSearchAttribute.class),
            "SIMPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(getSimpleAttributesInitializer(annotation, objectMapper))
        .build());

    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, MultipleSearchAttribute.class),
            "MULTIPLE_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(getMultipleAttributesInitializer(annotation, objectMapper))
        .build());

  }

  private CodeBlock getSimpleAttributesInitializer(SearchInfo annotation,
      ObjectMapper objectMapper) {
    var simpleConfigs = new ArrayList<SimpleSearchAttributeConfig>();
    for (var simplePath : annotation.simpleAttributePaths()) {
      simpleConfigs.add(
          getSearchAttributeConfig(objectMapper, SEARCH_INFO_PATH_TEMPLATE.formatted(simplePath),
              SimpleSearchAttributeConfig.class));
    }

    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var simpleAttributes = simpleConfigs.stream()
        .map(SimpleSearchAttributeConfig::simpleAttributes)
        .flatMap(Collection::stream)
        .toList();
    var simpleAttributesSize = simpleAttributes.size();
    for (var i = 0; i < simpleAttributesSize; i++) {
      var simpleAttribute = simpleAttributes.get(i);
      initializer.add("new $T($S, $S)", SimpleSearchAttribute.class,
          simpleAttribute.jsonField(), simpleAttribute.dbField());
      if (i < simpleAttributesSize - 1) {
        initializer.add(",\n");
      }
    }

    initializer.add(")");
    return initializer.build();

  }

  private CodeBlock getMultipleAttributesInitializer(SearchInfo annotation,
      ObjectMapper objectMapper) {
    var multipleConfigs = new ArrayList<MultipleSearchAttributeConfig>();
    for (var multiplePath : annotation.multipleAttributePaths()) {
      multipleConfigs.add(getSearchAttributeConfig(objectMapper,
          SEARCH_INFO_PATH_TEMPLATE.formatted(multiplePath), MultipleSearchAttributeConfig.class));
    }

    var initializer = CodeBlock.builder().add("$T.of(\n", Set.class);
    var multipleAttributes = multipleConfigs.stream()
        .map(MultipleSearchAttributeConfig::multipleAttributes)
        .flatMap(Collection::stream)
        .toList();

    for (int i = 0; i < multipleAttributes.size(); i++) {
      var attr = multipleAttributes.get(i);

      // Вложенный блок для Set<JoinInfo>
      CodeBlock joinsBlock = CodeBlock.builder()
          .add("$T.of(\n", Set.class)
          .add(attr.joinInfo().stream()
              .map(j -> CodeBlock.of("new $T($L, $S, $S, $S, $S, $T.$L)",
                  JoinInfo.class,
                  j.order(),                  // Integer
                  j.referenceJoinColumn(),    // String
                  j.joinTable(),              // String
                  j.joinTableAlias(),         // String
                  j.joinColumn(),             // String
                  JoinTypeEnum.class,
                  j.joinType().name()         // Значение Enum
              ))
              .collect(CodeBlock.joining(",\n")))
          .add("\n)")
          .build();

      initializer.add("    new $T($S, $S, $L)",
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

  private FieldSpec createStaticStringField(String name, String value) {
    return FieldSpec.builder(String.class, name)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$S", value)
        .build();
  }

  private <T> T getSearchAttributeConfig(ObjectMapper objectMapper, String path,
      Class<T> configClass) {
    try {
      var options = processingEnv.getOptions();
      var resourcesDir = options.get(RESOURCES_DIR_OPTION);

      if (nonNull(resourcesDir)) {
        var filePath = Paths.get(resourcesDir, path);
        if (exists(filePath)) {
          try (var inputStream = new FileInputStream(filePath.toFile())) {
            return objectMapper.convertValue(new Yaml().load(inputStream), configClass);
          }
        }
      }

      var errorMessage = "Cannot find resource file: %s".formatted(path);
      processingEnv.getMessager().printMessage(ERROR, errorMessage);
      throw new RuntimeException(errorMessage);
    } catch (IOException ex) {
      var errorMessage = "Error loading resource: %s".formatted(path);
      processingEnv.getMessager().printMessage(ERROR, errorMessage);
      throw new RuntimeException(errorMessage, ex);
    }
  }

}
