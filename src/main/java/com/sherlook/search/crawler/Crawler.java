package com.sherlook.search.crawler;

import com.sherlook.search.utils.DatabaseHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Io;
import org.springframework.stereotype.Component;

@Component
public class Crawler {
  @Value("${crawler.threads}")
  private int threads;

  @Value("${crawler.max-pages}")
  private int maxPages;

  @Value("${crawler.start-pages}")
  private String startPagesPath;

  @Value("${crawler.savepath}")
  private String saveDirPath;

  private final PersistentQueue urlQueue; // Persistent queue to store URLs
  private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

  private final DatabaseHelper databaseHelper;
  private HtmlSaver htmlSaver;

  public Crawler(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
    try {
      urlQueue = new PersistentQueue(new File("urlQueue.txt"));
    } catch (IOException e) {
      System.err.println("Failed to create PersistentQueue: " + e.getMessage());
      throw new RuntimeException("Failed to create PersistentQueue", e);
    }
  }

  @PostConstruct
  public void init() {
    try {
      this.htmlSaver = new HtmlSaver(saveDirPath);
    } catch (IOException e) {
      System.err.println("Failed to create HtmlSaver: " + e.getMessage());
      System.out.println(saveDirPath);
      this.htmlSaver = null;
    }
  }

  public void start() {
    System.out.println("Starting crawler with " + threads + " threads");
    ExecutorService executor = Executors.newFixedThreadPool(threads);

    Path path = Paths.get("urlQueue.txt");
    // Check if urlQueue.txt doesnt exisit or doenst contain any URLs
    boolean isEmpty = false;

    try {
      isEmpty = Files.notExists(path) || Files.size(path) == 0;
    } catch (IOException e) {
      System.err.println("Error checking urlQueue.txt: " + e.getMessage());
      isEmpty = true;
    }

    if (isEmpty) {
      System.out.println("Starting with empty queue. Reading start pages from " + startPagesPath);
      try (BufferedReader bf = new BufferedReader(new java.io.FileReader(startPagesPath))) {
        String startUrl;
        while ((startUrl = bf.readLine()) != null) {
          urlQueue.offer(startUrl);
        }
      } catch (Exception e) {
        System.err.println("Error reading start pages: " + e.getMessage());
        return;
      }
    }

    for (int i = 0; i < threads; i++) {
      executor.execute(new CrawlTask(urlQueue, visitedUrls, maxPages, databaseHelper, htmlSaver));
    }

    executor.shutdown();
    while (!executor.isTerminated()) {
      // Wait for all tasks to finish
    }

    System.out.println("All tasks completed");
  }
}
