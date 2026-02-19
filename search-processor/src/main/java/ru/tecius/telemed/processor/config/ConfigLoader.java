package ru.tecius.telemed.processor.config;

import static java.nio.file.Files.exists;
import static java.util.Objects.nonNull;
import static ru.tecius.telemed.processor.util.ProcessorConstants.RESOURCES_DIR_OPTION;
import static ru.tecius.telemed.processor.util.ProcessorConstants.SEARCH_INFO_PATH_TEMPLATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import org.springframework.validation.Validator;
import org.yaml.snakeyaml.Yaml;
import ru.tecius.telemed.configuration.nativ.CriteriaSearchAttributeConfig;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttributeConfig;
import ru.tecius.telemed.processor.error.ErrorHandler;
import ru.tecius.telemed.exception.ProcessingException;
import ru.tecius.telemed.processor.validator.ValidationHandler;

public class ConfigLoader {

  private final ProcessingEnvironment processingEnv;
  private final ObjectMapper objectMapper;
  private final ErrorHandler errorHandler;
  private final ValidationHandler<NativeSearchAttributeConfig> nativeConfigValidator;
  private final ValidationHandler<CriteriaSearchAttributeConfig> criteriaConfigValidator;

  public ConfigLoader(ProcessingEnvironment processingEnv, Validator validator) {
    this.processingEnv = processingEnv;
    this.objectMapper = new ObjectMapper();
    this.errorHandler = new ErrorHandler(processingEnv.getMessager());
    this.nativeConfigValidator = new ValidationHandler<>(validator, this.errorHandler);
    this.criteriaConfigValidator = new ValidationHandler<>(validator, this.errorHandler);
  }

  public List<NativeSearchAttributeConfig> loadNativeConfigs(String[] paths) {
    var configs = new ArrayList<NativeSearchAttributeConfig>();
    for (var path : paths) {
      configs.add(nativeConfigValidator.validate(
          loadConfig(SEARCH_INFO_PATH_TEMPLATE.formatted(path),
              NativeSearchAttributeConfig.class)));
    }
    return configs;
  }

  public List<CriteriaSearchAttributeConfig> loadCriteriaConfigs(String[] paths) {
    var configs = new ArrayList<CriteriaSearchAttributeConfig>();
    for (var path : paths) {
      configs.add(criteriaConfigValidator.validate(
          loadConfig(SEARCH_INFO_PATH_TEMPLATE.formatted(path),
              CriteriaSearchAttributeConfig.class)));
    }
    return configs;
  }

  public <T> T loadConfig(String path, Class<T> configClass) {
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
      errorHandler.reportError(errorMessage);
      throw new ProcessingException(errorMessage);

    } catch (IOException ex) {
      var errorMessage = "Error loading resource: %s".formatted(path);
      errorHandler.reportError(errorMessage);
      throw new ProcessingException(errorMessage, ex);
    }
  }

}
