package com.sherlook.search.ranker;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.utils.DatabaseHelper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Ranker {

  private QueryProcessor queryProcessor;
  private DatabaseHelper databaseHelper;

  @Autowired
  public Ranker(QueryProcessor queryProcessor, DatabaseHelper databaseHelper) {
    this.queryProcessor = queryProcessor;
    this.databaseHelper = databaseHelper;
  }

  /**
   * Gets the list of relevant documents for a given query with their tf-idf scores sorted
   * descendingly.
   *
   * @param query
   */
  public List<RankedDocument> getRelevance(String query) {
    List<String> words = queryProcessor.processQuery(query);
    return databaseHelper.getDocumentRelevance(words);
  }

  public void rankDocuments() {
    System.out.println("Ranking documents...");
  }
}
