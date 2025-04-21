package com.sherlook.search.indexer;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Tokenizer {
  public List<String> tokenize(String text) {
    if (text == null || text.isEmpty()) return new ArrayList<>();

    List<String> tokens = new ArrayList<>();
    String[] rawTokens = text.toLowerCase().split("\\W+");

    for (String token : rawTokens) if (!token.isEmpty()) tokens.add(token);

    return tokens;
  }

  public int tokenizeWithPositions(
      String text,
      int startPosition,
      List<String> tokens,
      List<Integer> positions,
      List<Section> sections,
      Section section) {

    int pos = startPosition;
    List<String> newTokens = tokenize(text);

    for (String token : newTokens) {
      tokens.add(token);
      positions.add(pos);
      sections.add(section);
      pos++;
    }

    return pos;
  }
}
