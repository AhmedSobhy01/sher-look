package com.sherlook.search.indexer;

public class DocumentWord {
  private final Document document;
  private final Word word;
  private final int position;
  private final Section section;

  public DocumentWord(Document document, Word word, int position, Section section) {
    this.document = document;
    this.word = word;
    this.position = position;
    this.section = section;
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

  public Section getSection() {
    return section;
  }
}
