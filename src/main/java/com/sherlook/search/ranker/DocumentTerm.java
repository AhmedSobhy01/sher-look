package com.sherlook.search.ranker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentTerm {
  private final String word;
  private final int documentId;
  private final String url;
  private final String title;
  private final int document_size;
  private final Map<String, List<Integer>> positionsBySection;

  public DocumentTerm(
      String word,
      int documentId,
      String url,
      String title,
      int document_size,
      Map<String, List<Integer>> positionsBySection) {
    this.word = word;
    this.documentId = documentId;
    this.url = url;
    this.title = title;
    this.document_size = document_size;
    this.positionsBySection = positionsBySection;
  }

  public String getWord() {
    return word;
  }

  public int getDocumentId() {
    return documentId;
  }

  public int getDocumentSize() {
    return document_size;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
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
    private final int document_size;
    private final Map<String, List<Integer>> positionsBySection;

    public DocumentTermBuilder(
        String word, int documentId, String url, String title, int document_size) {
      this.word = word;
      this.documentId = documentId;
      this.url = url;
      this.title = title;
      this.document_size = document_size;
      this.positionsBySection = new HashMap<String, List<Integer>>();
    }

    public void addPositions(String section, List<Integer> positions) {
      positionsBySection.put(section, positions);
    }

    public DocumentTerm build() {
      return new DocumentTerm(word, documentId, url, title, document_size, positionsBySection);
    }
  }
}
