package ru.tecius.telemed.configuration.criteria;

import jakarta.validation.constraints.NotBlank;

public record JsonData(
    @NotBlank(message = "Поле attributes.json.key не может быть пустым")
    String key
) {

}
