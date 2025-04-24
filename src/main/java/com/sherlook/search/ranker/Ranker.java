package com.sherlook.search.ranker;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.utils.DatabaseHelper;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Ranker {

  private QueryProcessor queryProcessor;
  private DatabaseHelper databaseHelper;
  private static final Map<String, Double> SECTION_WEIGHTS =
      Map.of("title", 2.0, "header", 1.5, "body", 1.0);
  private static final double IDF_SMOOTHING_FACTOR = 0.0001;
  private static final double DAMPING_FACTOR_PAGE_RANK = 0.85;
  private static final double CONVERGENCE_THRESHOLD = 0.00001;

  @Autowired
  public Ranker(QueryProcessor queryProcessor, DatabaseHelper databaseHelper) {
    this.queryProcessor = queryProcessor;
    this.databaseHelper = databaseHelper;
  }

  public List<RankedDocument> getDocumentRelevance(
      List<String> queryTerms, Boolean isPhraseSearch) {
    List<DocumentTerm> documentTerms = databaseHelper.getDocumentTerms(queryTerms);
    Map<String, Integer> termFrequencies =
        databaseHelper.getTermFrequencyAcrossDocuments(queryTerms);
    int totalDocumentCount = databaseHelper.getTotalDocumentCount();

    // compute idf
    // this computation can be done once after indexing
    Map<String, Double> idfMap = new HashMap<>();
    for (String term : queryTerms) {
      int df = termFrequencies.getOrDefault(term, 0);
      double idf =
          Math.log10(
              (double) totalDocumentCount
                  / (df + IDF_SMOOTHING_FACTOR)); // Add a small constant to avoid division by zero
      idfMap.put(term, idf);
    }

    // Group by doc id
    Map<Integer, List<DocumentTerm>> docGroups =
        documentTerms.stream().collect(Collectors.groupingBy(DocumentTerm::getDocumentId));
    List<RankedDocument> rankedDocs = new ArrayList<>();

    for (Map.Entry<Integer, List<DocumentTerm>> entry : docGroups.entrySet()) {
      int docId = entry.getKey();
      List<DocumentTerm> terms = entry.getValue();
      DocumentTerm firstTerm = terms.get(0);

      // if(isPhraseSearch)
      // continue;

      double tfIdfSum = 0.0;
      for (DocumentTerm dt : terms) {
        double weightedTf = 0.0;
        for (Map.Entry<String, List<Integer>> sectionEntry :
            dt.getPositionsBySection().entrySet()) {
          String section = sectionEntry.getKey();
          int frequency = sectionEntry.getValue().size();
          double tf = (double) frequency / dt.getWordCountInDocument();
          double weight = SECTION_WEIGHTS.getOrDefault(section, 1.0);
          weightedTf += tf * weight;
        }
        double idf = idfMap.getOrDefault(dt.getWord(), 0.0);
        tfIdfSum += weightedTf * idf;
      }
      rankedDocs.add(new RankedDocument(docId, firstTerm.getUrl(), firstTerm.getTitle(), tfIdfSum));
    }

    rankedDocs.sort((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()));
    return rankedDocs;
  }

  private Graph buildGraph(List<Integer> docIds, List<Link> links){
    Map<Integer, List<Integer>> outgoingLinks = new HashMap<>();
    Map<Integer, List<Integer>> incomingLinks = new HashMap<>();
    Set<Integer> danglingNodes = new HashSet<>();
    for(int docId : docIds){
        outgoingLinks.put(docId, new ArrayList<>());
        incomingLinks.put(docId, new ArrayList<>());
    }
    for(Link link : links){
      int source = link.getSourceId();
        int target = link.getTargetId();
        outgoingLinks.get(source).add(target);
        incomingLinks.get(target).add(source);
    }

    for(int docId : docIds){
        if(outgoingLinks.get(docId).isEmpty()){
            danglingNodes.add(docId);
        }
    }

    return new Graph(outgoingLinks, incomingLinks, danglingNodes);
  }

  public void rankDocuments() {
    System.out.println("Ranking documents...");
  }
}
