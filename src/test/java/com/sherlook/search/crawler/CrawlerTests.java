package com.sherlook.search.crawler;

import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CrawlerTests {

  private PersistentQueue mockQueue;
  private HtmlSaver mockSaver;
  private DatabaseHelper mockDatabase;
  private Crawler crawler;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    mockQueue = mock(PersistentQueue.class);
    mockSaver = mock(HtmlSaver.class);
    mockDatabase = mock(DatabaseHelper.class);
  }

  @Test
  void testStartReadsStartPagesAndQueuesThemIfQueueIsEmpty() throws Exception {
    // Prepare temp files
    Path startPagesFile = tempDir.resolve("start-pages.txt");
    Path saveDir = tempDir.resolve("save-dir");
    Path queueFile = tempDir.resolve("urlQueue.txt");

    // Write fake start URLs
    Files.writeString(startPagesFile, "http://example.com\nhttp://example.org\n");

    // Mock the queue's behavior
    when(mockQueue.isIntiallyEmpty()).thenReturn(true);

    // Inject dependencies
    crawler = new Crawler(mockDatabase, mockQueue, mockSaver);
    setField(crawler, "threads", 1);
    setField(crawler, "maxPages", 10);
    setField(crawler, "startPagesPath", startPagesFile.toString());
    setField(crawler, "saveDirPath", saveDir.toString());
    setField(crawler, "urlQueueFilePath", queueFile.toString());

    // Don't test real multithreading behavior — let thread execute no-op
    crawler.start();

    // ✅ Verify that both URLs were queued
    verify(mockQueue).offer("http://example.com");
    verify(mockQueue).offer("http://example.org");
  }

  @Test
  void testStartSkipsQueueingIfQueueIsNotEmpty() {
    when(mockQueue.isIntiallyEmpty()).thenReturn(false);

    crawler = new Crawler(mockDatabase, mockQueue, mockSaver);
    setField(crawler, "threads", 1);
    setField(crawler, "maxPages", 5);
    setField(crawler, "startPagesPath", "does-not-matter.txt");
    setField(crawler, "saveDirPath", tempDir.resolve("save").toString());
    setField(crawler, "urlQueueFilePath", tempDir.resolve("queue.txt").toString());

    crawler.start();

    // ✅ No interactions with file reading
    verify(mockQueue, never()).offer(anyString());
  }

  @Test
  void testStartDoesNothingIfNotInitialized() {
    crawler = new Crawler(mockDatabase);
    setField(crawler, "threads", 1);
    setField(crawler, "maxPages", 5);
    crawler.start(); // htmlSaver and urlQueue are not initialized
    // No exception should be thrown, and the test should exit quietly
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
