package com.sherlook.search.query;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryProcessor {

  @Autowired
  public QueryProcessor() {}

  /**
   * Processes a user search query and returns the list of words in the query
   *
   * @param query
   * @return a list of word IDs
   */
  public List<String> processQuery(String query) {
    System.out.println("Processing query: " + query);
    if (query == null || query.trim().isEmpty()) return List.of();
    String[] tokens = query.toLowerCase().split("\\W+");
    List<String> words = new ArrayList<>();
    for (String token : tokens) {
      if (token.length() > 2) {
        words.add(token);
      }
    }
    return words;
  }
}
