package com.sherlook.search.crawler;

import java.util.Objects;

public class UrlDepthPair {
  private String url;
  private int depth;

  public UrlDepthPair(String url, int depth) {
    this.url = url;
    this.depth = depth;
  }

  public String getUrl() {
    return url;
  }

  public int getDepth() {
    return depth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UrlDepthPair other = (UrlDepthPair) o;
    return depth == other.depth && url.equals(other.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, depth);
  }

  @Override
  public String toString() {
    return "UrlDepthPair{" + "url='" + url + '\'' + ", depth=" + depth + '}';
  }
}
