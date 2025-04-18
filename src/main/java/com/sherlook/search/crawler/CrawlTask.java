package com.sherlook.search.crawler;

import com.sherlook.search.utils.DatabaseHelper;
import com.sherlook.search.utils.UrlNormalizer;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CrawlTask implements Runnable {
  PersistentQueue urlQueue;
  private int maxPages;
  private DatabaseHelper databaseHelper;
  private HtmlSaver htmlSaver;
  private Set<String> visitedUrls;

  public CrawlTask(
      PersistentQueue urlQueue,
      Set<String> visitedUrls,
      int maxPages,
      DatabaseHelper databaseHelper,
      HtmlSaver htmlSaver) {
    this.urlQueue = urlQueue;
    this.maxPages = maxPages;
    this.databaseHelper = databaseHelper;
    this.htmlSaver = htmlSaver;
    this.visitedUrls = visitedUrls;
  }

  public void run() {
    boolean running = true;
    if (htmlSaver == null) {
      System.out.println("[CrawlerTask] HtmlSaver is not initialized. Exiting.");
      return;
    }
    while (running) {
      int crawledPages = databaseHelper.getCrawledPagesCount();
      if (crawledPages >= maxPages) {
        System.out.println("[CrawlerTask] Max pages crawled. Stopping.");
        break;
      }
      running = crawl();
    }
  }

  private boolean crawl() {
    try {
      String urlToCrawl = urlQueue.poll(10, TimeUnit.SECONDS);
      if (urlToCrawl == null) {
        System.out.println("[CrawlerTask] No URLs to crawl. Exiting.");
        return false;
      }

      urlToCrawl = UrlNormalizer.normalize(urlToCrawl);
      if (urlToCrawl == null) {
        System.out.println("[CrawlerTask] Invalid URL: " + urlToCrawl);
        return true;
      }

      // Check if the URL is already crawled
      if (!visitedUrls.add(urlToCrawl)) {
        System.out.println("[CrawlerTask] URL already crawled: " + urlToCrawl);
        return true;
      }

      if (databaseHelper.isUrlCrawled(urlToCrawl)) {
        System.out.println("[CrawlerTask] URL already crawled: " + urlToCrawl);
        return true;
      }

      if (!Robots.isAllowed(urlToCrawl)) {
        System.out.println("[CrawlerTask] Crawling not allowed by robots.txt: " + urlToCrawl);
        return true;
      }

      System.out.println("[CrawlerTask] Crawling URL: " + urlToCrawl);
      Connection conn = Jsoup.connect(urlToCrawl);
      Document doc = conn.get();
      if (conn.response().statusCode() == 200) {
        System.out.println("[CrawlerTask] Page title: " + doc.title());
      } else {
        System.out.println(
            "[CrawlerTask] Failed to fetch page. Status code: " + conn.response().statusCode());
      }

      for (Element link : doc.select("a[href]")) {
        String absUrl = link.absUrl("href");
        absUrl = UrlNormalizer.normalize(absUrl);
        if (absUrl != null && UrlNormalizer.isAbsolute(absUrl)) {
          urlQueue.offer(absUrl);
        }
      }

      // save the html page to file system
      htmlSaver.save(urlToCrawl, doc.html());

      // Save the crawled page to the database
      String title = doc.title();
      String description = doc.select("meta[name=description]").attr("content");
      databaseHelper.insertDocument(
          urlToCrawl, title, description, htmlSaver.getFilePath(urlToCrawl).toString());
      System.out.println("[CrawlerTask] Saved page to database: " + urlToCrawl);
      return true;

    } catch (Exception e) {
      if (e instanceof java.net.SocketTimeoutException) {
        System.out.println("[CrawlerTask] Socket timeout while crawling URL: " + e.getMessage());
        return true;
      }
      System.err.println("[CrawlerTask] Error: " + e.getMessage());
      return true;
    }
  }
}
