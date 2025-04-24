package com.sherlook.search.ranker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import java.util.*;
import java.util.stream.Collectors;
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
  void testGetTfIdf_TypicalCase() {
    List<String> queryTerms = Arrays.asList("machine", "learning");
    when(databaseHelper.getTotalDocumentCount()).thenReturn(1000);

    Map<String, Double> mockIdfMap = Map.of(
            "machine", Math.log(1000.0 / (50 + 1)),  // ≈ 2.976
            "learning", Math.log(1000.0 / (20 + 1))  // ≈ 3.863
    );
    when(databaseHelper.getIDF(queryTerms)).thenReturn(mockIdfMap);

    List<DocumentTerm> documentTerms = Arrays.asList(
            new DocumentTerm("machine", 1, "https://example.com", "AI Guide", 100,
                    Map.of("title", Arrays.asList(5, 10))), // 2x in title
            new DocumentTerm("learning", 1, "https://example.com", "AI Guide", 100,
                    Map.of("body", Arrays.asList(11))),      // 1x in body
            new DocumentTerm("machine", 2, "https://example2.com", "Tech Blog", 50,
                    Map.of("body", Arrays.asList(3))),       // 1x in body
            new DocumentTerm("learning", 2, "https://example2.com", "Tech Blog", 50,
                    Map.of("header", Arrays.asList(4))),     // 1x in header
            new DocumentTerm("machine", 3, "https://example3.com", "ML Intro", 200,
                    Map.of("body", Arrays.asList(7)))        // 1x in body
    );
    when(databaseHelper.getDocumentTerms(queryTerms)).thenReturn(documentTerms);

    List<RankedDocument> result = ranker.getDocumentTfIdf(queryTerms, false);

    assertEquals(3, result.size(), "Should return three documents");
    Map<Integer, RankedDocument> docsById = result.stream()
            .collect(Collectors.toMap(RankedDocument::getDocId, d -> d));

    // Document 1 (docId=1)
    RankedDocument doc1 = docsById.get(1);
    assertEquals("https://example.com", doc1.getUrl(), "Document 1 URL");
    assertEquals("AI Guide", doc1.getTitle(), "Document 1 title");
    // TF-IDF calculation:
    // "machine": TF = 2/100 = 0.02 → weighted (title ×2) → 0.02*2 = 0.04 → 0.04 * 2.976 ≈ 0.119
    // "learning": TF = 1/100 = 0.01 → weighted (body ×1) → 0.01*1 = 0.01 → 0.01 * 3.863 ≈ 0.0386
    // Total ≈ 0.119 + 0.0386 = 0.1576
    assertEquals(0.1576, doc1.getTfIdf(), 0.001, "Document 1 TF-IDF score");

    // Document 2 (docId=2)
    RankedDocument doc2 = docsById.get(2);
    assertNotNull(doc2, "Document 2 should be in results");
    assertEquals("https://example2.com", doc2.getUrl(), "Document 2 URL");
    assertEquals("Tech Blog", doc2.getTitle(), "Document 2 title");
    // TF-IDF calculation:
    // "machine": TF = 1/50 = 0.02 → weighted (body ×1) → 0.02*1 = 0.02 → 0.02 * 2.976 ≈ 0.0595
    // "learning": TF = 1/50 = 0.02 → weighted (header ×1.5) → 0.02*1.5 = 0.03 → 0.03 * 3.863 ≈ 0.1159
    // Total ≈ 0.0595 + 0.1159 = 0.1754
    assertEquals(0.1754, doc2.getTfIdf(), 0.001, "Document 2 TF-IDF score");

    // Document 3 (docId=3)
    RankedDocument doc3 = docsById.get(3);
    assertNotNull(doc3, "Document 3 should be in results");
    assertEquals("https://example3.com", doc3.getUrl(), "Document 3 URL");
    assertEquals("ML Intro", doc3.getTitle(), "Document 3 title");
    // TF-IDF calculation:
    // "machine": TF = 1/200 = 0.005 → weighted (body ×1) → 0.005*1 = 0.005 → 0.005 * 2.976 ≈ 0.0149
    assertEquals(0.0149, doc3.getTfIdf(), 0.001, "Document 3 TF-IDF score");
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
