package com.sherlook.search.ranker;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
  Map<Integer, Integer> outgoingLinkCount;
  Map<Integer, List<Integer>> incomingLinks;
  Set<Integer> danglingNodes;

  public Graph(
      Map<Integer, Integer> outgoingLinkCount,
      Map<Integer, List<Integer>> incomingLinks,
      Set<Integer> danglingNodes) {
    this.outgoingLinkCount = outgoingLinkCount;
    this.incomingLinks = incomingLinks;
    this.danglingNodes = danglingNodes;
  }
}
