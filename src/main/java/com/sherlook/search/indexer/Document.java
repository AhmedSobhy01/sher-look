package com.sherlook.search.indexer;

import java.sql.Timestamp;

public class Document {
  private final int id;
  private final String url;
  private final String title;
  private final String description;
  private final String filePath;
  private final Timestamp crawlTime;

  public Document(
      int id, String url, String title, String description, String filePath, Timestamp crawlTime) {
    this.id = id;
    this.url = url;
    this.title = title;
    this.description = description;
    this.filePath = filePath;
    this.crawlTime = crawlTime;
  }

  public int getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getFilePath() {
    return filePath;
  }

  public Timestamp getCrawlTime() {
    return crawlTime;
  }
}
