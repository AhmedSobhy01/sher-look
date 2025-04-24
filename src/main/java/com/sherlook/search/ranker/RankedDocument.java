package com.sherlook.search.ranker;

public class RankedDocument {
  private final int documentId;
  private final String url;
  private final String title;
  private final double tfIdf;
  private double finalScore = 0.0;

  public RankedDocument(int documentId, String url, String title, double tfIdf) {
    this.documentId = documentId;
    this.url = url;
    this.title = title;
    this.tfIdf = tfIdf;

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

  public double getFinalScore() {
    return finalScore;
  }

  public void setFinalScore(double finalScore) {
    this.finalScore = finalScore;
  }
}
