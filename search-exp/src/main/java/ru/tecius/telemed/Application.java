package ru.tecius.telemed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.tecius.telemed.service.MenuItemService;

/**
 * Главный класс приложения.
 * Точка входа в Spring Boot приложение.
 */
@SpringBootApplication
public class Application {

  /**
   * Главный метод приложения. Запускает Spring Boot приложение с указанными аргументами.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    var context = SpringApplication.run(Application.class, args);
    var menuItemService = context.getBean(MenuItemService.class);
    var result = menuItemService.search();
    System.out.println(result);
  }

}
