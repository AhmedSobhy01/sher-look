package com.sherlook.search.query;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryProcessor {

  boolean isPhraseMatching;
  String[] phrases;
  int[] operators;
  List<String> tokens;

  public QueryProcessor() {
    isPhraseMatching = false;
    phrases = new String[3];
    operators = new int[2]; // Operators: (0) None, (1) AND, (2) OR, (3) NOT
    tokens = new ArrayList<>();
  }

  /**
   * Processes a user search query and returns the list of words in the query
   *
   * @param query the search query
   * @return a list of word IDs
   */
  public List<String> processQuery(String query) {
    System.out.println("Processing query: " + query);
    if (query == null || query.trim().isEmpty()) {
      return List.of();
    }

    if (query.contains("\"")) {
      isPhraseMatching = true;
      parsePhrases(query);
    } else {
      isPhraseMatching = false;

    }

    return tokens;
  }

  /**
   * Parses the phrases in the query and fills the phrases and operators
   * arrays
   *
   * @param query the search query
   */
  private void parsePhrases(String query) {
    Matcher phraseMatch = Pattern.compile("\"[^\"]+\"").matcher(query);
    Matcher operatorMatch = Pattern.compile("\"\\s*(AND|OR|NOT)\\s*\"").matcher(query);
    operators[0] = operators[1] = 0;
    phrases[0] = phrases[1] = phrases[2] = null;

    // Find all phrases in the query
    int i = 0;
    while (phraseMatch.find() && i < phrases.length) {
      phrases[i++] = phraseMatch.group().replaceAll("\"", "").trim();
    }

    // Check if there is more than one phrase and fill the operators
    if (i >= 2) {
      int j = 0;
      while (operatorMatch.find()) {
        String operator = operatorMatch.group().replaceAll("^\"|\"$", "").trim();
        operators[j++] = operator.equals("AND") ? 1 : operator.equals("OR") ? 2 : 3;
      }
    }
  }
}
