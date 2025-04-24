package com.sherlook.search.utils;

import com.sherlook.search.indexer.Document;
import com.sherlook.search.indexer.DocumentWord;
import com.sherlook.search.indexer.Section;
import com.sherlook.search.indexer.Word;
import com.sherlook.search.ranker.DocumentTerm;
import com.sherlook.search.ranker.DocumentTerm.DocumentTermBuilder;
import com.sherlook.search.ranker.Link;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional
  public void insertDocument(String url, String title, String description, String filePath) {
    String sql =
        """
        INSERT INTO documents (url, title, description, file_path, crawl_time)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    jdbcTemplate.update(sql, url, title, description, filePath);
  }

  @Transactional
  public void insertLinks(int documentId, List<String> links) {
    String sql = "INSERT INTO links (source_document_id, target_url) VALUES (?, ?)";
    jdbcTemplate.batchUpdate(
        sql,
        links,
        links.size(),
        (ps, link) -> {
          ps.setInt(1, documentId);
          ps.setString(2, link);
        });
  }

  @Transactional
  public int getDocumentId(String url) {
    String sql = "SELECT id FROM documents WHERE url = ?";
    Integer documentId;
    try {
      documentId = jdbcTemplate.queryForObject(sql, Integer.class, url);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      return -1;
    }
    return documentId != null ? documentId : -1;
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
      jdbcTemplate.update("UPDATE words SET count = count + 1 WHERE id = ?", wordId);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      sql = "INSERT INTO words (word, count) VALUES (?, 1)";
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

  public Map<String, Integer> getOrCreateWordIds(List<String> words) {
    Map<String, Integer> wordIds = new HashMap<>();
    if (words.isEmpty()) return wordIds;

    Set<String> uniqueWords = new HashSet<>(words);

    // Count of each word
    Map<String, Integer> wordCounts = new HashMap<>();
    for (String word : words) wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);

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

          if (wordCounts.get(word) > 0)
            jdbcTemplate.update(
                "UPDATE words SET count = count + ? WHERE id = ?", wordCounts.get(word), id);
        });

    // Insert new words
    if (!uniqueWords.isEmpty()) {
      List<Object[]> newWords = new ArrayList<>();
      for (String word : uniqueWords) newWords.add(new Object[] {word, wordCounts.get(word)});

      jdbcTemplate.batchUpdate("INSERT INTO words (word, count) VALUES (?, ?)", newWords);

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

  public List<DocumentTerm> getDocumentTerms(List<String> queryTerms) {
    if (queryTerms.isEmpty()) {
      return Collections.emptyList();
    }

    String placeholders = String.join(",", Collections.nCopies(queryTerms.size(), "?"));
    String sql =
        "SELECT d.id AS document_id, d.url, d.title, w.word, w.id AS word_id, dw.section, "
            + "GROUP_CONCAT(dw.position) AS positions "
            + "FROM document_words dw "
            + "JOIN documents d ON dw.document_id = d.id "
            + "JOIN words w ON dw.word_id = w.id "
            + "WHERE w.word IN ("
            + placeholders
            + ") "
            + "GROUP BY d.id, w.id, dw.section";

    return jdbcTemplate.query(
        sql,
        ps -> {
          for (int i = 0; i < queryTerms.size(); i++) {
            ps.setString(i + 1, queryTerms.get(i));
          }
        },
        rs -> {
          Map<Pair<Integer, Integer>, DocumentTermBuilder> builders =
              new HashMap<>(); // one builder per (docId, wordId)
          List<DocumentTerm> result = new ArrayList<>();

          while (rs.next()) {
            int docId = rs.getInt("document_id");
            int wordId = rs.getInt("word_id");
            String section = rs.getString("section");
            String positionsString = rs.getString("positions");

            Pair<Integer, Integer> key = Pair.of(docId, wordId);

            DocumentTermBuilder builder =
                builders.computeIfAbsent(
                    key,
                    k -> {
                      try {
                        return new DocumentTermBuilder(
                            rs.getString("word"),
                            docId,
                            rs.getString("url"),
                            rs.getString("title"));
                      } catch (SQLException e) {
                        throw new RuntimeException(e);
                      }
                    });

            if (positionsString != null && !positionsString.isEmpty()) {
              List<Integer> positions =
                  Arrays.stream(positionsString.split(","))
                      .map(Integer::parseInt)
                      .collect(Collectors.toList());
              builder.addPositions(section, positions);
              builder.setWordCountInDocument(builder.getWordCountInDocument() + positions.size());
            }
          }

          for (DocumentTermBuilder builder : builders.values()) {
            result.add(builder.build());
          }

          return result;
        });
  }

  public Map<String, Integer> getTermFrequencyAcrossDocuments(List<String> queryTerms) {
    String sql =
        "SELECT w.word, w.count FROM words w WHERE w.word IN ("
            + String.join(",", Collections.nCopies(queryTerms.size(), "?"))
            + ")";
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, queryTerms.toArray());
    Map<String, Integer> wordFrequencies = new HashMap<>();
    for (Map<String, Object> row : rows) {
      String word = (String) row.get("word");
      Integer count = ((Number) row.get("count")).intValue();
      wordFrequencies.put(word, count);
    }
    return wordFrequencies;
  }

  public int getTotalDocumentCount() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents", Integer.class);
  }

  public List<Link> getLinks() {
    List<Link> links = new ArrayList<>();
    String sql =
        "SELECT l.source_document_id, d.document_id as target_document_id"
            + " FROM links l"
            + " JOIN documents d ON l.target_url = d.url";
    jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          int sourceDocumentId = rs.getInt("source_document_id");
          int targetDocumentId = rs.getInt("target_document_id");
          links.add(new Link(sourceDocumentId, targetDocumentId));
          return null;
        });
    return links;
  }

  public List<Integer> getAllDocumentIds() {
    String sql = "SELECT id FROM documents";
    List<Integer> docIds = new ArrayList<>();
    jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          int documentId = rs.getInt("id");
          docIds.add(documentId);
          return null;
        });
    return docIds;
  }

  public void batchUpdatePageRank(Map<Integer, Double> pageRank) {
    String sql = "UPDATE documents SET page_rank = ? WHERE id = ?";
    jdbcTemplate.batchUpdate(
        sql,
        pageRank.entrySet(),
        pageRank.size(),
        (ps, entry) -> {
          ps.setDouble(1, entry.getValue());
          ps.setInt(2, entry.getKey());
        });
  }

  public Map<Integer, Double> getPageRank(List<Integer> docIds) {
    String sql =
        "SELECT id, page_rank FROM documents WHERE id IN ("
            + String.join(",", Collections.nCopies(docIds.size(), "?"))
            + ")";
    Map<Integer, Double> pageRankMap = new HashMap<>();
    jdbcTemplate.query(
        sql,
        ps -> {
          for (int i = 0; i < docIds.size(); i++) {
            ps.setInt(i + 1, docIds.get(i));
          }
        },
        rs -> {
          while (rs.next()) {
            int documentId = rs.getInt("id");
            double pageRank = rs.getDouble("page_rank");
            pageRankMap.put(documentId, pageRank);
          }
        });
    return pageRankMap;
  }
}
