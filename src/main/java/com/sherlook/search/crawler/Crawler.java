package com.sherlook.search.crawler;

import com.sherlook.search.utils.ConsoleColors;
import com.sherlook.search.utils.DatabaseHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${crawler.url-queue-file}")
  private String urlQueueFilePath;

  @Value("${crawler.max-depth}")
  private int maxDepth;

  private final DatabaseHelper databaseHelper;

  private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

  private HtmlSaver htmlSaver;
  private PersistentQueue urlQueue; // Persistent queue to store URLs

  // Constructor for dependency injection
  // This constructor is used for testing purposes

  public Crawler(DatabaseHelper databaseHelper, PersistentQueue queue, HtmlSaver htmlSaver) {
    this.databaseHelper = databaseHelper;
    this.urlQueue = queue;
    this.htmlSaver = htmlSaver;
  }

  @Autowired
  public Crawler(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  @PostConstruct
  public void init() {
    try {
      if (htmlSaver == null) {
        htmlSaver = new HtmlSaver(saveDirPath);
      }
      if (urlQueue == null) {
        urlQueue = new PersistentQueue(new File(urlQueueFilePath), visitedUrls);
      }
    } catch (IOException e) {
      ConsoleColors.printError("Crawler");
      System.err.println("Failed to create Classes: " + e.getMessage());
      System.out.println(saveDirPath);
      this.htmlSaver = null;
      this.urlQueue = null;
    }
  }

  public void start() {
    ConsoleColors.printInfo("Crawler");
    System.out.println("Starting crawler with " + threads + " threads");
    ExecutorService executor = Executors.newFixedThreadPool(threads);

    if (htmlSaver == null || urlQueue == null) {
      ConsoleColors.printError("Crawler");
      System.err.println("Crawler not initialized properly. Exiting.");
      return;
    }

    boolean isEmpty = urlQueue.isIntiallyEmpty();
    if (isEmpty) {
      ConsoleColors.printInfo("Crawler");
      System.out.println("Starting with empty queue. Reading start pages from " + startPagesPath);
      try (BufferedReader bf = new BufferedReader(new java.io.FileReader(startPagesPath))) {
        String startUrlPair;
        while ((startUrlPair = bf.readLine()) != null) {
          String[] parts = startUrlPair.split(" ");
          String url = parts[0];
          int depth = Integer.parseInt(parts[1]);
          UrlDepthPair startUrl = new UrlDepthPair(url, depth);
          urlQueue.offer(startUrl);
        }
      } catch (Exception e) {
        ConsoleColors.printError("Crawler");
        System.err.println("Error reading start pages: " + e.getMessage());
        return;
      }
    }

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < threads; i++) {
      executor.execute(
          new CrawlTask(urlQueue, visitedUrls, maxPages, databaseHelper, htmlSaver, maxDepth, i));
    }

    executor.shutdown();
    while (!executor.isTerminated()) {
      // Wait for all tasks to finish
    }
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    ConsoleColors.printSuccess("Crawler");
    System.out.println("All tasks completed in " + duration + " ms");
  }
}
