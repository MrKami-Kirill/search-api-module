package ru.tecius.telemed.configuration.nativ;

import static java.util.Objects.isNull;
import static java.util.stream.Stream.ofNullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

public record NativeSearchAttributeConfig(
    @NotNull(message = "Поле attributes не может быть null")
    @NotEmpty(message = "Поле attributes не может быть пустым")
    @Valid
    Set<NativeSearchAttribute> attributes
) {

    @AssertTrue(message = "Поле attributes.json.key должно быть уникальным для всех атрибутов")
    public boolean isUniqueJsonKeys() {
        if (isNull(attributes())) {
            return true;
        }

        var keys = attributes().stream()
            .flatMap(attr -> ofNullable(attr.json()))
            .map(JsonData::key)
            .filter(Objects::nonNull)
            .toList();

        return Objects.equals(keys.size(), Set.copyOf(keys).size());
    }

}
