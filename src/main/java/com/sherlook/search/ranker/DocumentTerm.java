package com.sherlook.search.ranker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentTerm {
  private final String word;
  private final int documentId;
  private final String url;
  private final String title;
  private final int documentSize;
  private final String description;
  private final Map<String, List<Integer>> positionsBySection;

  public DocumentTerm(
      String word,
      int documentId,
      String url,
      String title,
      int documentSize,
      String description,
      Map<String, List<Integer>> positionsBySection) {
    this.word = word;
    this.documentId = documentId;
    this.url = url;
    this.title = title;
    this.documentSize = documentSize;
    this.description = description;
    this.positionsBySection = positionsBySection;
  }

  public String getWord() {
    return word;
  }

  public int getDocumentId() {
    return documentId;
  }

  public int getDocumentSize() {
    return documentSize;
  }

    public String getDescription() {
        return description;
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
    private final int documentSize;
    private final String description;
    private final Map<String, List<Integer>> positionsBySection;

    public DocumentTermBuilder(
        String word, int documentId, String url, String title, int documentSize, String description) {
      this.word = word;
      this.documentId = documentId;
      this.url = url;
      this.title = title;
      this.documentSize = documentSize;
        this.description = description;
      this.positionsBySection = new HashMap<String, List<Integer>>();
    }

    public void addPositions(String section, List<Integer> positions) {
      positionsBySection.put(section, positions);
    }

    public DocumentTerm build() {
      return new DocumentTerm(word, documentId, url, title, documentSize, description, positionsBySection);
    }
  }
}
