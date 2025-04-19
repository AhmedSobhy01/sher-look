package com.sherlook.search.crawler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

class PersistentQueueTests {

  private Path tempFilePath;
  private PersistentQueue queue;

  @BeforeEach
  void setUp() throws IOException {
    tempFilePath = Files.createTempFile("queue", ".txt");
    queue = new PersistentQueue(tempFilePath.toFile());
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFilePath);
  }

  @Test
  void testOfferAddsNewUrl() throws Exception {
    String url = "http://example.com";

    queue.offer(url);
    String polled = queue.poll(1, TimeUnit.SECONDS);

    assertEquals("http://example.com", polled);
  }

  @Test
  void testOfferIgnoresDuplicateUrl() throws Exception {
    String url = "http://example.com";
    queue.offer(url);
    queue.offer(url); // duplicate

    String polled = queue.poll(1, TimeUnit.SECONDS);
    assertEquals(url, polled);

    String shouldBeNull = queue.poll(1, TimeUnit.SECONDS);
    assertNull(shouldBeNull);
  }

  @Test
  void testPollMarksUrlAsVisited() throws Exception {
    String url = "http://visited.com";
    queue.offer(url);
    String polled = queue.poll(1, TimeUnit.SECONDS);

    assertEquals(url, polled);

    // Read the file manually to verify "V_" line exists
    boolean foundVLine =
        Files.readAllLines(tempFilePath).stream().anyMatch(line -> line.startsWith("V_"));
    assertTrue(foundVLine, "Polled URL should be marked as visited in file");
  }

  @Test
  void testConstructorLoadsUncrawledUrls() throws Exception {
    Files.writeString(tempFilePath, "U_http://example.com\nU_http://second.com\n");

    PersistentQueue reloaded = new PersistentQueue(tempFilePath.toFile());
    String first = reloaded.poll(1, TimeUnit.SECONDS);
    String second = reloaded.poll(1, TimeUnit.SECONDS);

    assertEquals("http://example.com", first);
    assertEquals("http://second.com", second);
  }

  @Test
  void testOfferIgnoresNullUrl() throws Exception {
    queue.offer(null);
    assertNull(queue.poll(1, TimeUnit.SECONDS));
  }
}
