package ru.tecius.telemed.processor;

import static java.util.Objects.isNull;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.tecius.telemed.annotation.SearchInfo;
import ru.tecius.telemed.processor.config.ConfigLoader;
import ru.tecius.telemed.processor.error.ErrorHandler;
import ru.tecius.telemed.processor.generator.ClassGenerator;
import ru.tecius.telemed.processor.util.ProcessorConstants;

@AutoService(Processor.class)
@SupportedAnnotationTypes("ru.tecius.telemed.annotation.SearchInfo")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SearchInfoProcessor extends AbstractProcessor {

  private ConfigLoader configLoader;
  private ClassGenerator classGenerator;
  private ErrorHandler errorHandler;

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of(ProcessorConstants.RESOURCES_DIR_OPTION);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    initHelpers();
    for (var element : roundEnv.getElementsAnnotatedWith(SearchInfo.class)) {
      if (element.getKind() == ElementKind.CLASS) {
        generateFile((TypeElement) element);
      }
    }

    return true;
  }

  private void initHelpers() {
    if (isNull(configLoader)) {
      var factoryBean = new LocalValidatorFactoryBean();
      factoryBean.afterPropertiesSet();
      configLoader = new ConfigLoader(processingEnv, factoryBean);
      classGenerator = new ClassGenerator();
      errorHandler = new ErrorHandler(processingEnv.getMessager());
    }
  }

  private void generateFile(TypeElement typeElement) {
    try {
      var annotation = typeElement.getAnnotation(SearchInfo.class);
      var packageName = processingEnv.getElementUtils().getPackageOf(typeElement).toString();

      var simpleConfigs = configLoader.loadSimpleConfigs(annotation.simpleAttributePaths());
      var multipleConfigs = configLoader.loadMultipleConfigs(annotation.multipleAttributePaths());

      var classSpec = classGenerator.generateClassSpec(typeElement, annotation, simpleConfigs, multipleConfigs);

      JavaFile.builder(packageName, classSpec)
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (IOException ex) {
      errorHandler.reportError("Filer Error: %s".formatted(ex.getMessage()));
    }
  }

}
