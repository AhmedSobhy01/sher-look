package com.sherlook.search.query;

import com.sherlook.search.indexer.Tokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryProcessor {

  @Autowired private Tokenizer tokenizer;

  boolean isPhraseMatching;
  String[] phrases;
  int[] operators;
  List<String> tokens;
  List<String> stems;
  String lastQuery;

  public QueryProcessor() {
    lastQuery = null;
    isPhraseMatching = false;
    phrases = new String[3];
    operators = new int[2]; // Two operators: (0) None, (1) AND, (2) OR, (3) NOT
    tokens = new ArrayList<>();
    stems = new ArrayList<>();
  }

  // Processes a user search query.
  public void processQuery(String query) {
    System.out.println("Processing query: " + query);
    if (query == null || query.trim().isEmpty()) {
      return;
    }

    query = query.trim().toLowerCase();

    if (query.equals(lastQuery)) {
      return;
    } else {
      lastQuery = query;
    }

    if (query.matches("\".*\"")) {
      parsePhrases(query);
    } else {
      parseTokens(query);
    }
  }

  // Parses the tokens from the query and applies additional processing logic.
  private void parseTokens(String query) {
    tokens.clear();
    stems.clear();
    isPhraseMatching = false;
    tokenizer.tokenizeQuery(query, tokens, stems);
  }

  // Parses the phrases and logical operators (AND, OR, NOT) in the query.
  private void parsePhrases(String query) {
    Matcher phraseMatch = Pattern.compile("\"[^\"]+\"").matcher(query);
    Matcher operatorMatch = Pattern.compile("\"\\s*(AND|OR|NOT)\\s*\"").matcher(query);
    operators[0] = operators[1] = 0;
    phrases[0] = phrases[1] = phrases[2] = null;
    isPhraseMatching = true;
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

  // Retrieves the list of tokens processed from the query.
  public List<String> getTokens() {
    return tokens;
  }

  // Retrieves the list of stemmed tokens processed from the query.
  public List<String> getStems() {
    return stems;
  }

  // Retrieves the array of phrases extracted from the query.
  public String[] getPhrases() {
    return phrases;
  }

  // Retrieves the array of operators extracted from the query.
  public int[] getOperators() {
    return operators;
  }

  // Checks if the query contains phrase matching.
  public boolean isPhraseMatching() {
    return isPhraseMatching;
  }
}
