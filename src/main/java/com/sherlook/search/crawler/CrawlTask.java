package com.sherlook.search.crawler;

import com.sherlook.search.utils.ConsoleColors;
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
      ConsoleColors.printError("CrawlerTask");
      System.err.println("HtmlSaver is not initialized. Exiting.");
      return;
    }
    while (running) {
      int crawledPages = databaseHelper.getCrawledPagesCount();
      if (crawledPages >= maxPages) {
        ConsoleColors.printSuccess("CrawlerTask");
        System.out.println("Max pages crawled. Stopping.");
        break;
      }
      running = crawl();
    }
  }

  private boolean crawl() {
    try {
      String urlToCrawl = urlQueue.poll(10, TimeUnit.SECONDS);
      if (urlToCrawl == null) {
        ConsoleColors.printWarning("CrawlerTask");
        System.out.println("No URLs to crawl. Exiting.");
        return false;
      }

      urlToCrawl = UrlNormalizer.normalize(urlToCrawl);
      if (urlToCrawl == null) {
        ConsoleColors.printWarning("CrawlerTask");
        System.out.println("Invalid URL: " + urlToCrawl);
        return true;
      }

      // Check if the URL is already crawled
      if (!visitedUrls.add(urlToCrawl)) {
        ConsoleColors.printInfo("CrawlerTask");
        System.out.println("URL already crawled: " + urlToCrawl);
        return true;
      }

      if (databaseHelper.isUrlCrawled(urlToCrawl)) {
        ConsoleColors.printInfo("CrawlerTask");
        System.out.println("URL already crawled: " + urlToCrawl);
        return true;
      }

      if (!Robots.isAllowed(urlToCrawl)) {
        ConsoleColors.printWarning("CrawlerTask");
        System.out.println("Crawling not allowed by robots.txt: " + urlToCrawl);
        return true;
      }

      ConsoleColors.printInfo("CrawlerTask");
      System.out.println("Crawling URL: " + urlToCrawl);
      Connection conn = Jsoup.connect(urlToCrawl);
      Document doc = conn.get();
      if (conn.response().statusCode() == 200) {
        ConsoleColors.printSuccess("CrawlerTask");
        System.out.println("Page title: " + doc.title());
      } else {
        System.out.println("Failed to fetch page. Status code: " + conn.response().statusCode());
      }

      for (Element link : doc.select("a[href]")) {
        String absUrl = link.absUrl("href");
        absUrl = UrlNormalizer.normalize(absUrl);
        if (absUrl != null && UrlNormalizer.isAbsolute(absUrl)) {
          urlQueue.offer(absUrl);
        }
      }

      // Save the html page to file system
      htmlSaver.save(urlToCrawl, doc.html());

      // Save the crawled page to the database
      String title = doc.title();
      String description = doc.select("meta[name=description]").attr("content");
      databaseHelper.insertDocument(
          urlToCrawl, title, description, htmlSaver.getFilePath(urlToCrawl).toString());
      ConsoleColors.printSuccess("CrawlerTask");
      System.out.println("Saved page to database: " + urlToCrawl);
      return true;

    } catch (Exception e) {
      if (e instanceof java.net.SocketTimeoutException) {
        ConsoleColors.printWarning("CrawlerTask");
        System.out.println("Socket timeout while crawling URL: " + e.getMessage());
        return true;
      }
      ConsoleColors.printError("CrawlerTask");
      System.err.println("Error: " + e.getMessage());
      return true;
    }
  }
}
