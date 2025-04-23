package com.sherlook.search.ranker;

public class RankedDocument {
  private int documentId;
  private String url;
  private String title;
  private double tfIdf;

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

  public double getScore() {
    return tfIdf;
  }
}
