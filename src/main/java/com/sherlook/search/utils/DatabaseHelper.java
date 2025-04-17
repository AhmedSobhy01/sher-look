package com.sherlook.search.utils;

import com.sherlook.search.indexer.Document;
import com.sherlook.search.indexer.DocumentWord;
import com.sherlook.search.indexer.Word;
import java.util.List;
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
            + "w.id AS word_id, w.word, dw.position "
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

          return new DocumentWord(document, word, rs.getInt("position"));
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

  public void insertDocumentWord(int documentId, String word, int position) {
    String sql = "SELECT id FROM words WHERE word = ?";
    Integer wordId;
    try {
      wordId = jdbcTemplate.queryForObject(sql, Integer.class, word);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      sql = "INSERT INTO words (word) VALUES (?)";
      jdbcTemplate.update(sql, word);
      wordId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Integer.class);
    }

    sql = "INSERT INTO document_words (document_id, word_id, position) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql, documentId, wordId, position);
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
  
}
