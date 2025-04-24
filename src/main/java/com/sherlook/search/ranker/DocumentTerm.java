package com.sherlook.search.ranker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentTerm {
  private final String word;
  private final int documentId;
  private final String url;
  private final String title;
  private final int wordCountInDocument; // for tf
  private final Map<String, List<Integer>> positionsBySection;

  public DocumentTerm(
      String word,
      int documentId,
      String url,
      String title,
      int wordCountInDocument,
      Map<String, List<Integer>> positionsBySection) {
    this.word = word;
    this.documentId = documentId;
    this.url = url;
    this.title = title;
    this.wordCountInDocument = wordCountInDocument;
    this.positionsBySection = positionsBySection;
  }

  public String getWord() {
    return word;
  }

  public int getDocumentId() {
    return documentId;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
  }

  public int getWordCountInDocument() {
    return wordCountInDocument;
  }

  public Map<String, List<Integer>> getPositionsBySection() {
    return positionsBySection;
  }

  // A clean way to build the positionsBySection map
  public static class DocumentTermBuilder {
    private final String word;
    private final int documentId;
    private final String url;
    private final String title;
    private int wordCountInDocument;
    private final Map<String, List<Integer>> positionsBySection;

    public DocumentTermBuilder(String word, int documentId, String url, String title) {
      this.word = word;
      this.documentId = documentId;
      this.url = url;
      this.title = title;
      this.wordCountInDocument = 0;
      this.positionsBySection = new HashMap<String, List<Integer>>();
    }

    public void setWordCountInDocument(int wordCountInDocument) {
      this.wordCountInDocument = wordCountInDocument;
    }

    public int getWordCountInDocument() {
      return wordCountInDocument;
    }

    public void addPositions(String section, List<Integer> positions) {
      positionsBySection.put(section, positions);
    }

    public DocumentTerm build() {
      return new DocumentTerm(
          word, documentId, url, title, wordCountInDocument, positionsBySection);
    }
  }
}
