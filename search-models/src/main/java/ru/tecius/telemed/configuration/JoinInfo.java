package ru.tecius.telemed.configuration;

import ru.tecius.telemed.enumeration.JoinTypeEnum;

public record JoinInfo(Integer order,
                       String referenceJoinColumn,
                       String joinTable,
                       String joinTableAlias,
                       String joinColumn,
                       JoinTypeEnum joinType) {

}
