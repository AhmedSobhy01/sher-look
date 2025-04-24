package com.sherlook.search.ranker;

public class Link {
  private final int sourceId;
  private final int targetId;

  public Link(int sourceId, int targetId) {
    this.sourceId = sourceId;
    this.targetId = targetId;
  }

  public int getSourceId() {
    return sourceId;
  }

  public int getTargetId() {
    return targetId;
  }
}
