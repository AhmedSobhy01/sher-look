package com.sherlook.search.utils;

import com.sherlook.search.indexer.Document;
import com.sherlook.search.indexer.DocumentWord;
import com.sherlook.search.indexer.Section;
import com.sherlook.search.indexer.Word;
import com.sherlook.search.ranker.DocumentTerm;
import com.sherlook.search.ranker.DocumentTerm.DocumentTermBuilder;
import com.sherlook.search.ranker.Link;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
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
  public void insertDocument(
      String url, String title, String description, String filePath, String hash) {
    String sql =
        """
        INSERT INTO documents (url, title, description, file_path, document_hash, crawl_time)
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    jdbcTemplate.update(sql, url, title, description, filePath, hash);
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

  @Transactional
  public boolean isHashExsists(String hash) {
    String sql = "SELECT COUNT(*) FROM documents WHERE document_hash = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, hash);
    return count != null && count > 0;
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

  public Map<String, Integer> getOrCreateWordIds(List<String> words, List<String> stems) {
    Map<String, Integer> wordIds = new HashMap<>();
    if (words.isEmpty() || words.size() != stems.size()) return wordIds;

    Set<String> uniqueWords = new HashSet<>(words);

    // Count of each word
    Map<String, Integer> wordCounts = new HashMap<>();
    Map<String, String> wordToStem = new HashMap<>();
    for (int i = 0; i < words.size(); i++) {
      String word = words.get(i);
      String stem = stems.get(i);
      wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
      wordToStem.put(word, stem);
    }

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
      for (String word : uniqueWords)
        newWords.add(new Object[] {word, wordToStem.get(word), wordCounts.get(word)});

      jdbcTemplate.batchUpdate("INSERT INTO words (word, stem, count) VALUES (?, ?, ?)", newWords);

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

  public Map<String, Integer> getOrCreateWordIds(List<String> words) {
    List<String> stems = words.stream().map(String::toLowerCase).collect(Collectors.toList());
    return getOrCreateWordIds(words, stems);
  }

  public void batchInsertDocumentWords(
      int documentId,
      List<String> words,
      List<String> stems,
      List<Integer> positions,
      List<Section> sections) {
    if (words.isEmpty()
        || words.size() != positions.size()
        || words.size() != sections.size()
        || words.size() != stems.size()) return;

    Map<String, Integer> wordIds = getOrCreateWordIds(words, stems);

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
    if (queryTerms == null || queryTerms.isEmpty()) {
      return Collections.emptyList();
    }
    long totalStartTime = System.currentTimeMillis();
    System.out.println("\n--- Starting getDocumentTerms (Final Optimized Plan) ---");
    System.out.println("Query Terms: " + queryTerms);

    // FTS pre-filter for candidates
    long stepStartTime = System.currentTimeMillis();
    String ftsQuery = String.join(" OR ", queryTerms);
    String ftsCandidateSql = "SELECT rowid FROM documents_fts WHERE documents_fts MATCH ?";
    List<Integer> candidateDocIds =
        jdbcTemplate.queryForList(ftsCandidateSql, new Object[] {ftsQuery}, Integer.class);
    long stepEndTime = System.currentTimeMillis();
    System.out.println(
        String.format(
            "1. FTS Query ('%s'): %d ms [Found %d docs]",
            ftsQuery, (stepEndTime - stepStartTime), candidateDocIds.size()));

    if (candidateDocIds.isEmpty()) {
      return Collections.emptyList();
    }

    // get word ids
    stepStartTime = System.currentTimeMillis();
    Map<Integer, String> wordIdToWord = new HashMap<>();
    String wordPlaceholders =
        "(" + String.join(",", Collections.nCopies(queryTerms.size(), "?")) + ")";
    String wordIdSql = "SELECT id, word FROM words WHERE word IN " + wordPlaceholders;
    Object[] queryParams = queryTerms.toArray();
    try {
      jdbcTemplate.query(
          wordIdSql,
          queryParams,
          rs -> {
            wordIdToWord.put(rs.getInt("id"), rs.getString("word"));
          });
    } catch (Exception e) {
      System.err.println("Error executing wordIdSql query: " + e.getMessage());
      e.printStackTrace();
    }
    stepEndTime = System.currentTimeMillis();
    System.out.println(
        String.format(
            "2. Word ID Lookup: %d ms [Found %d words]",
            (stepEndTime - stepStartTime), wordIdToWord.size()));

    if (wordIdToWord.isEmpty()) {
      return Collections.emptyList();
    }

    // temp table to eliminate the large IN (...docIds...)
    stepStartTime = System.currentTimeMillis();
    jdbcTemplate.execute("CREATE TEMP TABLE temp_candidate_ids (id INTEGER PRIMARY KEY)");
    stepEndTime = System.currentTimeMillis();
    System.out.println(
        String.format("3a. Temp Table Creation: %d ms", (stepEndTime - stepStartTime)));

    List<DocumentTerm> result;
    try {
      stepStartTime = System.currentTimeMillis();
      jdbcTemplate.batchUpdate(
          "INSERT INTO temp_candidate_ids (id) VALUES (?)",
          new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
              ps.setInt(1, candidateDocIds.get(i));
            }

            @Override
            public int getBatchSize() {
              return candidateDocIds.size();
            }
          });
      stepEndTime = System.currentTimeMillis();
      System.out.println(
          String.format(
              "3b. Temp Table Population (%d IDs): %d ms",
              candidateDocIds.size(), (stepEndTime - stepStartTime)));

      stepStartTime = System.currentTimeMillis();
      String idPlaceholders =
          "(" + String.join(",", Collections.nCopies(wordIdToWord.size(), "?")) + ")";
      String sql =
          "SELECT d.id AS document_id, d.url, d.title, dw.word_id, dw.section, "
              + "d.document_size, d.description, GROUP_CONCAT(dw.position) AS positions "
              + "FROM document_words dw "
              + "JOIN documents d ON dw.document_id = d.id "
              + "JOIN temp_candidate_ids t ON dw.document_id = t.id "
              + "WHERE dw.word_id IN "
              + idPlaceholders
              + " "
              + "GROUP BY d.id, dw.word_id, dw.section";

      Map<Pair<Integer, Integer>, DocumentTermBuilder> builders = new HashMap<>();
      jdbcTemplate.query(
          sql,
          ps -> {
            int i = 1;
            for (Integer wordId : wordIdToWord.keySet()) {
              ps.setInt(i++, wordId);
            }
          },
          (ResultSetExtractor<Void>)
              rs -> {
                while (rs.next()) {
                  int docId = rs.getInt("document_id");
                  int wordId = rs.getInt("word_id");
                  String word = wordIdToWord.get(wordId);
                  String section = rs.getString("section");
                  String positionsStr = rs.getString("positions");
                  List<Integer> positions = new ArrayList<>();
                  if (positionsStr != null && !positionsStr.isEmpty()) {
                    positions =
                        Arrays.stream(positionsStr.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                  }
                  Pair<Integer, Integer> key = Pair.of(docId, wordId);
                  DocumentTermBuilder builder =
                      builders.computeIfAbsent(
                          key,
                          k -> {
                            try {
                              return new DocumentTermBuilder(
                                  word,
                                  docId,
                                  rs.getString("url"),
                                  rs.getString("title"),
                                  rs.getInt("document_size"),
                                  rs.getString("description"));
                            } catch (SQLException e) {
                              throw new RuntimeException(e);
                            }
                          });
                  builder.addPositions(section, positions);
                }
                return null;
              });
      stepEndTime = System.currentTimeMillis();
      System.out.println(
          String.format("4. Main Relational Query : %d ms", (stepEndTime - stepStartTime)));

      stepStartTime = System.currentTimeMillis();
      result =
          builders.values().stream().map(DocumentTermBuilder::build).collect(Collectors.toList());
      stepEndTime = System.currentTimeMillis();
      System.out.println(
          String.format("5. Final Java Object Assembly: %d ms", (stepEndTime - stepStartTime)));

    } finally {
      // cleanup temp table
      stepStartTime = System.currentTimeMillis();
      jdbcTemplate.execute("DROP TABLE IF EXISTS temp_candidate_ids");
      stepEndTime = System.currentTimeMillis();
      System.out.println(
          String.format("6. Temp Table Cleanup: %d ms", (stepEndTime - stepStartTime)));
    }

    long totalEndTime = System.currentTimeMillis();
    System.out.println(
        String.format("--- Total function time: %d ms ---", (totalEndTime - totalStartTime)));
    return result;
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
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents", Integer.class);
    return count != null ? count : 0;
  }

  public List<Link> getLinks() {
    List<Link> links = new ArrayList<>();
    String sql =
        "SELECT l.source_document_id, d.id as target_document_id"
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
    int batchSize = 500;

    List<Map.Entry<Integer, Double>> entries = new ArrayList<>(pageRank.entrySet());
    for (int i = 0; i < entries.size(); i += batchSize) {
      int endIndex = Math.min(i + batchSize, entries.size());
      List<Map.Entry<Integer, Double>> batch = entries.subList(i, endIndex);

      jdbcTemplate.batchUpdate(
          sql,
          batch,
          batch.size(),
          (ps, entry) -> {
            ps.setDouble(1, entry.getValue());
            ps.setInt(2, entry.getKey());
          });

      // Small delay between batches
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public Map<Integer, Double> getPageRank(List<Integer> docIds) {
    if (docIds.isEmpty()) {
      return Collections.emptyMap();
    }

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
        (ResultSetExtractor<Void>)
            rs -> {
              while (rs.next()) {
                int documentId = rs.getInt("id");
                double pageRank = rs.getDouble("page_rank");
                pageRankMap.put(documentId, pageRank);
              }
              return null;
            });

    return pageRankMap;
  }

  public void updateDocumentSize(int documentId, int size) {
    String sql = "UPDATE documents SET document_size = ? WHERE id = ?";
    jdbcTemplate.update(sql, size, documentId);
  }

  @Transactional
  public void calculateIDF() {
    int totalDocCount = getTotalDocumentCount();
    if (totalDocCount == 0) return;

    String sql =
        "SELECT w.id, COUNT(DISTINCT dw.document_id) as doc_frequency "
            + "FROM words w "
            + "JOIN document_words dw ON w.id = dw.word_id "
            + "GROUP BY w.id";

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    jdbcTemplate.batchUpdate(
        "UPDATE words SET idf = ? WHERE id = ?",
        rows,
        rows.size(),
        (ps, row) -> {
          int docFrequency = ((Number) row.get("doc_frequency")).intValue();
          double idf = Math.log((double) totalDocCount / docFrequency + 1);
          ps.setDouble(1, idf);
          ps.setInt(2, ((Number) row.get("id")).intValue());
        });
  }

  public Map<String, Double> getIDF(List<String> queryTerms) {
    String sql =
        "SELECT w.word, w.idf FROM words w WHERE w.word IN ("
            + String.join(",", Collections.nCopies(queryTerms.size(), "?"))
            + ")";
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, queryTerms.toArray());
    Map<String, Double> idfMap = new HashMap<>();
    for (Map<String, Object> row : rows) {
      String word = (String) row.get("word");
      Double idf = ((Number) row.get("idf")).doubleValue();
      idfMap.put(word, idf);
    }
    return idfMap;
  }

  public void batchInsertDocumentWords(
      int documentId, List<String> words, List<Integer> positions, List<Section> sections) {
    List<String> stems = words.stream().map(String::toLowerCase).collect(Collectors.toList());

    batchInsertDocumentWords(documentId, words, stems, positions, sections);
  }

  public Map<Integer, Map<Integer, String>> getWordsAroundPositions(
      Map<Integer, List<Integer>> docPositions, int windowSize) {
    if (docPositions.isEmpty()) {
      return Collections.emptyMap();
    }

    // Build dynamic query with parameters
    StringBuilder sql =
        new StringBuilder(
            "SELECT dw.document_id, dw.position, w.word FROM document_words dw "
                + "JOIN words w ON dw.word_id = w.id WHERE ");

    List<Object> params = new ArrayList<>();
    boolean first = true;

    for (Map.Entry<Integer, List<Integer>> entry : docPositions.entrySet()) {
      int docId = entry.getKey();
      for (Integer position : entry.getValue()) {
        if (!first) {
          sql.append(" OR ");
        }
        first = false;

        sql.append("(dw.document_id = ? AND dw.position BETWEEN ? AND ?)");
        params.add(docId);
        params.add(Math.max(0, position - windowSize));
        params.add(position + windowSize);
      }
    }

    Map<Integer, Map<Integer, String>> result = new HashMap<>();

    this.jdbcTemplate.query(
        sql.toString(),
        ps -> {
          for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
          }
        },
        (ResultSetExtractor<Void>)
            rs -> {
              while (rs.next()) {
                int docId = rs.getInt("document_id");
                int position = rs.getInt("position");
                String word = rs.getString("word");

                result.computeIfAbsent(docId, k -> new HashMap<>()).put(position, word);
              }
              return null;
            });

    return result;
  }

  @Transactional
  public void updateFTSEntry(int documentId, String ftsContent) {
    String insertSql = "INSERT INTO documents_fts(rowid, content) VALUES(?, ?)";
    jdbcTemplate.update(insertSql, documentId, ftsContent);
  }
}
