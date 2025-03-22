package com.sherlook.search.crawler;

import com.sherlook.search.utils.UrlNormalizer;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crawler {
  @Value("${crawler.threads}")
  private int threads;

  @Value("${crawler.max-pages}")
  private int maxPages;

  private final Set<String> visited = ConcurrentHashMap.newKeySet();
  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  public void start() {
    System.out.println("Starting crawler with " + threads + " threads...");
    ExecutorService executor = Executors.newFixedThreadPool(threads);

    // Seed the crawler
    queue.offer("https://curlie.org/search?q=gaming&stime=92452189");

    // Start worker threads
    for (int i = 0; i < threads; i++) {
      executor.submit(this::crawl);
    }

    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void crawl() {
    while (!queue.isEmpty() && visited.size() < maxPages) {
      try {
        String url = queue.poll(2, TimeUnit.SECONDS); // Get URL or timeout
        if (url == null || visited.contains(url))
          continue;

        // Mark URL as visited
        visited.add(url);
        System.out.println("Crawling: " + url);

        // Fetch the page
        Connection.Response response = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; SherlookBot/1.0; +http://yourdomain.com)")
            .timeout(5000)
            .execute();

        if (!response.contentType().contains("text/html"))
          continue;

        Document doc = response.parse();

        // Save page content
        savePage(doc, visited.size());

        // Extract links and add them to the queue
        Elements links = doc.select("a[href]");
        for (Element link : links) {
          String newUrl = UrlNormalizer.normalize(link.absUrl("href"));
          if (newUrl != null && !visited.contains(newUrl)) {
            queue.offer(newUrl);
          }
        }

        // Delay to avoid getting blocked
        Thread.sleep(500);

      } catch (IOException | InterruptedException e) {
        System.err.println("Failed to crawl URL: " + e.getMessage());
      }
    }
  }

  private void savePage(Document doc, int id) {
    try (FileWriter file = new FileWriter("data/crawled_pages/" + id + ".html")) {
      file.write(doc.html());
    } catch (IOException e) {
      System.err.println("Failed to save page: " + e.getMessage());
    }
  }
}
