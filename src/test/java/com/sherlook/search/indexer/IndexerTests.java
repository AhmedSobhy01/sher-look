package com.sherlook.search.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@ExtendWith(MockitoExtension.class)
public class IndexerTests {

  @Mock private DatabaseHelper databaseHelper;
  @Mock private PlatformTransactionManager txManager;
  @Mock private TransactionStatus txStatus;
  @Mock private Tokenizer tokenizer;

  private Indexer indexer;

  @TempDir Path tempDir;

  private Document createTestDocument(int id, String filePath) {
    return new Document(id, null, null, null, filePath, null);
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
    indexer = new Indexer(databaseHelper, txManager, tokenizer);
  }

  @Test
  void testIndexDocument_WithValidHtmlFile_ShouldExtractMetadataAndBatchInsert()
      throws IOException {
    when(txManager.getTransaction(any(DefaultTransactionDefinition.class))).thenReturn(txStatus);

    when(tokenizer.tokenizeWithPositions(anyString(), anyInt(), any(), any(), any(), any()))
        .thenAnswer(
            (Answer<Integer>)
                invocation -> {
                  String text = invocation.getArgument(0);
                  int startPos = invocation.getArgument(1);
                  List<String> tokens = invocation.getArgument(2);
                  List<Integer> positions = invocation.getArgument(3);
                  List<Section> sections = invocation.getArgument(4);
                  Section section = invocation.getArgument(5);

                  String[] words = text.toLowerCase().split("\\W+");
                  int pos = startPos;

                  for (String word : words) {
                    if (!word.isEmpty()) {
                      tokens.add(word);
                      positions.add(pos++);
                      sections.add(section);
                    }
                  }

                  return pos;
                });

    String html =
        "<!DOCTYPE html><html><head>"
            + "<title>Test Title</title>"
            + "<meta name=\"description\" content=\"Test Description\">"
            + "</head><body>"
            + "<p>This is a test document with some content.</p>"
            + "</body></html>";

    File htmlFile = createHtmlFile("test.html", html);
    Document doc = createTestDocument(123, htmlFile.getAbsolutePath());

    indexer.indexDocument(doc);

    verify(txManager).getTransaction(any(DefaultTransactionDefinition.class));
    verify(txManager).commit(eq(txStatus));

    verify(databaseHelper)
        .updateDocumentMetadata(eq(123), eq("Test Title"), eq("Test Description"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> wordsCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Integer>> posCaptor = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Section>> secCaptor = ArgumentCaptor.forClass(List.class);

    verify(databaseHelper, atLeastOnce())
        .batchInsertDocumentWords(
            eq(123), wordsCaptor.capture(), posCaptor.capture(), secCaptor.capture());

    List<String> tokens = wordsCaptor.getValue();
    assertTrue(tokens.contains("test"));
    assertTrue(tokens.contains("document"));
    assertTrue(tokens.contains("content"));

    List<Section> secs = secCaptor.getValue();
    assertTrue(secs.contains(Section.TITLE));
    assertTrue(secs.contains(Section.BODY));

    verify(databaseHelper).updateIndexTime(eq(123));
  }

  @Test
  void testLoadUnindexedDocuments_WithValidData_ShouldReturnQueueOfDocuments() throws SQLException {
    Document d1 = createTestDocument(1, "/d1.html");
    Document d2 = createTestDocument(2, "/d2.html");
    when(databaseHelper.getUnindexedDocuments()).thenReturn(List.of(d1, d2));

    Queue<Document> result = indexer.loadUnindexedDocuments();

    assertEquals(2, result.size());
    assertEquals(1, result.poll().getId());
    assertEquals(2, result.poll().getId());
    verify(databaseHelper).getUnindexedDocuments();
  }

  @Test
  void testIndex_WithNonexistentFile_ShouldRollbackAndContinue() throws SQLException {
    when(txManager.getTransaction(any(DefaultTransactionDefinition.class))).thenReturn(txStatus);

    Document badDoc = createTestDocument(1, "/no-such-file.html");
    when(databaseHelper.getUnindexedDocuments()).thenReturn(List.of(badDoc));

    indexer.index();

    verify(txManager).getTransaction(any(DefaultTransactionDefinition.class));
    verify(txManager).rollback(eq(txStatus));
    verify(databaseHelper).getUnindexedDocuments();
  }
}
