package ru.tecius.telemed.common.criteria;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum HintName {

  FETCH_GRAPH(HINT_SPEC_FETCH_GRAPH),
  LOAD_GRAPH(HINT_SPEC_LOAD_GRAPH);

  private final String value;
}
