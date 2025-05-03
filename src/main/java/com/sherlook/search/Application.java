package com.sherlook.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.sherlook.search.crawler.Crawler;
import com.sherlook.search.indexer.Indexer;
import com.sherlook.search.utils.DatabaseHelper;

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
          DatabaseHelper databaseHelper = context.getBean(DatabaseHelper.class);
          System.out.println("Ready to serve");
          break;

        /*
         * case "rank":
         * Ranker ranker = context.getBean(Ranker.class);
         * // compute time
         *
         * long startTime = System.currentTimeMillis();
         * // stop words are bottlenecks
         * List<String> queryTerms = Arrays.asList("machine", "learning");
         * List<RankedDocument> ranked = ranker.rank(queryTerms, false, 3, 20);
         * long endTime = System.currentTimeMillis();
         * long duration = endTime - startTime;
         * System.out.println("Time taken to compute Rank: " + duration + " ms");
         * System.out.println("Ranked documents number :" + ranked.size());
         * System.out.println(
         * "Ranked document first with url: "
         * + ranked.get(2).getUrl()
         * + " with title "
         * + ranked.get(2).getTitle());
         * break;
         */
        /*
         * case "rank":
         * Ranker ranker = context.getBean(Ranker.class);
         * // compute time
         *
         * long startTime = System.currentTimeMillis();
         * // stop words are bottlenecks
         * List<String> queryTerms = Arrays.asList("machine", "learning");
         * List<RankedDocument> ranked = ranker.rank(queryTerms, false, 3, 20);
         * long endTime = System.currentTimeMillis();
         * long duration = endTime - startTime;
         * System.out.println("Time taken to compute Rank: " + duration + " ms");
         * System.out.println("Ranked documents number :" + ranked.size());
         * System.out.println(
         * "Ranked document first with url: "
         * + ranked.get(2).getUrl()
         * + " with title "
         * + ranked.get(2).getTitle());
         * break;
         */

        default:
          System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
      }
    } else {
      System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
    }
  }
}
