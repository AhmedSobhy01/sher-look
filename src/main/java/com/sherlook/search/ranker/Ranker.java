package com.sherlook.search.ranker;

import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Ranker {

  private final DatabaseHelper databaseHelper;

  private static final Map<String, Double> SECTION_WEIGHTS =
      Map.of("title", 2.0, "header", 1.5, "body", 1.0);
  private static final double IDF_SMOOTHING_FACTOR = 0.0001;
  private static final double DAMPING_FACTOR_PAGE_RANK = 0.85;
  private static final double CONVERGENCE_THRESHOLD = 0.00001;
  private static final double MAX_ITERATIONS = 100;
  private static final double TF_IDF_CONTRIBUTION = 0.7;
  private static final double PAGE_RANK_CONTRIBUTION = 0.3;

  @Autowired
  public Ranker(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  public List<RankedDocument> getDocumentTfIdf(List<String> queryTerms, Boolean isPhraseSearch) {
    List<DocumentTerm> documentTerms = databaseHelper.getDocumentTerms(queryTerms);

    // get idf
    Map<String, Double> idfMap = databaseHelper.getIDF(queryTerms);

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
          double tf = (double) frequency / dt.getDocumentSize();
          double weight = SECTION_WEIGHTS.getOrDefault(section, 1.0);
          weightedTf += tf * weight;
        }
        double idf = idfMap.getOrDefault(dt.getWord(), 0.0);
        tfIdfSum += weightedTf * idf;
      }
      rankedDocs.add(new RankedDocument(docId, firstTerm.getUrl(), firstTerm.getTitle(), tfIdfSum));
    }

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
    int numDocs = docIds.size();

    for (int docId : docIds) {
      pageRankPrevious.put(docId, 1.0 / docIds.size()); // assuming uniform distribution
      pageRankCurrent.put(docId, 0.0);
    }

    boolean converged = false;
    for (int i = 0; i < MAX_ITERATIONS; ++i) {
      double sumDanglingNodeRanks = 0.0;
      for (int danglingNode : graph.danglingNodes) {
        sumDanglingNodeRanks += pageRankPrevious.getOrDefault(danglingNode, 0.0);
      }
      // uniform dangling contribution
      double danglingContribution = sumDanglingNodeRanks / numDocs;

      for (int docId : docIds) {
        double incomingSum = 0.0;
        for (int source : graph.incomingLinks.get(docId)) {
          int outDegree = graph.outgoingLinkCount.get(source);
          if (outDegree > 0) {
            incomingSum += pageRankPrevious.get(source) / outDegree;
          }
        }
        double newRank =
            (1 - DAMPING_FACTOR_PAGE_RANK) / numDocs
                + DAMPING_FACTOR_PAGE_RANK * (incomingSum + danglingContribution);
        pageRankCurrent.put(docId, newRank);
      }

      // Normalize current scores to sum to 1
      double sum = pageRankCurrent.values().stream().mapToDouble(Double::doubleValue).sum();
      if (sum > 0) {
        for (int docId : docIds) {
          pageRankCurrent.put(docId, pageRankCurrent.get(docId) / sum);
        }
      }

      double maxDiff = 0.0;
      for (int docId : docIds) {
        double diff = Math.abs(pageRankCurrent.get(docId) - pageRankPrevious.get(docId));
        maxDiff = Math.max(maxDiff, diff);
      }

      // check convergence
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

  /**
   * This method is the interface for the search engine to rank documents based on the query terms.
   * It combines the TF-IDF score and PageRank score to produce a final ranking.
   *
   * @param queryTerms
   * @param isPhraseSearch
   * @return List of ranked documents
   */
  public List<RankedDocument> rank(
      List<String> queryTerms, Boolean isPhraseSearch, int offset, int limit) {
    List<RankedDocument> tfIdfDocs = getDocumentTfIdf(queryTerms, isPhraseSearch);
    List<Integer> docIds =
        tfIdfDocs.stream().map(RankedDocument::getDocId).collect(Collectors.toList());
    Map<Integer, Double> pageRankScores = databaseHelper.getPageRank(docIds);

    for (RankedDocument doc : tfIdfDocs) {
      double tfIdfScore = doc.getTfIdf();
      double pageRankScore = pageRankScores.getOrDefault(doc.getDocId(), 0.0);
      double finalScore = TF_IDF_CONTRIBUTION * tfIdfScore + PAGE_RANK_CONTRIBUTION * pageRankScore;
      doc.setFinalScore(finalScore);
    }
    tfIdfDocs.sort((d1, d2) -> Double.compare(d2.getFinalScore(), d1.getFinalScore()));
    return tfIdfDocs;
  }
}
