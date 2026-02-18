package ru.tecius.telemed.enumeration;

public enum JoinTypeEnum {

  INNER_JOIN("INNER JOIN"),
  LEFT_JOIN("LEFT JOIN"),
  RIGHT_JOIN("RIGHT JOIN"),
  FULL_JOIN("FULL JOIN"),
  CROSS_JOIN("CROSS JOIN"),
  LATERAL_JOIN("LATERAL JOIN");

  private final String value;

  JoinTypeEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}