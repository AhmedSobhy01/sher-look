package com.sherlook.search;

import com.sherlook.search.crawler.Crawler;
import com.sherlook.search.indexer.Indexer;
import com.sherlook.search.ranker.Ranker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    if (args.length > 0) {
      ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
      switch (args[0].toLowerCase()) {
        case "crawl":
          Crawler crawler = context.getBean(Crawler.class);
          crawler.start();

          context.close();
          System.exit(0);
          break;

        case "index":
          Indexer indexer = context.getBean(Indexer.class);
          indexer.index();

          context.close();
          System.exit(0);
          break;

        case "page-rank":
          Ranker pageRank = context.getBean(Ranker.class);
          pageRank.rankPagesByPopularity();

          context.close();
          System.exit(0);
          break;

        case "serve":
          System.out.println("Ready to serve");
          break;

        default:
          System.out.println("Usage: java -jar search-engine.jar [crawl|index|pageRank|serve]");
          context.close();
          System.exit(1);
      }
    } else {
      System.out.println("Usage: java -jar search-engine.jar [crawl|index|pageRank|serve]");
      System.exit(1);
    }
  }
}
