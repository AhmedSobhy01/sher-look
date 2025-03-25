package com.sherlook.search.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sherlook.search.utils.DatabaseHelper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IndexerTests {

  @Mock private DatabaseHelper databaseHelper;

  private Indexer indexer;

  @TempDir Path tempDir;

  private Document createTestDocument(int id, String filePath) {
    Document doc = new Document(id, null, null, null, filePath, null);
    return doc;
  }

  private File createHtmlFile(String fileName, String content) throws IOException {
    File file = tempDir.resolve(fileName).toFile();
    try (FileWriter writer = new FileWriter(file)) {
      writer.write(content);
    }
    return file;
  }

  @BeforeEach
  void setUp() {
    indexer = new Indexer(databaseHelper);
  }

  @Test
  void testIndexDocument_WithValidHtmlFile_ShouldExtractMetadataAndTokens() throws IOException {
    String htmlContent =
        "<!DOCTYPE html><html><head><title>Test Title</title>"
            + "<meta name=\"description\" content=\"Test Description\"></head>"
            + "<body><p>This is a test document with some content.</p></body></html>";

    File htmlFile = createHtmlFile("test.html", htmlContent);
    Document document = createTestDocument(123, htmlFile.getAbsolutePath());

    indexer.indexDocument(document);

    verify(databaseHelper)
        .updateDocumentMetadata(eq(123), eq("Test Title"), eq("Test Description"));

    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> positionCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(databaseHelper, atLeastOnce())
        .insertDocumentWord(eq(123), tokenCaptor.capture(), positionCaptor.capture());

    List<String> capturedTokens = tokenCaptor.getAllValues();
    assertTrue(capturedTokens.contains("test"));
    assertTrue(capturedTokens.contains("document"));
    assertTrue(capturedTokens.contains("content"));

    verify(databaseHelper).updateIndexTime(eq(123));
  }

  @Test
  void testLoadUnindexedDocuments_WithValidData_ShouldReturnQueueOfDocuments() throws SQLException {
    Document doc1 = createTestDocument(1, "/doc1.html");
    Document doc2 = createTestDocument(2, "/doc2.html");
    List<Document> mockDocuments = Arrays.asList(doc1, doc2);

    when(databaseHelper.getUnindexedDocuments()).thenReturn(mockDocuments);

    Queue<Document> result = indexer.loadUnindexedDocuments();

    assertEquals(2, result.size());
    assertEquals(1, result.poll().getId());
    assertEquals(2, result.poll().getId());
    verify(databaseHelper).getUnindexedDocuments();
  }

  @Test
  void testIndex_WithNonexistentFile_ShouldHandleGracefully() throws SQLException {
    Document doc1 = createTestDocument(1, "/nonexistent.html");
    List<Document> mockDocuments = Arrays.asList(doc1);

    when(databaseHelper.getUnindexedDocuments()).thenReturn(mockDocuments);

    indexer.index();

    verify(databaseHelper).getUnindexedDocuments();
  }
}
