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
import org.yaml.snakeyaml.Yaml;
import ru.tecius.telemed.configuration.MultipleSearchAttributeConfig;
import ru.tecius.telemed.configuration.SimpleSearchAttributeConfig;
import ru.tecius.telemed.processor.error.ErrorHandler;
import ru.tecius.telemed.processor.exception.ProcessingException;

public class ConfigLoader {

  private final ProcessingEnvironment processingEnv;
  private final ObjectMapper objectMapper;
  private final ErrorHandler errorHandler;

  public ConfigLoader(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.objectMapper = new ObjectMapper();
    this.errorHandler = new ErrorHandler(processingEnv.getMessager());
  }

  public List<SimpleSearchAttributeConfig> loadSimpleConfigs(String[] paths) {
    var configs = new ArrayList<SimpleSearchAttributeConfig>();
    for (var path : paths) {
      configs.add(loadConfig(SEARCH_INFO_PATH_TEMPLATE.formatted(path),
          SimpleSearchAttributeConfig.class));
    }
    return configs;
  }

  public List<MultipleSearchAttributeConfig> loadMultipleConfigs(String[] paths) {
    var configs = new ArrayList<MultipleSearchAttributeConfig>();
    for (var path : paths) {
      configs.add(loadConfig(SEARCH_INFO_PATH_TEMPLATE.formatted(path),
          MultipleSearchAttributeConfig.class));
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
