package ru.tecius.telemed.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum JoinTypeEnum {

  INNER_JOIN("INNER JOIN"),
  LEFT_JOIN("LEFT JOIN"),
  RIGHT_JOIN("RIGHT JOIN"),
  FULL_JOIN("FULL JOIN"),
  CROSS_JOIN("CROSS JOIN"),
  LATERAL_JOIN("LATERAL JOIN");

  private final String value;
}