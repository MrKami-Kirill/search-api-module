package ru.tecius.telemed.dto.request;

import java.util.LinkedList;

public record SearchDataDto(String attribute,
                            LinkedList<String> value,
                            Operator operator) {

}
