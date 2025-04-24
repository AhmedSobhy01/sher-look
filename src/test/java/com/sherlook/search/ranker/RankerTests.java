package com.sherlook.search.ranker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankerTest {

  @Mock private DatabaseHelper databaseHelper;

  @InjectMocks private Ranker ranker;

  private static final double DELTA = 1e-6;

  @BeforeEach
  @Test
  void testGetDocumentRelevance_TypicalCase() {
    // Arrange
    List<String> queryTerms = Arrays.asList("machine", "learning");
    when(databaseHelper.getTotalDocumentCount()).thenReturn(1000);
    when(databaseHelper.getTermFrequencyAcrossDocuments(queryTerms))
        .thenReturn(Map.of("machine", 50, "learning", 20));

    // Document 1: "machine" in title (2x), "learning" in body (1x)
    // Document 2: "machine" in body (1x), "learning" in header (1x)
    // Document 3: "machine" in body (1x), no "learning"
    List<DocumentTerm> documentTerms =
        Arrays.asList(
            new DocumentTerm(
                "machine",
                1,
                "https://example.com",
                "AI Guide",
                100,
                Map.of("title", Arrays.asList(5, 10))),
            new DocumentTerm(
                "learning",
                1,
                "https://example.com",
                "AI Guide",
                100,
                Map.of("body", Arrays.asList(11))),
            new DocumentTerm(
                "machine",
                2,
                "https://example2.com",
                "Tech Blog",
                50,
                Map.of("body", Arrays.asList(3))),
            new DocumentTerm(
                "learning",
                2,
                "https://example2.com",
                "Tech Blog",
                50,
                Map.of("header", Arrays.asList(4))),
            new DocumentTerm(
                "machine",
                3,
                "https://example3.com",
                "ML Intro",
                200,
                Map.of("body", Arrays.asList(7))));
    when(databaseHelper.getDocumentTerms(queryTerms)).thenReturn(documentTerms);

    // Act
    List<RankedDocument> result = ranker.getDocumentTfIdf(queryTerms, false);

    // Assert
    assertEquals(3, result.size(), "Should return three documents");

    // Calculate expected TF-IDF scores
    // IDF calculations:
    // idf(machine) = log10(1000 / (50 + 0.0001)) ≈ 1.30102999566
    // idf(learning) = log10(1000 / (20 + 0.0001)) ≈ 1.69897000433

    // Document 2:
    // machine: TF = 1/50 = 0.02, weighted (body, 1.0) = 0.02 * 1.0 = 0.02
    // learning: TF = 1/50 = 0.02, weighted (header, 1.5) = 0.02 * 1.5 = 0.03
    // TF-IDF = (0.02 * 1.30102999566) + (0.03 * 1.69897000433) ≈ 0.0260205999 + 0.0509691001 ≈
    // 0.0769897000
    RankedDocument doc2 = result.get(0);
    assertEquals(2, doc2.getDocId(), "Document 2 ID");
    assertEquals("https://example2.com", doc2.getUrl(), "Document 2 URL");
    assertEquals("Tech Blog", doc2.getTitle(), "Document 2 title");
    assertEquals(0.0769897000, doc2.getTfIdf(), DELTA, "Document 2 TF-IDF score");

    // Document 1:
    // machine: TF = 2/100 = 0.02, weighted (title, 2.0) = 0.02 * 2.0 = 0.04
    // learning: TF = 1/100 = 0.01, weighted (body, 1.0) = 0.01 * 1.0 = 0.01
    // TF-IDF = (0.04 * 1.30102999566) + (0.01 * 1.69897000433) ≈ 0.0520411998 + 0.0169897000 ≈
    // 0.0690308998
    RankedDocument doc1 = result.get(1);
    assertEquals(1, doc1.getDocId(), "Document 1 ID");
    assertEquals("https://example.com", doc1.getUrl(), "Document 1 URL");
    assertEquals("AI Guide", doc1.getTitle(), "Document 1 title");
    assertEquals(0.0690308998, doc1.getTfIdf(), DELTA, "Document 1 TF-IDF score");

    // Document 3:
    // machine: TF = 1/200 = 0.005, weighted (body, 1.0) = 0.005 * 1.0 = 0.005
    // learning: not present, so 0
    // TF-IDF = (0.005 * 1.30102999566) + 0 ≈ 0.0065051499783
    RankedDocument doc3 = result.get(2);
    assertEquals(3, doc3.getDocId(), "Document 3 ID");
    assertEquals("https://example3.com", doc3.getUrl(), "Document 3 URL");
    assertEquals("ML Intro", doc3.getTitle(), "Document 3 title");
    assertEquals(0.0065051499783, doc3.getTfIdf(), DELTA, "Document 3 TF-IDF score");

    // Verify sorting (highest TF-IDF first)
    assertTrue(
        result.get(0).getTfIdf() > result.get(1).getTfIdf(),
        "Document 2 should have higher TF-IDF than Document 1");
    assertTrue(
        result.get(1).getTfIdf() > result.get(2).getTfIdf(),
        "Document 1 should have higher TF-IDF than Document 3");

    result.forEach(
        doc -> {
          assertNotNull(doc.getUrl(), "URL should not be null");
          assertNotNull(doc.getTitle(), "Title should not be null");
          assertTrue(doc.getTfIdf() >= 0, "TF-IDF should be non-negative");
        });
  }

  // pagerank tests
  @Test
  public void testComputePageRank_SimpleGraph() {
    // 3 documents, simple link structure
    // Doc 1 -> Doc 2, Doc 2 -> Doc 3, Doc 3 -> Doc 1 , should result in 1/3 in a single iteration
    List<Integer> docIds = Arrays.asList(1, 2, 3);
    List<Link> links =
        Arrays.asList(
            new Link(1, 2), // Doc 1 links to Doc 2
            new Link(2, 3), // Doc 2 links to Doc 3
            new Link(3, 1) // Doc 3 links to Doc 1
            );

    Map<Integer, Double> scores = ranker.computePageRank(docIds, links);

    // Verify
    assertEquals(3, scores.size(), "Should have scores for all 3 documents");
    assertTrue(scores.containsKey(1), "Score for Doc 1 missing");
    assertTrue(scores.containsKey(2), "Score for Doc 2 missing");
    assertTrue(scores.containsKey(3), "Score for Doc 3 missing");

    // In a balanced cycle, scores should be approximately equal
    double expectedScore = 1.0 / 3.0; // Idealized, assuming normalization
    double tolerance = 0.05; // Allow for numerical precision
    assertEquals(expectedScore, scores.get(1), tolerance, "Doc 1 score incorrect");
    assertEquals(expectedScore, scores.get(2), tolerance, "Doc 2 score incorrect");
    assertEquals(expectedScore, scores.get(3), tolerance, "Doc 3 score incorrect");

    // Verify scores sum to ~1
    double sum = scores.values().stream().mapToDouble(Double::doubleValue).sum();
    assertEquals(1.0, sum, 0.05, "Scores should sum to approximately 1");
  }

  @Test
  public void testComputePageRank_DanglingNode() {
    List<Integer> docIds = Arrays.asList(1, 2);
    List<Link> links = Arrays.asList(new Link(1, 2));

    Map<Integer, Double> scores = ranker.computePageRank(docIds, links);

    assertEquals(2, scores.size(), "Should have scores for both documents");
    assertTrue(scores.containsKey(1), "Score for Doc 1 missing");
    assertTrue(scores.containsKey(2), "Score for Doc 2 missing");
    assertNotNull(scores.get(1), "Score for Doc 1 is null");
    assertNotNull(scores.get(2), "Score for Doc 2 is null");
    assertTrue(scores.get(2) > scores.get(1), "Doc 2 should have higher score than Doc 1");

    double sum = scores.values().stream().mapToDouble(Double::doubleValue).sum();
    // verifies the algorithm does not 'leak' scores to dangling nodes
    assertEquals(1.0, sum, 0.05, "Scores should sum to approximately 1");
  }

  @Test
  public void testComputePageRank_ComplexGraph() {
    // Complex graph:
    // 1 -> 2,3,4 (hub)
    // 2 -> 3
    // 3 -> 1,4 (authority)
    // 4 -> none (dangling)
    // 5 -> 1,3 (no incoming links)
    List<Integer> docIds = Arrays.asList(1, 2, 3, 4, 5);
    List<Link> links =
        Arrays.asList(
            new Link(1, 2),
            new Link(1, 3),
            new Link(1, 4),
            new Link(2, 3),
            new Link(3, 1),
            new Link(3, 4),
            new Link(5, 1),
            new Link(5, 3));

    Map<Integer, Double> scores = ranker.computePageRank(docIds, links);

    assertEquals(5, scores.size(), "Should have scores for all 5 documents");
    for (Integer docId : docIds) {
      assertTrue(scores.containsKey(docId), "Score for Doc " + docId + " missing");
      assertNotNull(scores.get(docId), "Score for Doc " + docId + " is null");
    }

    // Expected converged scores (approximate, based on simulation)
    double[] expectedScores = {0.2297, 0.1415, 0.2896, 0.2600, 0.0789};
    int[] docIdsArray = {1, 2, 3, 4, 5};
    double tolerance = 0.05;
    for (int i = 0; i < docIdsArray.length; i++) {
      assertEquals(
          expectedScores[i],
          scores.get(docIdsArray[i]),
          tolerance,
          "Score for Doc " + docIdsArray[i] + " incorrect");
    }

    assertTrue(
        scores.get(3) > scores.get(4),
        "Doc 3 (authority) should rank higher than Doc 4 (dangling)");
    assertTrue(
        scores.get(4) > scores.get(1), "Doc 4 (dangling) should rank higher than Doc 1 (hub)");
    assertTrue(scores.get(1) > scores.get(2), "Doc 1 (hub) should rank higher than Doc 2");
    assertTrue(scores.get(2) > scores.get(5), "Doc 2 should rank higher than Doc 5");

    double sum = scores.values().stream().mapToDouble(Double::doubleValue).sum();
    assertEquals(1.0, sum, 0.05, "Scores should sum to approximately 1");
  }
}
