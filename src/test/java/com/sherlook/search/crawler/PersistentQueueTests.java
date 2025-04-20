package com.sherlook.search.crawler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

class PersistentQueueTests {

  private Path tempFilePath;
  private PersistentQueue queue;

  @BeforeEach
  void setUp() throws IOException {
    tempFilePath = Files.createTempFile("queue", ".txt");
    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    queue = new PersistentQueue(tempFilePath.toFile(), visitedUrls);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFilePath);
  }

  @Test
  void testOfferAddsNewUrl() throws Exception {
    String url = "http://example.com";

    queue.offer(new UrlDepthPair(url, 0));
    UrlDepthPair polled = queue.poll(1, TimeUnit.SECONDS);

    assertEquals(new UrlDepthPair("http://example.com", 0), polled);
  }

  @Test
  void testOfferIgnoresDuplicateUrl() throws Exception {
    String url = "http://example.com";
    queue.offer(new UrlDepthPair(url, 0));
    queue.offer(new UrlDepthPair(url, 0)); // duplicate

    UrlDepthPair polled = queue.poll(1, TimeUnit.SECONDS);
    assertEquals(url, polled);

    UrlDepthPair shouldBeNull = queue.poll(1, TimeUnit.SECONDS);
    assertNull(shouldBeNull);
  }

  @Test
  void testPollMarksUrlAsVisited() throws Exception {
    String url = "http://visited.com";
    queue.offer(new UrlDepthPair(url, 0));
    UrlDepthPair polled = queue.poll(1, TimeUnit.SECONDS);

    assertEquals(url, polled);

    // Read the file manually to verify "V_" line exists
    boolean foundVLine =
        Files.readAllLines(tempFilePath).stream().anyMatch(line -> line.startsWith("V_"));
    assertTrue(foundVLine, "Polled URL should be marked as visited in file");
  }

  @Test
  void testConstructorLoadsUncrawledUrls() throws Exception {
    Files.writeString(tempFilePath, "U_http://example.com 0\nU_http://second.com 1\n");

    PersistentQueue reloaded =
        new PersistentQueue(tempFilePath.toFile(), ConcurrentHashMap.newKeySet());
    UrlDepthPair first = reloaded.poll(1, TimeUnit.SECONDS);
    UrlDepthPair second = reloaded.poll(1, TimeUnit.SECONDS);

    assertEquals(new UrlDepthPair("http://example.com", 0), first);
    assertEquals(new UrlDepthPair("http://second.com", 1), second);
  }

  @Test
  void testOfferIgnoresNullUrl() throws Exception {
    queue.offer(null);
    assertNull(queue.poll(1, TimeUnit.SECONDS));
  }
}
