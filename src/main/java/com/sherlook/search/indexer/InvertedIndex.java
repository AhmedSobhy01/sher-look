package com.sherlook.search.indexer;

import com.sherlook.search.utils.DatabaseHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvertedIndex {

  private final Map<String, List<DocumentWord>> index;
  private final DatabaseHelper databaseHelper;

  @Autowired
  public InvertedIndex(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;

    index = new HashMap<>();
    loadIndexFromDatabase();
  }

  private void loadIndexFromDatabase() {
    List<DocumentWord> documentWords = databaseHelper.getDocumentWords();
    for (DocumentWord documentWord : documentWords) {
      String word = documentWord.getWord().getWord();

      if (!index.containsKey(word)) index.put(word, new ArrayList<>());
    }
  }

  public List<DocumentWord> getPostings(String word) {
    return index.getOrDefault(word, new ArrayList<>());
  }
}
