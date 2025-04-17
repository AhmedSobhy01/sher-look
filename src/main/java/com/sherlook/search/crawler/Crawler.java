package com.sherlook.search.crawler;

import com.sherlook.search.utils.DatabaseHelper;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crawler {
  @Value("${crawler.threads}")
  private int threads;

  @Value("${crawler.max-pages}")
  private int maxPages;

  @Value("${crawler.start-url}")
  private String startUrl;

  @Value("${crawler.savepath}")
  private String saveDirPath;

  BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>();

  private final DatabaseHelper databaseHelper;
  private HtmlSaver htmlSaver;

  public Crawler(DatabaseHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  @PostConstruct
  public void init() {
    try {
      this.htmlSaver = new HtmlSaver(saveDirPath);
    } catch (Exception e) {
      System.err.println("Failed to create HtmlSaver: " + e.getMessage());
      System.out.println(saveDirPath);
      this.htmlSaver = null;
    }
  }

  public void start() {
    System.out.println("Starting crawler with " + threads + " threads");
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    urlQueue.add(startUrl);
    for (int i = 0; i < threads; i++) {
      executor.execute(new CrawlTask(urlQueue, maxPages, databaseHelper, htmlSaver));
      // System.out.println("Thread " + i + " started");
    }

    executor.shutdown();
    while (!executor.isTerminated()) {
      // Wait for all tasks to finish
    }

    System.out.println("All tasks completed");
  }
}
