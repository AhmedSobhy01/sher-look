package com.sherlook.search.ranker;

public class RankedDocument {
  private final int documentId;
  private final String url;
  private final String title;
  private String snippet;
  private final String description;
  private final double tfIdf;
  private double finalScore = 0.0;

  public RankedDocument(
      int documentId, String url, String title, double tfIdf, String description) {
    this.documentId = documentId;
    this.url = url;
    this.title = title;
    this.tfIdf = tfIdf;
    this.description = description;
  }

  public int getDocId() {
    return documentId;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
  }

  public double getTfIdf() {
    return tfIdf;
  }

  public String getSnippet() {
    return snippet;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }

  public String getDescription() {
    return description;
  }

  public double getFinalScore() {
    return finalScore;
  }

  public void setFinalScore(double finalScore) {
    this.finalScore = finalScore;
  }
}
