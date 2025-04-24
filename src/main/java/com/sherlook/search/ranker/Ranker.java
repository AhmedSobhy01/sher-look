package com.sherlook.search.ranker;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.utils.ConsoleColors;
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
  private static final double MAX_ITERATIONS = 100;

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

  private Graph buildGraph(List<Integer> docIds, List<Link> links) {
    Map<Integer, List<Integer>> incomingLinks = new HashMap<>();
    Map<Integer, Integer> outgoingLinkCount = new HashMap<>();
    Set<Integer> danglingNodes = new HashSet<>();
    for (int docId : docIds) {
      incomingLinks.put(docId, new ArrayList<>());
      outgoingLinkCount.put(docId, 0);
    }
    for (Link link : links) {
      int source = link.getSourceId();
      int target = link.getTargetId();
      outgoingLinkCount.put(source, outgoingLinkCount.get(source) + 1);
      incomingLinks.get(target).add(source);
    }

    for (int docId : docIds) {
      if (outgoingLinkCount.get(docId) == 0) {
        danglingNodes.add(docId);
      }
    }

    return new Graph(outgoingLinkCount, incomingLinks, danglingNodes);
  }

  public Map<Integer, Double> computePageRank(List<Integer> docIds, List<Link> links) {
    Graph graph = buildGraph(docIds, links);
    Map<Integer, Double> pageRankPrevious = new HashMap<>();
    Map<Integer, Double> pageRankCurrent = new HashMap<>();
    int N = docIds.size();

    for (int docId : docIds) {
      pageRankPrevious.put(docId, 1.0 / docIds.size()); // assuming uniform distribution
      pageRankCurrent.put(docId, 0.0);
    }

    boolean converged = false;
    for (int i = 0; i < MAX_ITERATIONS; ++i) {
      double S = 0.0;
      for (int danglingNode : graph.danglingNodes) {
        S += pageRankPrevious.getOrDefault(danglingNode, 0.0);
      }
      // uniform dangling contribution
      double danglingContribution = S / N;


      for (int docId : docIds) {
        double incomingSum = 0.0;
        for (int source : graph.incomingLinks.get(docId)) {
          int outDegree = graph.outgoingLinkCount.get(source);
          if (outDegree > 0) {
            incomingSum += pageRankPrevious.get(source) / outDegree;
          }
        }
        double newRank =
            (1 - DAMPING_FACTOR_PAGE_RANK) / N
                + DAMPING_FACTOR_PAGE_RANK * (incomingSum + danglingContribution);
        pageRankCurrent.put(docId, newRank);
      }


      // Normalize current scores to sum to 1
      double sum = pageRankCurrent.values().stream().mapToDouble(Double::doubleValue).sum();
      if(sum > 0){
        for (int docId : docIds) {
          pageRankCurrent.put(docId, pageRankCurrent.get(docId) / sum);
        }
      }

      double maxDiff = 0.0;
      for (int docId : docIds) {
        double diff = Math.abs(pageRankCurrent.get(docId) - pageRankPrevious.get(docId));
        maxDiff = Math.max(maxDiff, diff);
      }

      //check convergence
      if (maxDiff < CONVERGENCE_THRESHOLD) {
        ConsoleColors.printSuccess(
            "PageRank converged after " + (i + 1) + " iterations" + " with max diff: " + maxDiff);
        converged = true;
        break;
      }
      pageRankPrevious = pageRankCurrent;
      pageRankCurrent = new HashMap<>();
    }

    if (!converged) {
      System.out.println("PageRank did not converge after " + MAX_ITERATIONS + " iterations");
    }

    System.out.println("Scores");
    for (int docId : docIds) {
      System.out.println("Doc ID: " + docId + ", Score: " + pageRankPrevious.get(docId));
    }
    return pageRankPrevious;
  }

  /**
   * This method is to be called directly after crawling, and parallel to indexing. It computes and
   * updates the page rank score of all documents in the database.
   */
  public void rankPagesByPopularity() {
    System.out.println("Started ranking pages by popularity");
    List<Integer> docIds = databaseHelper.getAllDocumentIds();
    List<Link> links = databaseHelper.getLinks();
    Map<Integer, Double> pageRankScores = computePageRank(docIds, links);
    System.out.println("PageRank scores computed");
    databaseHelper.batchUpdatePageRank(pageRankScores);
    System.out.println("PageRank scores updated in the database");
  }

  public void rankDocuments() {
    System.out.println("Ranking documents...");
  }
}
