package com.sherlook.search;

import com.sherlook.search.crawler.Crawler;
import com.sherlook.search.indexer.Indexer;
import com.sherlook.search.ranker.Ranker;
import com.sherlook.search.utils.DatabaseHelper;
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
          System.out.println("Initializing DatabaseHelper...");
          DatabaseHelper databaseHelper = context.getBean(DatabaseHelper.class);
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

          /*

          case "rank":
            Ranker ranker = context.getBean(Ranker.class);
            // compute time

            long startTime = System.currentTimeMillis();
            // stop words are bottlenecks
            List<String> queryTerms =
                Arrays.asList("search", "engine");
            List<RankedDocument> ranked = ranker.rank(queryTerms, true, 0, 10);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("Time taken to compute Rank: " + duration + " ms");
            System.out.println("Ranked documents number :" + ranked.size());
            System.out.println(
                "Ranked document first with url: "
                        +ranked.get(0).getUrl() + " "
                        + ranked.get(0).getTfIdf() + " with snippet "
                      + ranked.get(0).getSnippet()
                    + " with title "
                    + ranked.get(0).getTitle());
            break;


           */

        default:
          System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
      }
    } else {
      System.out.println("Usage: java -jar search-engine.jar [crawl|index|serve]");
    }
  }
}
