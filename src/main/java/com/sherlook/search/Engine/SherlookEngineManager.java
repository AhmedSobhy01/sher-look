package com.sherlook.search.Engine;

import com.sherlook.search.crawler.Crawler;
import com.sherlook.search.indexer.Indexer;
import com.sherlook.search.ranker.Ranker;
import com.sherlook.search.utils.DatabaseHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SherlookEngineManager {
  private final DatabaseHelper databaseHelper;
  private final Ranker ranker;
  private final Crawler crawler;
  private final Indexer indexer;

  public SherlookEngineManager(
      DatabaseHelper databaseHelper, Ranker ranker, Crawler crawler, Indexer indexer) {
    this.databaseHelper = databaseHelper;
    this.ranker = ranker;
    this.crawler = crawler;
    this.indexer = indexer;
  }

  public void runOfflineProcessing() {
    System.out.println("Running offline processing...");
    runCrawlAndProcess();
    System.out.println("Offline processing completed.");
  }

  public void runCrawlAndProcess() {
    System.out.println("Started Crawling");
    crawler.start();
    System.out.println("Crawling completed.");
    processPostCrawlTasks();
  }

  public void processPostCrawlTasks() {
    System.out.println("Started Indexing and PageRank in parallel");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    Future<Void> indexingFuture =
        executor.submit(
            () -> {
              indexer.index();
              return null;
            });
    Future<Void> pageRankFuture =
        executor.submit(
            () -> {
              ranker.rankPagesByPopularity();
              return null;
            });
    try {
      indexingFuture.get();
      pageRankFuture.get();
    } catch (Exception e) {
      System.err.println("Error during post-crawl processing: " + e.getMessage());
    } finally {
      executor.shutdown();
    }
  }
}
