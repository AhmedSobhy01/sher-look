package com.sherlook.search.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sherlook.search.indexer.Tokenizer;
import com.sherlook.search.utils.ConsoleColors;

@Component
public class QueryProcessor {

  @Autowired
  private Tokenizer tokenizer;

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
    if (query == null || query.trim().isEmpty()) {
      ConsoleColors.printWarning("QueryProcessor");
      System.out.println("Empty query received");
      return;
    }

    query = query.trim();

    ConsoleColors.printInfo("QueryProcessor");
    System.out.println(
        "Processing query: " + ConsoleColors.BOLD_CYAN + query + ConsoleColors.RESET);

    if (query.equals(lastQuery)) {
      ConsoleColors.printInfo("QueryProcessor");
      System.out.println("Using cached results for query: " + query);
      return;
    } else {
      lastQuery = query;
    }

    if (query.matches("\".*\"")) {
      ConsoleColors.printInfo("QueryProcessor");
      System.out.println(
          "Detected "
              + ConsoleColors.BOLD_YELLOW
              + "phrase search"
              + ConsoleColors.RESET
              + " query");
      parsePhrases(query);
    } else {
      ConsoleColors.printInfo("QueryProcessor");
      System.out.println(
          "Detected "
              + ConsoleColors.BOLD_GREEN
              + "keyword search"
              + ConsoleColors.RESET
              + " query");
      parseTokens(query);
    }

    if (isPhraseMatching) {
      ConsoleColors.printInfo("QueryProcessor");
      System.out.print("Phrases: ");
      for (int i = 0; i < phrases.length && phrases[i] != null; i++) {
        System.out.print(ConsoleColors.BOLD_CYAN + phrases[i] + ConsoleColors.RESET);
        if (i < phrases.length - 1 && phrases[i + 1] != null) {
          String op = "?";
          switch (operators[i]) {
            case 1:
              op = "AND";
              break;
            case 2:
              op = "OR";
              break;
            case 3:
              op = "NOT";
              break;
          }
          System.out.print(" " + ConsoleColors.BOLD_PURPLE + op + ConsoleColors.RESET + " ");
        }
      }
      System.out.println();
    } else {
      ConsoleColors.printInfo("QueryProcessor");
      System.out.println(
          "Tokens: " + ConsoleColors.BOLD_CYAN + String.join(", ", tokens) + ConsoleColors.RESET);
      ConsoleColors.printInfo("QueryProcessor");
      System.out.println(
          "Stems: " + ConsoleColors.BOLD_GREEN + String.join(", ", stems) + ConsoleColors.RESET);
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
      phrases[i++] = phraseMatch.group().replaceAll("\"", "").toLowerCase().trim();
    }

    // Check if there is more than one phrase and fill the operators
    if (i > 1) {
      i = 0;
      while (operatorMatch.find()) {
        String operator = operatorMatch.group().trim();
        operators[i++] = operator.contains("AND") ? 1 : operator.contains("OR") ? 2 : 3;
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
