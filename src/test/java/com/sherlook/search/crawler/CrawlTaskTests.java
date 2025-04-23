package com.sherlook.search.crawler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import com.sherlook.search.utils.Hash;
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

  private PersistentQueue mockQueue;
  private DatabaseHelper mockDatabase;
  private HtmlSaver mockHtmlSaver;

  @BeforeEach
  void setUp() {
    mockQueue = mock(PersistentQueue.class);
    mockDatabase = mock(DatabaseHelper.class);
    mockHtmlSaver = mock(HtmlSaver.class);
  }

  @Test
  void testSkipsIfAlreadyVisitedOrInDb() throws Exception {
    Set<String> visited = ConcurrentHashMap.newKeySet();
    visited.add("http://example.com");

    when(mockQueue.poll(10, TimeUnit.SECONDS))
        .thenReturn(new UrlDepthPair("http://example.com", 0))
        .thenReturn(null);
    when(mockDatabase.isUrlCrawled("http://example.com")).thenReturn(true);

    CrawlTask task = new CrawlTask(mockQueue, visited, 5, mockDatabase, mockHtmlSaver, 5, 0);
    task.run();

    verify(mockDatabase, never()).insertDocument(any(), any(), any(), any(), any());
    verify(mockHtmlSaver, never()).save(any(), any());
  }

  @Test
  void testSkipsIfDisallowedByRobots() throws Exception {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    when(mockQueue.poll(10, TimeUnit.SECONDS))
        .thenReturn(new UrlDepthPair("http://example.com", 0))
        .thenReturn(null);
    when(mockDatabase.isUrlCrawled("http://example.com")).thenReturn(false);

    try (MockedStatic<Robots> robotsMock = mockStatic(Robots.class)) {
      robotsMock.when(() -> Robots.isAllowed("http://example.com")).thenReturn(false);

      CrawlTask task = new CrawlTask(mockQueue, visited, 5, mockDatabase, mockHtmlSaver, 5, 0);
      task.run();
    }

    verify(mockHtmlSaver, never()).save(any(), any());
    verify(mockDatabase, never()).insertDocument(any(), any(), any(), any(), any());
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
    when(doc.html()).thenReturn("<html>example</html>");

    when(mockQueue.poll(10, TimeUnit.SECONDS))
        .thenReturn(new UrlDepthPair("http://example.com", 0))
        .thenReturn(null);
    when(mockDatabase.isUrlCrawled("http://example.com")).thenReturn(false);
    when(mockHtmlSaver.getFilePath("http://example.com")).thenReturn(examplePath);
    when(mockDatabase.isHashExsists("http://example.com")).thenReturn(false);

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
      CrawlTask task = new CrawlTask(mockQueue, visited, 5, mockDatabase, mockHtmlSaver, 5, 0);
      task.run();
    }

    verify(mockHtmlSaver).save(eq("http://example.com"), any());
    verify(mockDatabase)
        .insertDocument(
            eq("http://example.com"),
            eq("Example Title"),
            any(),
            eq(examplePath.toString()),
            eq(Hash.sha256("<html>example</html>")));
  }

  @Test
  void testHandlesTimeoutGracefully() throws Exception {

    Set<String> visited = ConcurrentHashMap.newKeySet();

    when(mockQueue.poll(10, TimeUnit.SECONDS))
        .thenReturn(new UrlDepthPair("http://example.com", 0))
        .thenReturn(null);
    when(mockDatabase.isUrlCrawled("http://example.com")).thenReturn(false);

    org.jsoup.Connection mockConn = mock(org.jsoup.Connection.class);
    when(mockConn.get()).thenThrow(new SocketTimeoutException("timeout"));

    try (MockedStatic<Robots> robotsMock = mockStatic(Robots.class);
        MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      robotsMock.when(() -> Robots.isAllowed("http://example.com")).thenReturn(true);

      jsoupMock.when(() -> Jsoup.connect("http://example.com")).thenReturn(mockConn);

      CrawlTask task = new CrawlTask(mockQueue, visited, 5, mockDatabase, mockHtmlSaver, 5, 0);
      task.run();
    }

    // assert it just returned cleanly:
    verify(mockHtmlSaver, never()).save(any(), any());
    verify(mockDatabase, never()).insertDocument(any(), any(), any(), any(), any());
  }
}
