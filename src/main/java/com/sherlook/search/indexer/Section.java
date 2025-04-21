package com.sherlook.search.indexer;

public enum Section {
  TITLE,
  HEADER,
  BODY;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  public static Section fromString(String section) {
    switch (section.toLowerCase()) {
      case "title":
        return TITLE;
      case "header":
        return HEADER;
      case "body":
        return BODY;
      default:
        throw new IllegalArgumentException("Unknown section: " + section);
    }
  }
}
