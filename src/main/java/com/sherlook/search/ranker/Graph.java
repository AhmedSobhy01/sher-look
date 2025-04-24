package com.sherlook.search.ranker;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
    Map<Integer, List<Integer>> outgoingLinks;
    Map<Integer, List<Integer>> incomingLinks;
    Set<Integer> danglingNodes;

    public Graph(Map<Integer, List<Integer>> outgoingLinks, Map<Integer, List<Integer>> incomingLinks, Set<Integer> danglingNodes) {
        this.outgoingLinks = outgoingLinks;
        this.incomingLinks = incomingLinks;
        this.danglingNodes = danglingNodes;
    }
}
