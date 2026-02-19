package ru.tecius.telemed.configuration.nativ;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record NativeSearchAttributeConfig(
    @NotNull(message = "Поле attributes не может быть null")
    @NotEmpty(message = "Поле attributes не может быть пустым")
    @Valid
    Set<NativeSearchAttribute> attributes
) {

}
