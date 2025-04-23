package com.sherlook.search.crawler;

import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;
import com.sherlook.search.utils.Hash;
import com.sherlook.search.utils.UrlNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.transaction.annotation.Transactional;

public class CrawlTask implements Runnable {
  PersistentQueue urlQueue;
  private int maxPages;
  private DatabaseHelper databaseHelper;
  private HtmlSaver htmlSaver;
  private Set<String> visitedUrls;
  private final int maxDepth;
  private final int threadId;
  private Set<String> visitedUrlsHashes;

  public CrawlTask(
      PersistentQueue urlQueue,
      Set<String> visitedUrls,
      int maxPages,
      DatabaseHelper databaseHelper,
      HtmlSaver htmlSaver,
      int maxDepth,
      int threadId) {
    this.urlQueue = urlQueue;
    this.maxPages = maxPages;
    this.databaseHelper = databaseHelper;
    this.htmlSaver = htmlSaver;
    this.visitedUrls = visitedUrls;
    this.maxDepth = maxDepth;
    this.threadId = threadId;
    this.visitedUrlsHashes = ConcurrentHashMap.newKeySet();
  }

  public void run() {
    boolean running = true;
    while (running) {
      int crawledPages = databaseHelper.getCrawledPagesCount();
      if (crawledPages >= maxPages) {
        ConsoleColors.printSuccess("CrawlerTask " + threadId);
        System.out.println("Max pages crawled. Stopping.");
        break;
      }
      running = crawl("CrawlerTask " + threadId);
    }
  }

  private boolean crawl(String crawlTaskString) {
    try {
      UrlDepthPair urlToCrawlPair = urlQueue.poll(10, TimeUnit.SECONDS);
      if (urlToCrawlPair == null) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("No URLs to crawl. Exiting.");
        return false;
      }

      // Normalize the URL
      String url = urlToCrawlPair.getUrl();
      String urlToCrawl = UrlNormalizer.normalize(url);

      urlToCrawl = UrlNormalizer.normalize(urlToCrawl);
      if (urlToCrawl == null) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Invalid URL: " + urlToCrawl);
        return true;
      }

      // Check if the URL is already crawled
      // Check in memory first
      if (!visitedUrls.add(urlToCrawl)) {
        ConsoleColors.printInfo(crawlTaskString);
        System.out.println("URL already crawled: " + urlToCrawl);
        return true;
      }

      // Check in the database
      if (databaseHelper.isUrlCrawled(urlToCrawl)) {
        ConsoleColors.printInfo(crawlTaskString);
        System.out.println("URL already crawled: " + urlToCrawl);
        return true;
      }

      // Check if the URL is allowed to be crawled
      if (!Robots.isAllowed(urlToCrawl)) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Crawling not allowed by robots.txt: " + urlToCrawl);
        return true;
      }

      ConsoleColors.printInfo(crawlTaskString);
      System.out.println("Crawling URL: " + urlToCrawl);
      Connection conn = Jsoup.connect(urlToCrawl);
      Document doc = conn.get();
      if (conn.response().statusCode() == 200) {
        ConsoleColors.printSuccess(crawlTaskString);
        System.out.println("Page title: " + doc.title());
      } else {
        System.out.println("Failed to fetch page. Status code: " + conn.response().statusCode());
      }

      // Check if the document already exists
      String hash = Hash.sha256(doc.html());

      if (!visitedUrlsHashes.add(hash)) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Document already crawled: " + urlToCrawl);
        return true;
      }

      if (databaseHelper.isHashExsists(hash)) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Document already crawled: " + urlToCrawl);
        return true;
      }

      List<String> links = new ArrayList<>();

      for (Element link : doc.select("a[href]")) {
        String absUrl = link.absUrl("href");
        absUrl = UrlNormalizer.normalize(absUrl);
        if (absUrl != null
            && UrlNormalizer.isAbsolute(absUrl)
            && urlToCrawlPair.getDepth() < maxDepth) {
          boolean newLink = urlQueue.offer(new UrlDepthPair(absUrl, urlToCrawlPair.getDepth() + 1));
          if (newLink) {
            links.add(absUrl);
          }
        }
      }

      // Save the html page to file system
      htmlSaver.save(urlToCrawl, doc.html());

      // Save the crawled page to the database
      String title = doc.title();
      String description = doc.select("meta[name=description]").attr("content");
      List<String> uniqueChildrens = links.stream().distinct().toList();
      saveDocumentWithLinks(urlToCrawl, title, description, hash, uniqueChildrens);
      ConsoleColors.printSuccess(crawlTaskString);
      System.out.println("Saved page to database: " + urlToCrawl);

      Thread.sleep(1000); // Delay for one second between crawls

      return true;

    } catch (Exception e) {
      if (e instanceof java.net.SocketTimeoutException) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Socket timeout while crawling URL: " + e.getMessage());
        return true;
      } else if (e instanceof org.jsoup.UnsupportedMimeTypeException) {
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Unsupported MIME type while crawling URL: " + e.getMessage());
        return true;
      } else if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt(); // Good practice to reset interrupt flag
        ConsoleColors.printWarning(crawlTaskString);
        System.out.println("Sleep interrupted");
      }
      ConsoleColors.printError(crawlTaskString);
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      return true;
    }
  }

  @Transactional
  public void saveDocumentWithLinks(
      String urlToCrawl,
      String title,
      String description,
      String hash,
      List<String> uniqueChildrens)
      throws Exception {
    databaseHelper.insertDocument(
        urlToCrawl, title, description, htmlSaver.getFilePath(urlToCrawl).toString(), hash);
    int documentId = databaseHelper.getDocumentId(urlToCrawl);
    if (documentId == -1) {
      throw new Exception("Failed to get document ID for URL: " + urlToCrawl);
    }
    databaseHelper.insertLinks(documentId, uniqueChildrens);
  }
}
