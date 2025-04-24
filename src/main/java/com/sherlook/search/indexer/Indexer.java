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

      if (!title.isEmpty())
        pos =
            tokenizer.tokenizeWithPositions(
                title, pos, words, stems, positions, sections, Section.TITLE);

      for (Element el : htmlDoc.select("* :not(script):not(style)")) {
        if (el.tagName().equals("title") || el.tagName().equals("meta") || el.ownText().isEmpty())
          continue;

        Section sec = el.tagName().matches("h[1-6]") ? Section.HEADER : Section.BODY;
        pos =
            tokenizer.tokenizeWithPositions(el.text(), pos, words, stems, positions, sections, sec);
      }

      // Insert words into the database
      if (!words.isEmpty()) {
        databaseHelper.batchInsertDocumentWords(
            document.getId(), words, stems, positions, sections);

        ConsoleColors.printInfo("Indexer");
        System.out.println(
            "  Indexed " + words.size() + " words for document ID=" + document.getId());
      }

      // Update index time
      databaseHelper.updateIndexTime(document.getId());

      long elapsed = System.currentTimeMillis() - startTime;
      ConsoleColors.printSuccess("Indexer");
      System.out.println("Indexing completed in " + elapsed + " ms");

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
      System.out.println("Found " + docs.size() + " documents to index");

      while (!docs.isEmpty()) {
        indexDocument(docs.poll());
      }
      // batch update document_sizes after all documents are indexed
      databaseHelper.populateDocumentSizes();
      // precompute idf values
      databaseHelper.populateIDF();

      ConsoleColors.printSuccess("Indexer");
      System.out.println("All done!");
    } catch (SQLException e) {
      ConsoleColors.printError("Indexer");
      System.err.println("Could not load unindexed docs: " + e.getMessage());
    }
  }
}
