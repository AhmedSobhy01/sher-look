package com.sherlook.search.ranker;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RankerTests {

  @Autowired private Ranker ranker;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setup() {
    jdbcTemplate.execute("DELETE FROM document_words");
    jdbcTemplate.execute("DELETE FROM documents");
    jdbcTemplate.execute("DELETE FROM words");

    jdbcTemplate.update(
        "INSERT INTO words (id, word) VALUES (1, 'machine'), (2, 'learning'), (3, 'data')");

    jdbcTemplate.update(
        "INSERT INTO documents (id, url, title, description, file_path, crawl_time) VALUES "
            + "(1, 'http://example.com', 'Doc 1', 'description 1', 'path1', CURRENT_TIMESTAMP), "
            + "(2, 'http://example2.com', 'Doc 2', 'description 2', 'path2', CURRENT_TIMESTAMP), "
            + "(3, 'http://example3.com', 'Doc 3', 'description 3', 'path3', CURRENT_TIMESTAMP), "
            + "(4, 'http://example4.com', 'Doc 4', 'description 4', 'path4', CURRENT_TIMESTAMP)");

    jdbcTemplate.update(
        "INSERT INTO document_words (document_id, word_id, position) VALUES "
            +
            // Doc 1: 'machine' (x2), 'learning' (x1)
            "(1, 1, 1), (1, 1, 2), (1, 2, 3), "
            +
            // Doc 2: 'machine' (x1)
            "(2, 1, 1), "
            +
            // Doc 3: 'learning' (x2), 'data' (x1)
            "(3, 2, 1), (3, 2, 2), (3, 3, 3), "
            +
            // Doc 4: 'data' (x1) - no query words from "machine learning"
            "(4, 3, 1)");
  }
  /*
   @Test
   void testGetRelevance() {
     List<RankedDocument> result = ranker.getRelevance("Machine Learning");

     // Expected results based on TF-IDF calculations
     List<RankedDocument> expected =
         List.of(
             new RankedDocument(2, "http://example2.com", "Doc 2", 0.3010), // 'learning' frequent
             new RankedDocument(
                 1, "http://example.com", "Doc 1", 0.3010), // Both terms, 'machine' frequent
             new RankedDocument(
                 3, "http://example3.com", "Doc 3", 0.2006) // Only 'machine', low score
             // doc 4 not relevant
             );

     assertEquals(expected.size(), result.size(), "Result size mismatch");

     for (int i = 0; i < expected.size(); i++) {
       RankedDocument exp = expected.get(i);
       RankedDocument res = result.get(i);
       assertEquals(exp.getDocumentId(), res.getDocumentId(), "Document ID mismatch at index " + i);
       assertEquals(exp.getUrl(), res.getUrl(), "URL mismatch at index " + i);
       assertEquals(exp.getTitle(), res.getTitle(), "Title mismatch at index " + i);
       assertEquals(exp.getTfIdf(), res.getTfIdf(), 0.001, "TF-IDF mismatch at index " + i);
     }
   }

  */
}
