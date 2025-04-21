package com.sherlook.search.utils;

import com.sherlook.search.indexer.Document;
import com.sherlook.search.indexer.DocumentWord;
import com.sherlook.search.indexer.Section;
import com.sherlook.search.indexer.Word;
import com.sherlook.search.ranker.RankedDocument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHelper {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public DatabaseHelper(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate.setDataSource(jdbcTemplate.getDataSource());
  }

  public void insertDocument(String url, String title, String description, String filePath) {
    String sql =
        "INSERT INTO documents (url, title, description, file_path, crawl_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
    jdbcTemplate.update(sql, url, title, description, filePath);
  }

  public List<DocumentWord> getDocumentWords() {
    String sql =
        "SELECT d.id AS document_id, d.url, d.title, d.description, d.file_path, d.crawl_time, "
            + "w.id AS word_id, w.word, dw.position, dw.section "
            + "FROM documents d "
            + "JOIN document_words dw ON d.id = dw.document_id "
            + "JOIN words w ON dw.word_id = w.id "
            + "ORDER BY d.id, dw.position";

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Word word = new Word(rs.getInt("word_id"), rs.getString("word"));
          Document document =
              new Document(
                  rs.getInt("document_id"),
                  rs.getString("url"),
                  rs.getString("title"),
                  rs.getString("description"),
                  rs.getString("file_path"),
                  rs.getTimestamp("crawl_time"));

          return new DocumentWord(
              document, word, rs.getInt("position"), Section.fromString(rs.getString("section")));
        });
  }

  public void updateIndexTime(int documentId) {
    String sql = "UPDATE documents SET index_time = CURRENT_TIMESTAMP WHERE id = ?";
    jdbcTemplate.update(sql, documentId);
  }

  public void updateIndexTime(int documentId, String indexTime) {
    String sql = "UPDATE documents SET index_time = ? WHERE id = ?";
    jdbcTemplate.update(sql, indexTime, documentId);
  }

  public List<Document> getUnindexedDocuments() {
    String sql = "SELECT * FROM documents WHERE index_time IS NULL";

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new Document(
                rs.getInt("id"),
                rs.getString("url"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("file_path"),
                rs.getTimestamp("crawl_time")));
  }

  public void insertDocumentWord(int documentId, String word, int position, Section section) {
    String sql = "SELECT id FROM words WHERE word = ?";
    Integer wordId;
    try {
      wordId = jdbcTemplate.queryForObject(sql, Integer.class, word);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      sql = "INSERT INTO words (word) VALUES (?)";
      jdbcTemplate.update(sql, word);
      wordId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
    }

    sql =
        "INSERT INTO document_words (document_id, word_id, position, section) VALUES (?, ?, ?, ?)";
    jdbcTemplate.update(sql, documentId, wordId, position, section.toString());
  }

  public void updateDocumentMetadata(int documentId, String title, String description) {
    String sql = "UPDATE documents SET title = ?, description = ? WHERE id = ?";
    jdbcTemplate.update(sql, title, description, documentId);
  }

  public int getCrawledPagesCount() {
    String sql = "SELECT COUNT(*) FROM documents";
    Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
    return result != null ? result : 0;
  }

  public boolean isUrlCrawled(String url) {
    String sql = "SELECT COUNT(*) FROM documents WHERE url = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, url);
    return count != null && count > 0;
  }

  /**
   * Computes tf_idf score for documents for a given query
   *
   * @param queries
   * @return list of ranked documents according to their tf_idf scores
   */
  public List<RankedDocument> getDocumentRelevance(List<String> queries) {
    // I decided on log10 after some testing
    String sqltemp =
        """
        WITH tf AS (
            SELECT
                d.url,
                d.title,
                dw.document_id AS docId,
                w.word,
                (CAST((SELECT COUNT(*) FROM document_words dw2 WHERE dw2.document_id = dw.document_id AND dw2.word_id = dw.word_id) AS FLOAT) /
                 (SELECT COUNT(*) FROM document_words dw2 WHERE dw2.document_id = dw.document_id)) AS tf
            FROM document_words dw
            JOIN words w ON w.id = dw.word_id
            JOIN documents d ON d.id = dw.document_id
            WHERE w.word IN (%s)
            GROUP BY dw.document_id, w.word, d.url, d.title
        ),
        idf AS (
            SELECT
                w.word,
                LOG10(
                    (SELECT COUNT(*) FROM documents) /
                    ((SELECT COUNT(DISTINCT dw2.document_id) FROM document_words dw2 WHERE dw2.word_id = w.id) + 0.0001)
                ) AS idf
            FROM words w
            WHERE w.word IN (%s)
        )
        SELECT
            SUM(tf.tf * idf.idf) AS tf_idf,
            tf.docId AS document_id,
            tf.url,
            tf.title
        FROM tf
        JOIN idf ON tf.word = idf.word
        GROUP BY tf.docId, tf.url, tf.title
        ORDER BY tf_idf DESC
        """;
    String quotedQueries =
        String.join(",", queries.stream().map(q -> "'" + q + "'").collect(Collectors.joining(",")));
    String sql = String.format(sqltemp, quotedQueries, quotedQueries);
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new RankedDocument(
                rs.getInt("document_id"),
                rs.getString("url"),
                rs.getString("title"),
                rs.getDouble("tf_idf")));
  }

  public Map<String, Integer> getOrCreateWordIds(List<String> words) {
    Map<String, Integer> wordIds = new HashMap<>();
    if (words.isEmpty()) return wordIds;

    Set<String> uniqueWords = new HashSet<>(words);

    // Get existing words
    List<Map<String, Object>> existingWords = Collections.emptyList();
    if (!uniqueWords.isEmpty()) {
      String placeholders = String.join(",", Collections.nCopies(uniqueWords.size(), "?"));
      String selectSql = "SELECT id, word FROM words WHERE word IN (" + placeholders + ")";
      existingWords = jdbcTemplate.queryForList(selectSql, uniqueWords.toArray());
    }

    existingWords.forEach(
        row -> {
          String word = (String) row.get("word");
          Integer id = ((Number) row.get("id")).intValue();
          wordIds.put(word, id);
          uniqueWords.remove(word);
        });

    // Insert new words
    if (!uniqueWords.isEmpty()) {
      List<Object[]> newWords = new ArrayList<>();
      for (String word : uniqueWords) newWords.add(new Object[] {word});

      jdbcTemplate.batchUpdate("INSERT INTO words (word) VALUES (?)", newWords);

      List<Map<String, Object>> insertedWords =
          jdbcTemplate.queryForList(
              "SELECT id, word FROM words WHERE word IN ("
                  + String.join(",", Collections.nCopies(uniqueWords.size(), "?"))
                  + ")",
              uniqueWords.toArray());

      insertedWords.forEach(
          row -> {
            String word = (String) row.get("word");
            Integer id = ((Number) row.get("id")).intValue();
            wordIds.put(word, id);
          });
    }

    return wordIds;
  }

  public void batchInsertDocumentWords(
      int documentId, List<String> words, List<Integer> positions, List<Section> sections) {
    if (words.isEmpty() || words.size() != positions.size() || words.size() != sections.size()) {
      return;
    }

    Map<String, Integer> wordIds = getOrCreateWordIds(words);

    List<Object[]> batch = new ArrayList<>(words.size());
    for (int i = 0; i < words.size(); i++) {
      batch.add(
          new Object[] {
            documentId, wordIds.get(words.get(i)), positions.get(i), sections.get(i).toString()
          });
    }

    jdbcTemplate.batchUpdate(
        "INSERT INTO document_words (document_id, word_id, position, section) VALUES (?, ?, ?, ?)",
        batch);
  }
}
