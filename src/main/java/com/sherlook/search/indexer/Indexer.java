package com.sherlook.search.indexer;

import com.sherlook.search.utils.ConsoleColors;
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

    ConsoleColors.printInfo("Indexer");
    System.out.println("Indexing document: " + filePath);

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
      ConsoleColors.printInfo("Indexer");
      System.out.println("Updated metadata for document ID " + document.getId());

      int currentPosition = 0;

      // Index title
      if (!extractedTitle.isEmpty()) {
        String[] titleTokens = extractedTitle.toLowerCase().split("\\W+");
        for (String token : titleTokens) {
          token = token.trim();
          if (token.isEmpty()) continue;
          databaseHelper.insertDocumentWord(
              document.getId(), token, currentPosition, Section.TITLE);
          currentPosition++;
        }
      }

      // Index rest of page content
      for (Element element : htmlDoc.select("* :not(script):not(style)")) {
        if (element.tagName().equals("title") || element.tagName().equals("meta")) continue;

        if (element.ownText().isEmpty()) continue;

        Section section = element.tagName().matches("h[1-6]") ? Section.HEADER : Section.BODY;

        String[] tokens = element.text().toLowerCase().split("\\W+");
        for (String token : tokens) {
          token = token.trim();

          if (token.isEmpty()) continue;

          databaseHelper.insertDocumentWord(document.getId(), token, currentPosition, section);
          currentPosition++;
        }
      }

      ConsoleColors.printInfo("Indexer");
      System.out.println("Indexed words for document ID " + document.getId());

      // Update index_time
      databaseHelper.updateIndexTime(document.getId());
      long endTime = System.currentTimeMillis();
      ConsoleColors.printSuccess("Indexer");
      System.out.println("Updated index_time for document ID " + document.getId());

      ConsoleColors.printSuccess("Indexer");
      System.out.println("Indexing completed in " + (endTime - startTime) + " ms");

    } catch (IOException e) {
      ConsoleColors.printError("Indexer");
      System.err.println("Error parsing file at " + filePath + " using jsoup: " + e.getMessage());
    }
  }

  public Queue<Document> loadUnindexedDocuments() throws SQLException {
    ConsoleColors.printInfo("Indexer");
    System.out.println("Loading unindexed documents from database");
    Queue<Document> queue = new LinkedList<>();
    databaseHelper.getUnindexedDocuments().forEach(queue::add);
    return queue;
  }

  public void index() {
    ConsoleColors.printInfo("Indexer");
    System.out.println("Starting indexing process");

    try {
      Queue<Document> unindexedDocs = loadUnindexedDocuments();
      ConsoleColors.printInfo("Indexer");
      System.out.println("Found " + unindexedDocs.size() + " unindexed documents");

      while (!unindexedDocs.isEmpty()) {
        Document doc = unindexedDocs.poll();
        indexDocument(doc);
      }
      ConsoleColors.printSuccess("Indexer");
      System.out.println("Indexing complete");
    } catch (SQLException e) {
      ConsoleColors.printError("Indexer");
      System.err.println("Error loading unindexed documents: " + e.getMessage());
    }
  }
}
