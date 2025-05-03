package com.sherlook.search;

import com.sherlook.search.crawler.Crawler;
import com.sherlook.search.indexer.Indexer;
import com.sherlook.search.ranker.Ranker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    if (args.length > 0) {
      ApplicationContext context = SpringApplication.run(Application.class);
      switch (args[0].toLowerCase()) {
        case "crawl":
          Crawler crawler = context.getBean(Crawler.class);
          crawler.start();
          break;
        case "index":
          Indexer indexer = context.getBean(Indexer.class);
          indexer.index();
          break;
        case "serve":
          System.out.println("Ready to serve");
          break;

        case "pagerank":
          Ranker ranker = context.getBean(Ranker.class);
          // compute time
          long startTime = System.currentTimeMillis();
          ranker.rankPagesByPopularity();
          long endTime = System.currentTimeMillis();
          long duration = endTime - startTime;
          System.out.println("Time taken to compute PageRank: " + duration + " ms");
          break;

        default:
          System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
      }
    } else {
      System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
    }
  }
}
