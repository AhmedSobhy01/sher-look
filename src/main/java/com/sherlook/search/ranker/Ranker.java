package com.sherlook.search.ranker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;

@Component
public class Ranker {

  private final DatabaseHelper databaseHelper;

  private static final Map<String, Double> SECTION_WEIGHTS = Map.of("title", 2.0, "header", 1.5, "body", 1.0);
  private static final double DAMPING_FACTOR_PAGE_RANK = 0.85;
  private static final double CONVERGENCE_THRESHOLD = 0.00001;
  private static final double MAX_ITERATIONS = 100;
  private static final double TF_IDF_CONTRIBUTION = 0.7;
  private static final double PAGE_RANK_CONTRIBUTION = 0.3;

  // for optimization, helps me avoid two getDocumentTerms db calls, the most
  // expensive one
  public static class RankingResult {
    private final List<RankedDocument> rankedDocuments;
    private final List<DocumentTerm> documentTerms;

    public RankingResult(List<RankedDocument> rankedDocuments, List<DocumentTerm> documentTerms) {
      this.rankedDocuments = rankedDocuments;
      this.documentTerms = documentTerms;
    }

    public List<RankedDocument> getRankedDocuments() {
      return rankedDocuments;
    }

    public List<DocumentTerm> getDocumentTerms() {
      return documentTerms;
    }
  }

  @Autowired
  public Ranker(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  public List<RankedDocument> getDocumentTfIdf(
      List<String> queryTerms, List<DocumentTerm> documentTerms) {

    // get idf
    Map<String, Double> idfMap = databaseHelper.getIDF(queryTerms);
    System.out.println("IDF map: " + idfMap);

    // Group by doc id
    Map<Integer, List<DocumentTerm>> docGroups = documentTerms.stream()
        .collect(Collectors.groupingBy(DocumentTerm::getDocumentId));
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
        for (Map.Entry<String, List<Integer>> sectionEntry : dt.getPositionsBySection().entrySet()) {
          String section = sectionEntry.getKey();
          int frequency = sectionEntry.getValue().size();
          double tf = (double) frequency / dt.getDocumentSize();
          double weight = SECTION_WEIGHTS.getOrDefault(section, 1.0);
          weightedTf += tf * weight;
        }
        double idf = idfMap.getOrDefault(dt.getWord(), 0.0);
        tfIdfSum += weightedTf * idf;
      }
      rankedDocs.add(
          new RankedDocument(
              docId,
              firstTerm.getUrl(),
              firstTerm.getTitle(),
              tfIdfSum,
              firstTerm.getDescription()));
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
        double newRank = (1 - DAMPING_FACTOR_PAGE_RANK) / numDocs
            + DAMPING_FACTOR_PAGE_RANK * (incomingSum + danglingContribution);
        pageRankCurrent.put(docId, newRank);
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
    return pageRankPrevious;
  }

  public void rankPagesByPopularity() {
    System.out.println("Started ranking pages by popularity");
    List<Integer> docIds = databaseHelper.getAllDocumentIds();
    List<Link> links = databaseHelper.getLinks();
    Map<Integer, Double> pageRankScores = computePageRank(docIds, links);
    System.out.println("PageRank scores computed");
    databaseHelper.batchUpdatePageRank(pageRankScores);
    System.out.println("PageRank scores updated in the database");
  }

  public List<RankedDocument> getDocumentTfIdfPhrases(
      List<String> queryTerms, List<DocumentTerm> documentTerms) {

    Map<String, Double> idfMap = databaseHelper.getIDF(queryTerms);

    Map<Integer, List<DocumentTerm>> docGroups = documentTerms.stream()
        .collect(Collectors.groupingBy(DocumentTerm::getDocumentId));
    Map<Integer, List<DocumentTerm>> docGroups = documentTerms.stream()
        .collect(Collectors.groupingBy(DocumentTerm::getDocumentId));

    List<RankedDocument> rankedDocs = new ArrayList<>();

    for (Map.Entry<Integer, List<DocumentTerm>> entry : docGroups.entrySet()) {
      int docId = entry.getKey();
      List<DocumentTerm> terms = entry.getValue();

      Set<String> foundTermsSet = terms.stream().map(DocumentTerm::getWord).collect(Collectors.toSet());

      if (!foundTermsSet.containsAll(queryTerms)) {
        continue; // Skip if document doesn't contain all query terms
      }

      DocumentTerm firstTerm = terms.get(0);

      // Check if the terms appear as a phrase in any section
      if (containsPhrase(terms, queryTerms)) {
        double score = calculatePhraseScore(terms, idfMap);
        rankedDocs.add(
            new RankedDocument(
                docId,
                firstTerm.getUrl(),
                firstTerm.getTitle(),
                score,
                firstTerm.getDescription()));
      }
    }

    return rankedDocs;
  }

