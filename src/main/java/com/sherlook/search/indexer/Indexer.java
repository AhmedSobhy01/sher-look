package com.sherlook.search.indexer;

import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Component
public class Indexer {
  private final DatabaseHelper databaseHelper;
  private final PlatformTransactionManager txManager;
  private final Tokenizer tokenizer;

  private static final int BATCH_SIZE = 10000;

  @Autowired
  public Indexer(
      DatabaseHelper databaseHelper, PlatformTransactionManager txManager, Tokenizer tokenizer) {
    this.databaseHelper = databaseHelper;
    this.txManager = txManager;
    this.tokenizer = tokenizer;
  }

  public void indexDocument(Document document) {
    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

    try {
      String filePath = document.getFilePath();
      long startTime = System.currentTimeMillis();

      ConsoleColors.printInfo("Indexer");
      System.out.println("Indexing document: " + filePath);

      // Parse the HTML file
      File input = new File(filePath);
      org.jsoup.nodes.Document htmlDoc = Jsoup.parse(input, "UTF-8");

      // Extract title and description
      String title = htmlDoc.title();
      if (title.isEmpty()) {
        Element firstHeader = htmlDoc.selectFirst("h1, h2, h3, h4, h5, h6");
        if (firstHeader != null) title = firstHeader.text();
      }

      String description = "";
      Element metaDesc = htmlDoc.selectFirst("meta[name=description]");
      if (metaDesc != null) {
        description = metaDesc.attr("content");
      } else {
        Element p = htmlDoc.selectFirst("p");
        if (p != null) description = p.text();
      }

      // Update the document metadata in the database
      databaseHelper.updateDocumentMetadata(document.getId(), title, description);
      ConsoleColors.printInfo("Indexer");
      System.out.println("  Updated metadata for document ID=" + document.getId());

      // Extract words from the document
      List<String> words = new ArrayList<>();
      List<String> stems = new ArrayList<>();
      List<Integer> positions = new ArrayList<>();
      List<Section> sections = new ArrayList<>();
      int pos = 0;
      int totalWordCount = 0;

      if (!title.isEmpty())
        pos =
            tokenizer.tokenizeWithPositions(
                title, pos, words, stems, positions, sections, Section.TITLE);

      StringBuilder ftsContent = new StringBuilder();
      StringBuilder fullContent = new StringBuilder();
      ftsContent.append(title).append(" ").append(description);
      for (Element el : htmlDoc.select("* :not(script):not(style)")) {
        if (el.tagName().equals("title") || el.tagName().equals("meta") || el.ownText().isEmpty())
          continue;

        Section sec = el.tagName().matches("h[1-6]") ? Section.HEADER : Section.BODY;
        pos =
            tokenizer.tokenizeWithPositions(el.text(), pos, words, stems, positions, sections, sec);
        fullContent.append(el.text()).append(" ");
        // Process in batches to avoid going out of memory
        if (words.size() >= BATCH_SIZE) {
          processBatch(document.getId(), words, stems, positions, sections);
          totalWordCount += words.size();

          words.clear();
          stems.clear();
          positions.clear();
          sections.clear();
        }
      }

      // Insert words into the database
      if (!words.isEmpty()) {
        processBatch(document.getId(), words, stems, positions, sections);
        totalWordCount += words.size();
      }

      // Update document's total word count
      if (totalWordCount > 0) {
        databaseHelper.updateDocumentSize(document.getId(), totalWordCount);

        ConsoleColors.printInfo("Indexer");
        System.out.println(
            "  Indexed " + totalWordCount + " words for document ID=" + document.getId());
        long elapsed = System.currentTimeMillis() - startTime;
        ConsoleColors.printSuccess("Indexer");
        System.out.println("Indexing completed in " + elapsed + " ms");

        ftsContent.append(fullContent.toString().trim());
        databaseHelper.updateFTSEntry(document.getId(), ftsContent.toString().trim());

        ConsoleColors.printInfo("Indexer");
        System.out.println(
            "  Updated FTS entry for document ID=" + document.getId() + " in the database");
      }

      // Update index time
      databaseHelper.updateIndexTime(document.getId());

      txManager.commit(status);
    } catch (Exception e) {
      txManager.rollback(status);
      ConsoleColors.printError("Indexer");
      System.err.println(
          "Failed to index document ID="
              + document.getId()
              + ", rolled back. Cause: "
              + e.getMessage());
    }
  }

  private void processBatch(
      int documentId,
      List<String> words,
      List<String> stems,
      List<Integer> positions,
      List<Section> sections) {
    try {
      if (!words.isEmpty()) {
        databaseHelper.batchInsertDocumentWords(documentId, words, stems, positions, sections);
        ConsoleColors.printInfo("Indexer");
        System.out.println("  Processed batch of " + words.size() + " words");
      }
    } catch (Exception e) {
      ConsoleColors.printError("Indexer");
      System.err.println("Error processing batch: " + e.getMessage());
      throw e;
    }
  }

  public Queue<Document> loadUnindexedDocuments() throws SQLException {
    ConsoleColors.printInfo("Indexer");
    System.out.println("Loading unindexed documents...");
    Queue<Document> q = new LinkedList<>();
    databaseHelper.getUnindexedDocuments().forEach(q::add);
    return q;
  }

  public void index() {
    ConsoleColors.printInfo("Indexer");
    System.out.println("Starting indexing run");

    try {
      Queue<Document> docs = loadUnindexedDocuments();
      ConsoleColors.printInfo("Indexer");
      System.out.println("Found " + docs.size() + " documents to index");

      long startTime = System.currentTimeMillis();
      while (!docs.isEmpty()) indexDocument(docs.poll());
      long elapsed = System.currentTimeMillis() - startTime;

      ConsoleColors.printInfo("Indexer");
      System.out.println(
          "Indexing documents completed in "
              + ((elapsed / 1000) / 60)
              + " minutes and "
              + ((elapsed / 1000) % 60)
              + " seconds");

      startTime = System.currentTimeMillis();
      databaseHelper.calculateIDF();
      elapsed = System.currentTimeMillis() - startTime;

      ConsoleColors.printSuccess("Indexer");
      System.out.println(
          "IDF calculation completed in "
              + ((elapsed / 1000) / 60)
              + " minutes and "
              + ((elapsed / 1000) % 60)
              + " seconds");

      ConsoleColors.printSuccess("Indexer");
      System.out.println("All done!");
    } catch (SQLException e) {
      ConsoleColors.printError("Indexer");
      System.err.println("Could not load unindexed docs: " + e.getMessage());
    }
  }
}
