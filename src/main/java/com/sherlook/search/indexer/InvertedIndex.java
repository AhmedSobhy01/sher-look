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
    this.index = new HashMap<>();
  }

  public List<DocumentWord> getPostings(String word) {
    // Load from database if not cached
    if (!index.containsKey(word)) {
      List<DocumentWord> postings = databaseHelper.getPostingsForWord(word);
      index.put(word, postings);
    }
    return index.getOrDefault(word, new ArrayList<>());
  }
/*
  public Map<Integer, List<DocumentWord>> getDocTerms(List<String> queryTerms) {
    Map<Integer, List<DocumentWord>> docTerms = new HashMap<>();

    for (String term : queryTerms) {
      List<DocumentWord> postings = getPostings(term);

      for (DocumentWord posting : postings) {
        int docId = posting.getDocumentId();
        docTerms.computeIfAbsent(docId, k -> new ArrayList<>()).add(posting);
      }
    }

    return docTerms;
  }*/
}
