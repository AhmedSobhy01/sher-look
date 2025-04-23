package com.sherlook.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sherlook.search.indexer.Document;
import com.sherlook.search.indexer.Section;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Rollback
class DatabaseHelperTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private DatabaseHelper databaseHelper;

  private static final String TEST_URL_PREFIX = "https://test-";
  private static final String TEST_TITLE = "Test Title";
  private static final String TEST_DESCRIPTION = "Test Description";
  private static final String TEST_FILE_PATH = "/path/to/test/file.pdf";
  private static final String TEST_HASH = "test-hash";

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM documents WHERE url LIKE ?", TEST_URL_PREFIX + "%");
  }

  @Test
  void testInsertDocument_WithAllFields_ShouldInsertSuccessfully() {
    String url = TEST_URL_PREFIX + "complete";

    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT * FROM documents WHERE url = ?", url);
    assertEquals(1, results.size(), "Should insert exactly one document");

    Map<String, Object> document = results.get(0);
    assertEquals(TEST_TITLE, document.get("title"), "Title should match inserted value");
    assertEquals(
        TEST_DESCRIPTION, document.get("description"), "Description should match inserted value");
    assertEquals(
        TEST_FILE_PATH, document.get("file_path"), "File path should match inserted value");
  }

  @Test
  void testInsertDocument_WithNullDescription_ShouldInsertWithNullDescription() {
    String url = TEST_URL_PREFIX + "null-description";

    databaseHelper.insertDocument(url, TEST_TITLE, null, TEST_FILE_PATH, TEST_HASH);

    Map<String, Object> document =
        jdbcTemplate.queryForMap("SELECT * FROM documents WHERE url = ?", url);
    assertEquals(TEST_TITLE, document.get("title"));
    assertNull(document.get("description"), "Description should be null");
    assertEquals(TEST_FILE_PATH, document.get("file_path"));
  }

  @Test
  void testInsertDocument_WithNullTitle_ShouldInsertWithNullTitle() {
    String url = TEST_URL_PREFIX + "null-title";

    databaseHelper.insertDocument(url, null, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    Map<String, Object> document =
        jdbcTemplate.queryForMap("SELECT * FROM documents WHERE url = ?", url);
    assertNull(document.get("title"), "Title should be null");
    assertEquals(TEST_DESCRIPTION, document.get("description"));
  }

  @Test
  void testInsertDocument_WithDuplicateUrl_ShouldThrowException() {
    String url = TEST_URL_PREFIX + "duplicate";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    UncategorizedSQLException exception =
        assertThrows(
            UncategorizedSQLException.class,
            () ->
                databaseHelper.insertDocument(
                    url, "Another Title", "Another Description", TEST_FILE_PATH, TEST_HASH),
            "Should throw exception when inserting document with duplicate URL");

    String exceptionMessage = exception.getMessage().toLowerCase();
    boolean hasConstraintViolation =
        exceptionMessage.contains("unique")
            || exceptionMessage.contains("duplicate")
            || exceptionMessage.contains("constraint");

    assertEquals(true, hasConstraintViolation, "Exception should mention constraint violation");
  }

  @Test
  void testCheckURLCrawled() {
    String url = TEST_URL_PREFIX + "check-crawled";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    boolean isCrawled = databaseHelper.isUrlCrawled(url);
    assertEquals(true, isCrawled, "URL should be marked as crawled");

    isCrawled = databaseHelper.isUrlCrawled("https://not-crawled-url.com");
    assertEquals(false, isCrawled, "URL should not be marked as crawled");
  }

  @Test
  void testGetCrawledPagesCount() {
    String url1 = TEST_URL_PREFIX + "count-1";
    String url2 = TEST_URL_PREFIX + "count-2";
    databaseHelper.insertDocument(url1, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);
    databaseHelper.insertDocument(url2, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    int count = databaseHelper.getCrawledPagesCount();
    assertEquals(2, count, "Should return the correct number of crawled pages");
  }

  @Test
  void testUpdateDocumentMetadata() {
    String url = TEST_URL_PREFIX + "update-metadata";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url);
    int documentId = ((Number) results.get(0).get("id")).intValue();

    String updatedTitle = "Updated Title";
    String updatedDescription = "Updated Description";

    databaseHelper.updateDocumentMetadata(documentId, updatedTitle, updatedDescription);

    Map<String, Object> document =
        jdbcTemplate.queryForMap("SELECT * FROM documents WHERE id = ?", documentId);
    assertEquals(updatedTitle, document.get("title"), "Title should be updated");
    assertEquals(updatedDescription, document.get("description"), "Description should be updated");
  }

  @Test
  void testBatchInsertDocumentWords() {
    String url = TEST_URL_PREFIX + "batch-insert";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url);
    int documentId = ((Number) results.get(0).get("id")).intValue();

    List<String> words = Arrays.asList("word1", "word2", "word3");
    List<Integer> positions = Arrays.asList(0, 1, 2);
    List<Section> sections = Arrays.asList(Section.TITLE, Section.HEADER, Section.BODY);

    databaseHelper.batchInsertDocumentWords(documentId, words, positions, sections);

    List<Map<String, Object>> wordEntries =
        jdbcTemplate.queryForList(
            "SELECT w.word, dw.position, dw.section FROM document_words dw "
                + "JOIN words w ON dw.word_id = w.id "
                + "WHERE dw.document_id = ? ORDER BY dw.position",
            documentId);

    assertEquals(3, wordEntries.size(), "Should have inserted all words");
    assertEquals("word1", wordEntries.get(0).get("word"));
    assertEquals(0, wordEntries.get(0).get("position"));
    assertEquals(Section.TITLE.toString(), wordEntries.get(0).get("section"));

    assertEquals("word2", wordEntries.get(1).get("word"));
    assertEquals(1, wordEntries.get(1).get("position"));
    assertEquals(Section.HEADER.toString(), wordEntries.get(1).get("section"));

    assertEquals("word3", wordEntries.get(2).get("word"));
    assertEquals(2, wordEntries.get(2).get("position"));
    assertEquals(Section.BODY.toString(), wordEntries.get(2).get("section"));
  }

  @Test
  void testGetOrCreateWordIds() {
    // Insert existing words
    jdbcTemplate.update("INSERT INTO words (word) VALUES (?)", "existing1");
    jdbcTemplate.update("INSERT INTO words (word) VALUES (?)", "existing2");

    List<String> wordsToCheck = Arrays.asList("existing1", "existing2", "new1", "new2");

    Map<String, Integer> wordIds = databaseHelper.getOrCreateWordIds(wordsToCheck);

    assertEquals(4, wordIds.size(), "Should return IDs for all words");

    // Check the existing words
    assertNotNull(wordIds.get("existing1"), "Should have ID for existing1");
    assertNotNull(wordIds.get("existing2"), "Should have ID for existing2");

    // Check the new words
    assertNotNull(wordIds.get("new1"), "Should have created ID for new1");
    assertNotNull(wordIds.get("new2"), "Should have created ID for new2");

    // Verify words are inserted in the db
    int count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM words WHERE word IN (?, ?)", Integer.class, "new1", "new2");
    assertEquals(2, count, "New words should be inserted in database");
  }

  @Test
  void testGetUnindexedDocuments() {
    String url1 = TEST_URL_PREFIX + "indexed";
    databaseHelper.insertDocument(url1, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);
    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url1);
    int documentId1 = ((Number) results.get(0).get("id")).intValue();
    databaseHelper.updateIndexTime(documentId1);

    // Insert unindexed document
    String url2 = TEST_URL_PREFIX + "unindexed";
    databaseHelper.insertDocument(url2, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Document> unindexedDocs = databaseHelper.getUnindexedDocuments();

    assertFalse(unindexedDocs.isEmpty(), "Should find unindexed documents");
    assertEquals(1, unindexedDocs.size(), "Should find exactly one unindexed document");
    assertEquals(url2, unindexedDocs.get(0).getUrl(), "Should retrieve correct unindexed document");
  }

  @Test
  void testUpdateIndexTime() {
    String url = TEST_URL_PREFIX + "update-index-time";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url);
    int documentId = ((Number) results.get(0).get("id")).intValue();

    // Verify index_time is initially null
    Map<String, Object> beforeUpdate =
        jdbcTemplate.queryForMap("SELECT index_time FROM documents WHERE id = ?", documentId);
    assertNull(beforeUpdate.get("index_time"), "Index time should initially be null");

    // Update index time
    databaseHelper.updateIndexTime(documentId);

    // Verify index_time is now set
    Map<String, Object> afterUpdate =
        jdbcTemplate.queryForMap("SELECT index_time FROM documents WHERE id = ?", documentId);
    assertNotNull(afterUpdate.get("index_time"), "Index time should be updated");
  }

  @Test
  void testUpdateIndexTimeWithSpecificTime() {
    String url = TEST_URL_PREFIX + "update-index-specific-time";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url);
    int documentId = ((Number) results.get(0).get("id")).intValue();

    String specificTime = "2023-01-01 12:00:00";
    databaseHelper.updateIndexTime(documentId, specificTime);

    Map<String, Object> document =
        jdbcTemplate.queryForMap("SELECT index_time FROM documents WHERE id = ?", documentId);
    assertNotNull(document.get("index_time"), "Index time should be set");
    assertTrue(
        document.get("index_time").toString().contains("2023-01-01"),
        "Index time should contain the specified date");
  }

  @Test
  void testGetDocumentWordCount() {
    String url = TEST_URL_PREFIX + "word-count";
    databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    List<Map<String, Object>> results =
        jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", url);
    int documentId = ((Number) results.get(0).get("id")).intValue();

    List<String> words = Arrays.asList("word1", "word2", "word3", "word4", "word5");
    List<Integer> positions = Arrays.asList(0, 1, 2, 3, 4);
    List<Section> sections =
        Arrays.asList(Section.TITLE, Section.HEADER, Section.BODY, Section.BODY, Section.BODY);

    databaseHelper.batchInsertDocumentWords(documentId, words, positions, sections);

    int wordCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_words WHERE document_id = ?", Integer.class, documentId);

    assertEquals(5, wordCount, "Document should have 5 words");

    String emptyUrl = TEST_URL_PREFIX + "empty-doc";
    databaseHelper.insertDocument(
        emptyUrl, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH, TEST_HASH);

    results = jdbcTemplate.queryForList("SELECT id FROM documents WHERE url = ?", emptyUrl);
    int emptyDocId = ((Number) results.get(0).get("id")).intValue();

    int emptyWordCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_words WHERE document_id = ?", Integer.class, emptyDocId);

    assertEquals(0, emptyWordCount, "Empty document should have 0 words");
  }
}
