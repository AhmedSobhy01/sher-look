package com.sherlook.search.indexer;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Tokenizer {

  private final Stemmer stemmer;

  @Autowired
  public Tokenizer(Stemmer stemmer) {
    this.stemmer = stemmer;
  }

  public int tokenizeWithPositions(
      String text,
      int startPos,
      List<String> tokens,
      List<String> stems,
      List<Integer> positions,
      List<Section> sections,
      Section currentSection) {

    String[] words = text.toLowerCase().split("\\W+");
    int pos = startPos;

    for (String word : words) {
      if (!word.isEmpty()) {
        tokens.add(word);

        if (stems != null) stems.add(stemmer.stem(word));

        positions.add(pos++);
        sections.add(currentSection);
      }
    }

    return pos;
  }
}
