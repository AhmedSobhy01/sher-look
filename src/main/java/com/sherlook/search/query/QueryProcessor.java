package com.sherlook.search.query;

import com.sherlook.search.indexer.Stemmer;
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
  List<String> stems;
  String lastQuery;

  public QueryProcessor() {
    lastQuery = null;
    isPhraseMatching = false;
    phrases = new String[3];
    operators = new int[2]; // Operators: (0) None, (1) AND, (2) OR, (3) NOT
    tokens = new ArrayList<>();
    stems = new ArrayList<>();
  }

  /**
   * Processes a user search query and determines if it contains phrases or individual tokens.
   * Updates the `isPhraseMatching`, `phrases`, and `operators` fields accordingly.
   *
   * @param query the search query to process
   */
  public void processQuery(String query) {
    System.out.println("Processing query: " + query);
    if (query == null || query.trim().isEmpty()) {
      return;
    }
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


  /**
   * Parses the tokens from the query and applies additional processing logic.
   * Populates the `tokens` and `stems` lists.
   *
   * @param query the search query to parse tokens from
   */
  private void parseTokens(String query) {
    Stemmer stemmer = new Stemmer();
    tokens.clear();
    stems.clear();
    isPhraseMatching = false;
    String[] words = query.toLowerCase().split("\\W+");
    for (String word : words) {
      if (!word.isEmpty()) {
        tokens.add(word);
        stems.add(stemmer.stem(word));
      }
    }
  }

  /**
   * Parses the phrases and logical operators (AND, OR, NOT) in the query.
   * Populates the `phrases` and `operators` arrays based on the query content.
   *
   * @param query the search query containing phrases and operators
   */
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

  /**
   * Retrieves the list of tokens processed from the query.
   *
   * @return a list of tokens
   */
  public List<String> getTokens() {
    return tokens;
  }

  /**
   * Retrieves the list of stemmed tokens processed from the query.
   *
   * @return a list of stemmed tokens
   */
  public List<String> getStems() {
    return stems;
  }

  /**
   * Retrieves the array of phrases extracted from the query.
   *
   * @return an array of phrases
   */
  public String[] getPhrases() {
    return phrases;
  }

  /**
   * Retrieves the array of operators extracted from the query.
   *
   * @return an array of operators
   */
  public int[] getOperators() {
    return operators;
  }

  /**
   * Checks if the query contains phrase matching.
   *
   * @return true if the query contains phrases, false otherwise
   */
  public boolean isPhraseMatching() {
    return isPhraseMatching;
  }
}
