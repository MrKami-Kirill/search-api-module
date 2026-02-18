package ru.tecius.telemed.configuration;

import java.util.Set;

public record MultipleSearchAttribute(String jsonField,
                                      String dbField,
                                      Set<JoinInfo> joinInfo) {

}