  private boolean containsPhrase(List<DocumentTerm> terms, List<String> queryTerms) {
    // Map terms for quick lookup
    Map<String, DocumentTerm> termMap = new HashMap<>();
    for (DocumentTerm term : terms) {
      termMap.put(term.getWord(), term);
    }

    // Check each section
    for (String section : terms.get(0).getPositionsBySection().keySet()) {
      // Ensure all query terms exist in this section
      boolean allTermsExist = queryTerms.stream()
          .allMatch(
              queryTerm -> termMap.containsKey(queryTerm)
                  && termMap.get(queryTerm).getPositionsBySection().containsKey(section));

      if (allTermsExist && hasConsecutivePositions(termMap, queryTerms, section)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasConsecutivePositions(
      Map<String, DocumentTerm> termMap, List<String> queryTerms, String section) {

    DocumentTerm firstTerm = termMap.get(queryTerms.get(0));
    if (firstTerm == null || !firstTerm.getPositionsBySection().containsKey(section)) {
      return false;
    }

    List<Integer> firstPositions = firstTerm.getPositionsBySection().get(section);

    for (int startPos : firstPositions) {
      boolean phraseFound = true;

      // Check if subsequent terms are at consecutive positions
      for (int i = 1; i < queryTerms.size(); i++) {
        DocumentTerm nextTerm = termMap.get(queryTerms.get(i));
        if (nextTerm == null
            || !nextTerm.getPositionsBySection().containsKey(section)
            || nextTerm.getPositionsBySection().get(section) == null) {
          phraseFound = false;
          break;
        }

        int expectedPos = startPos + i;
        List<Integer> positions = nextTerm.getPositionsBySection().get(section);

        if (!positions.contains(expectedPos)) {
          phraseFound = false;
          break;
        }
      }

      if (phraseFound)
        return true;
    }

    return false;
  }

  private double calculatePhraseScore(List<DocumentTerm> terms, Map<String, Double> idfMap) {
    double score = 0.0;

    // Calculate scores based on frequency, section weight, and IDF
    for (DocumentTerm term : terms) {
      double idf = idfMap.getOrDefault(term.getWord(), 1.0);
      double termScore = 0.0;

      for (Map.Entry<String, List<Integer>> entry : term.getPositionsBySection().entrySet()) {
        String section = entry.getKey();
        double sectionWeight = SECTION_WEIGHTS.getOrDefault(section, 1.0);
        int frequency = entry.getValue().size(); // Individual term frequency

        termScore += (frequency * sectionWeight) / term.getDocumentSize();
      }

      score += termScore * idf;
    }

    return score;
  }

  public RankingResult rankAndStoreTotalDocuments(List<String> queryTerms, Boolean isPhraseSearch) {
    long start = System.currentTimeMillis();

    List<DocumentTerm> documentTerms = databaseHelper.getDocumentTerms(queryTerms);

    List<RankedDocument> tfIdfDocs;
    if (isPhraseSearch) {
      tfIdfDocs = getDocumentTfIdfPhrases(queryTerms, documentTerms);
    } else {
      tfIdfDocs = getDocumentTfIdf(queryTerms, documentTerms);
    }

    // Apply PageRank
    List<Integer> docIds = tfIdfDocs.stream().map(RankedDocument::getDocId).collect(Collectors.toList());
    Map<Integer, Double> pageRankScores = databaseHelper.getPageRank(docIds);

    for (RankedDocument doc : tfIdfDocs) {
      double tfIdfScore = doc.getTfIdf();
      double pageRankScore = pageRankScores.getOrDefault(doc.getDocId(), 0.0);
      double finalScore = TF_IDF_CONTRIBUTION * tfIdfScore + PAGE_RANK_CONTRIBUTION * pageRankScore;
      doc.setFinalScore(finalScore);
    }

    // Sort by final score
    tfIdfDocs.sort((d1, d2) -> Double.compare(d2.getFinalScore(), d1.getFinalScore()));

    long end = System.currentTimeMillis();
    System.out.println("Ranking time: " + (end - start) + " ms");

    return new RankingResult(tfIdfDocs, documentTerms);
  }

  private void generateSnippets(
      List<RankedDocument> documents, List<DocumentTerm> allDocTerms, List<String> queryTerms) {
    long totalStart = System.currentTimeMillis();

    // Measure time for filtering documents
    long filterStart = System.currentTimeMillis();
    Set<Integer> docIds = documents.stream().map(RankedDocument::getDocId).collect(Collectors.toSet());
    List<DocumentTerm> relevantTerms = allDocTerms.stream()
        .filter(term -> docIds.contains(term.getDocumentId()))
        .collect(Collectors.toList());
    Set<Integer> docIds = documents.stream().map(RankedDocument::getDocId).collect(Collectors.toSet());
    List<DocumentTerm> relevantTerms = allDocTerms.stream()
        .filter(term -> docIds.contains(term.getDocumentId()))
        .collect(Collectors.toList());
    long filterEnd = System.currentTimeMillis();
    System.out.println("Filtering relevant terms time: " + (filterEnd - filterStart) + " ms");

    // Measure time for position calculation
    long positionStart = System.currentTimeMillis();
    Map<Integer, List<Integer>> docPositions = new HashMap<>();

    for (DocumentTerm term : relevantTerms) {
      int docId = term.getDocumentId();

      // Find earliest position across all sections
      int earliestPos = Integer.MAX_VALUE;
      for (List<Integer> positions : term.getPositionsBySection().values()) {
        if (!positions.isEmpty()) {
          int minPos = Collections.min(positions);
          if (minPos < earliestPos) {
            earliestPos = minPos;
          }
        }
      }

      if (earliestPos != Integer.MAX_VALUE) {
        docPositions.computeIfAbsent(docId, k -> new ArrayList<>()).add(earliestPos);
      }
    }
    long positionEnd = System.currentTimeMillis();
    System.out.println("Finding positions time: " + (positionEnd - positionStart) + " ms");

    // Measure database call time
    long dbStart = System.currentTimeMillis();
    Map<Integer, Map<Integer, String>> surroundingWords = databaseHelper.getWordsAroundPositions(docPositions, 10);
    Map<Integer, Map<Integer, String>> surroundingWords = databaseHelper.getWordsAroundPositions(docPositions, 10);
    long dbEnd = System.currentTimeMillis();
    System.out.println("Database call for surrounding words: " + (dbEnd - dbStart) + " ms");

    // Measure snippet creation time
    long snippetStart = System.currentTimeMillis();
    for (RankedDocument doc : documents) {
      Map<Integer, String> wordMap = surroundingWords.getOrDefault(doc.getDocId(), Collections.emptyMap());

      if (wordMap.isEmpty()) {
        doc.setSnippet(doc.getDescription());
        continue;
      }

      // Sort positions
      List<Integer> positions = new ArrayList<>(wordMap.keySet());
      Collections.sort(positions);

      // Build snippet with highlighted terms
      StringBuilder snippet = new StringBuilder("... ");
      Set<String> queryLower = queryTerms.stream().map(String::toLowerCase).collect(Collectors.toSet());

      for (Integer pos : positions) {
        String word = wordMap.get(pos);
        if (queryLower.contains(word.toLowerCase())) {
          snippet.append("<b>").append(word).append("</b> ");
        } else {
          snippet.append(word).append(" ");
        }
      }
      snippet.append("...");

      doc.setSnippet(snippet.toString());
    }
    long snippetEnd = System.currentTimeMillis();
    System.out.println("Snippet creation time: " + (snippetEnd - snippetStart) + " ms");

    long totalEnd = System.currentTimeMillis();
    System.out.println("Total snippet generation time: " + (totalEnd - totalStart) + " ms");
  }

  public List<RankedDocument> getPageWithSnippets(
      RankingResult result, List<String> queryTerms, int offset, int limit) {
    List<RankedDocument> allDocs = result.getRankedDocuments();
    List<DocumentTerm> documentTerms = result.getDocumentTerms();

    int endIndex = Math.min(offset + limit, allDocs.size());
    List<RankedDocument> pagedResults = offset < allDocs.size() ? allDocs.subList(offset, endIndex) : new ArrayList<>();

    if (!pagedResults.isEmpty()) {
      generateSnippets(pagedResults, documentTerms, queryTerms);
    }

    return pagedResults;
  }
}
