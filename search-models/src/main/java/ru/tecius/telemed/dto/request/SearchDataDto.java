package ru.tecius.telemed.dto.request;

import java.util.List;

public record SearchDataDto(String attribute,
                            List<String> value,
                            Operator operator) {

}
