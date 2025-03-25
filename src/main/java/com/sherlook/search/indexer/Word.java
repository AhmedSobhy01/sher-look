package com.sherlook.search.indexer;

public class Word {
  private final int id;
  private final String word;

  public Word(int id, String word) {
    this.id = id;
    this.word = word;
  }

  public int getId() {
    return id;
  }

  public String getWord() {
    return word;
  }
}
