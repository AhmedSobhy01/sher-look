package com.sherlook.search.indexer;

public class DocumentWord {
  private final Document document;
  private final Word word;
  private final int position;

  public DocumentWord(Document document, Word word, int position) {
    this.document = document;
    this.word = word;
    this.position = position;
  }

  public Document getDocument() {
    return document;
  }

  public Word getWord() {
    return word;
  }

  public int getPosition() {
    return position;
  }
}
