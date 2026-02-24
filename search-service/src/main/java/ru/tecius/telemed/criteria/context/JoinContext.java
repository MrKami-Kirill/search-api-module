package ru.tecius.telemed.criteria.context;

import jakarta.persistence.criteria.Join;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JoinContext {

  private final Map<String, Join<?, ?>> joins = new LinkedHashMap<>();
  private final Set<String> processedPaths = new LinkedHashSet<>();
  private final Set<String> collectionJoins = new LinkedHashSet<>();
  private final Set<String> fetchedPaths = new LinkedHashSet<>();

  public Join<?, ?> getJoin(String path) {
    return joins.get(path);
  }

  public void addJoin(String path, Join<?, ?> join) {
    joins.put(path, join);
    processedPaths.add(path);
  }

  public boolean hasJoin(String path) {
    return processedPaths.contains(path);
  }

  public void markAsCollectionJoin(String path) {
    collectionJoins.add(path);
  }

  public boolean hasCollectionJoins() {
    return !collectionJoins.isEmpty();
  }

  public void markAsFetched(String path) {
    fetchedPaths.add(path);
  }

  public boolean isFetched(String path) {
    return fetchedPaths.contains(path);
  }

  public Set<String> getFetchedPaths() {
    return new LinkedHashSet<>(fetchedPaths);
  }


}
