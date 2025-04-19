package com.sherlook.search.crawler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CrawlTaskTests {

  private PersistentQueue queue;
  private DatabaseHelper db;
  private HtmlSaver saver;

  @BeforeEach
  void setUp() {
    queue = mock(PersistentQueue.class);
    db = mock(DatabaseHelper.class);
    saver = mock(HtmlSaver.class);
  }

  @Test
  void testSkipsIfAlreadyVisitedOrInDb() throws Exception {
    Set<String> visited = ConcurrentHashMap.newKeySet();
    visited.add("http://example.com");

    when(queue.poll(10, TimeUnit.SECONDS)).thenReturn("http://example.com").thenReturn(null);
    when(db.isUrlCrawled("http://example.com")).thenReturn(true);

    CrawlTask task = new CrawlTask(queue, visited, 5, db, saver);
    task.run();

    verify(db, never()).insertDocument(any(), any(), any(), any());
    verify(saver, never()).save(any(), any());
  }

  @Test
  void testSkipsIfDisallowedByRobots() throws Exception {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    when(queue.poll(10, TimeUnit.SECONDS)).thenReturn("http://example.com").thenReturn(null);
    when(db.isUrlCrawled("http://example.com")).thenReturn(false);

    try (MockedStatic<Robots> robotsMock = mockStatic(Robots.class)) {
      robotsMock.when(() -> Robots.isAllowed("http://example.com")).thenReturn(false);

      CrawlTask task = new CrawlTask(queue, visited, 5, db, saver);
      task.run();
    }

    verify(saver, never()).save(any(), any());
    verify(db, never()).insertDocument(any(), any(), any(), any());
  }

  @Test
  void testProcessesPageSuccessfully() throws Exception {
    Set<String> visited = ConcurrentHashMap.newKeySet();
    Path examplePath = Paths.get("example.html");

    // Arrange document and mocks
    Document doc = mock(Document.class);
    when(doc.title()).thenReturn("Example Title");
    when(doc.select("meta[name=description]")).thenReturn(mock(Elements.class));
    when(doc.select("a[href]")).thenReturn(new Elements());

    when(queue.poll(10, TimeUnit.SECONDS)).thenReturn("http://example.com").thenReturn(null);
    when(db.isUrlCrawled("http://example.com")).thenReturn(false);
    when(saver.getFilePath("http://example.com")).thenReturn(examplePath);

    // Static mocks must be closed after use
    try (MockedStatic<Robots> robotsMock = mockStatic(Robots.class);
        MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      // Stub Robots
      robotsMock.when(() -> Robots.isAllowed("http://example.com")).thenReturn(true);

      // Stub Jsoup connection
      Connection connection = mock(Connection.class);
      when(connection.get()).thenReturn(doc);
      Connection.Response response = mock(Connection.Response.class);
      when(response.statusCode()).thenReturn(200);
      when(connection.response()).thenReturn(response);
      jsoupMock.when(() -> Jsoup.connect("http://example.com")).thenReturn(connection);

      CrawlTask task = new CrawlTask(queue, visited, 5, db, saver);
      task.run();
    }

    verify(saver).save(eq("http://example.com"), any());
    verify(db)
        .insertDocument(
            eq("http://example.com"), eq("Example Title"), any(), eq(examplePath.toString()));
  }

  @Test
  void testHandlesTimeoutGracefully() throws Exception {
    // arrange
    Set<String> visited = ConcurrentHashMap.newKeySet();
    when(queue.poll(10, TimeUnit.SECONDS)).thenReturn("http://example.com").thenReturn(null);
    when(db.isUrlCrawled("http://example.com")).thenReturn(false);

    // stub Jsoup to throw timeout
    org.jsoup.Connection mockConn = mock(org.jsoup.Connection.class);
    when(mockConn.get()).thenThrow(new SocketTimeoutException("timeout"));

    // *both* static mocks in try-with-resources*
    try (MockedStatic<Robots> robotsMock = mockStatic(Robots.class);
        MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      robotsMock.when(() -> Robots.isAllowed("http://example.com")).thenReturn(true);

      jsoupMock.when(() -> Jsoup.connect("http://example.com")).thenReturn(mockConn);

      CrawlTask task = new CrawlTask(queue, visited, 5, db, saver);
      task.run();
    }

    // assert it just returned cleanly:
    verify(saver, never()).save(any(), any());
    verify(db, never()).insertDocument(any(), any(), any(), any());
  }
}
