package com.sherlook.search.indexer;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Tokenizer {

  private final Stemmer stemmer;
  private final StopWordsFilter stopWordsFilter;

  @Autowired
  public Tokenizer(Stemmer stemmer, StopWordsFilter stopWordsFilter) {
    this.stemmer = stemmer;
    this.stopWordsFilter = stopWordsFilter;
  }

  public Tokenizer() {
    this.stemmer = new Stemmer();
    this.stopWordsFilter = new StopWordsFilter();
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
        if (!stopWordsFilter.isStopWord(word)) {
          tokens.add(word);

          if (stems != null) stems.add(stemmer.stem(word));

          positions.add(pos++);
          sections.add(currentSection);
        }
      }
    }

    return pos;
  }

  public void tokenizeQuery(String query, List<String> tokens, List<String> stems) {
    String[] words = query.toLowerCase().split("\\W+");
    for (String word : words) {
      if (!(word.isEmpty() || stopWordsFilter.isStopWord(word))) {
        tokens.add(word);
        stems.add(stemmer.stem(word));
      }
    }
  }
}
