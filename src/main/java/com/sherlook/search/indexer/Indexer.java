package com.sherlook.search.indexer;

import com.sherlook.search.utils.DatabaseHelper;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Indexer {
  private final DatabaseHelper databaseHelper;

  @Autowired
  public Indexer(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  public void indexDocument(Document document) {
    String filePath = document.getFilePath();
    long startTime = System.currentTimeMillis();

    try {
      File input = new File(filePath);
      org.jsoup.nodes.Document htmlDoc = Jsoup.parse(input, "UTF-8");

      // Metadata
      String extractedTitle = htmlDoc.title();
      if (extractedTitle.isEmpty()) {
        Element firstHeader = htmlDoc.selectFirst("h1, h2, h3, h4, h5, h6");
        if (firstHeader != null) {
          extractedTitle = firstHeader.text();
        }
      }

      String extractedDescription = "";
      Element metaDesc = htmlDoc.selectFirst("meta[name=description]");
      if (metaDesc != null) {
        extractedDescription = metaDesc.attr("content");
      } else {
        Element firstParagraph = htmlDoc.selectFirst("p");
        if (firstParagraph != null) extractedDescription = firstParagraph.text();
      }

      databaseHelper.updateDocumentMetadata(document.getId(), extractedTitle, extractedDescription);

      // Tokenize the content
      String content = htmlDoc.text();
      String[] tokens = content.toLowerCase().split("\\W+");

      int validTokenCount = 0;
      for (String token : tokens) if (token != null && !token.trim().isEmpty()) validTokenCount++;

      System.out.println(
          "Indexing document ID " + document.getId() + " with " + validTokenCount + " tokens.");

      // Insert tokens into the database
      int position = 0;
      for (String token : tokens) {
        token = token.trim();
        if (token.isEmpty()) continue;

        databaseHelper.insertDocumentWord(document.getId(), token, position);
        position++;
      }

      // Update index_time
      databaseHelper.updateIndexTime(document.getId());
      long endTime = System.currentTimeMillis();
      System.out.println("Updated index_time for document ID " + document.getId());
      System.out.println("Indexing completed in " + (endTime - startTime) + " ms.");

    } catch (IOException e) {
      System.err.println("Error parsing file at " + filePath + " using jsoup: " + e.getMessage());
    }
  }

  public Queue<Document> loadUnindexedDocuments() throws SQLException {
    Queue<Document> queue = new LinkedList<>();
    databaseHelper.getUnindexedDocuments().forEach(queue::add);
    return queue;
  }

  public void index() {
    try {
      Queue<Document> unindexedDocs = loadUnindexedDocuments();
      System.out.println("Found " + unindexedDocs.size() + " unindexed documents.");

      while (!unindexedDocs.isEmpty()) {
        Document doc = unindexedDocs.poll();
        indexDocument(doc);
      }
      System.out.println("Indexing complete.");
    } catch (SQLException e) {
      System.err.println("Error loading unindexed documents: " + e.getMessage());
    }
  }
}
