package com.sherlook.search.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.ranker.RankedDocument;
import com.sherlook.search.ranker.Ranker;
import com.sherlook.search.utils.ConsoleColors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin(
    origins = {"http://localhost:5173", "http://localhost:3000"},
    maxAge = 3600)
public class SearchController {
  private final QueryProcessor queryProcessor;
  private final Ranker ranker;

  private final Cache<String, Ranker.RankingResult> rankingCache =
      Caffeine.newBuilder()
          .maximumSize(3000) // LRU component
          .expireAfterWrite(
              30, TimeUnit.MINUTES) // TTL component (very frequent queries could become stale)
          .build();

  @Autowired
  public SearchController(QueryProcessor queryProcessor, Ranker ranker) {
    this.queryProcessor = queryProcessor;
    this.ranker = ranker;
  }

  @GetMapping("/search")
  @ResponseBody
  public SearchResponse search(
      @RequestParam String query,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int resultsPerPage) {

    long startTime = System.currentTimeMillis();

    ConsoleColors.printInfo("SearchController");
    System.out.println("Received search request:");
    ConsoleColors.printInfo("SearchController");
    System.out.println("  Query: " + ConsoleColors.BOLD_CYAN + query + ConsoleColors.RESET);
    ConsoleColors.printInfo("SearchController");
    System.out.println("  Page: " + page + ", Results per page: " + resultsPerPage);

    queryProcessor.processQuery(query);
    String[] phrases = queryProcessor.getPhrases();
    int[] operators = queryProcessor.getOperators();
    boolean isPhraseSearch = queryProcessor.isPhraseMatching();
    List<String> searchTerms = getSearchTerms();

    ConsoleColors.printInfo("SearchController");
    System.out.println(
        "Search type: "
            + (isPhraseSearch
                ? ConsoleColors.BOLD_YELLOW + "Phrase search"
                : ConsoleColors.BOLD_GREEN + "Keyword search")
            + ConsoleColors.RESET);

    SearchResponse response =
        getSearchResults(
            phrases, operators, isPhraseSearch, searchTerms, page, resultsPerPage, startTime);

    long totalTime = System.currentTimeMillis() - startTime;
    ConsoleColors.printSuccess("SearchController");
    System.out.println(
        "Search completed in "
            + ConsoleColors.BOLD_GREEN
            + totalTime
            + "ms"
            + ConsoleColors.RESET
            + " - Found "
            + ConsoleColors.BOLD_CYAN
            + response.getTotalDocuments()
            + ConsoleColors.RESET
            + " results");

    return response;
  }

  private SearchResponse getSearchResults(
      String[] phrases,
      int[] operators,
      boolean isPhraseSearch,
      List<String> searchTerms,
      int page,
      int resultsPerPage,
      long startTime) {

    String cacheKey = getCacheKey(searchTerms, isPhraseSearch, operators);
    int offset = (page - 1) * resultsPerPage;

    ConsoleColors.printInfo("SearchController");
    System.out.println("Cache key: " + ConsoleColors.BOLD_PURPLE + cacheKey + ConsoleColors.RESET);
    ConsoleColors.printInfo("SearchController");
    System.out.print("Search terms: ");
    for (String term : searchTerms) {
      System.out.print(ConsoleColors.BOLD_CYAN + term + ConsoleColors.RESET + " ");
    }
    System.out.println();

    List<RankedDocument> results;
    Ranker.RankingResult rankingResult;

    ConsoleColors.printInfo("SearchController");
    System.out.println("Checking ranking cache...");

    boolean cacheHit = rankingCache.getIfPresent(cacheKey) != null;
    ConsoleColors.printInfo("SearchController");
    System.out.println(
        cacheHit
            ? "Cache " + ConsoleColors.BOLD_GREEN + "HIT" + ConsoleColors.RESET
            : "Cache "
                + ConsoleColors.BOLD_YELLOW
                + "MISS"
                + ConsoleColors.RESET
                + " - Performing ranking");
    rankingResult =
        rankingCache.get(
            cacheKey,
            key -> {
              if (isPhraseSearch) {
                return ranker.rankAndStoreTotalDocumentsPhrases(phrases, operators);
              } else {
                return ranker.rankAndStoreTotalDocuments(searchTerms, isPhraseSearch);
              }
            });

    ConsoleColors.printInfo("SearchController");
    System.out.println("Getting page " + page + " with offset " + offset);
    results = ranker.getPageWithSnippets(rankingResult, searchTerms, offset, resultsPerPage);

    long queryTime = System.currentTimeMillis() - startTime;

    ConsoleColors.printInfo("SearchController");
    System.out.println(
        "Returning "
            + results.size()
            + " results (page "
            + page
            + " of "
            + Math.ceil((double) rankingResult.getTotalDocuments() / resultsPerPage)
            + ")");

    return createResponse(results, rankingResult, resultsPerPage, queryTime);
  }

  private SearchResponse createResponse(
      List<RankedDocument> results,
      Ranker.RankingResult rankingResult,
      int resultsPerPage,
      long timeMs) {
    int totalDocs = rankingResult.getTotalDocuments();
    int totalPages = (int) Math.ceil((double) totalDocs / resultsPerPage);
    return new SearchResponse(results, totalPages, timeMs, totalDocs);
  }

  private String getCacheKey(List<String> queryTerms, Boolean isPhraseSearch, int[] operators) {
    if (queryTerms.isEmpty()) return "empty";

    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(String.join(",", queryTerms));
    keyBuilder.append("|").append(isPhraseSearch);

    if (operators != null && operators.length > 0) {
      keyBuilder.append("|");
      for (int i = 0; i < operators.length; i++) {
        keyBuilder.append(operators[i]);
        if (i < operators.length - 1) {
          keyBuilder.append(",");
        }
      }
    }

    return keyBuilder.toString();
  }

  private List<String> getSearchTerms() {
    if (queryProcessor.isPhraseMatching()) {
      // Collect all terms from non-NOT phrases for snippet generation
      String[] phrases = queryProcessor.getPhrases();
      int[] operators = queryProcessor.getOperators();
      List<String> terms = new ArrayList<>();
      for (int i = 0; i < phrases.length && phrases[i] != null; i++) {
        // Exclude terms from NOT phrases
        if (i > 0 && operators[i - 1] == 3) {
          continue;
        }
        terms.addAll(Arrays.asList(phrases[i].split("\\s+")));
      }
      return terms;
    } else {
      return queryProcessor.getTokens();
    }
  }

  public static class SearchResponse {
    private final List<RankedDocument> results;
    private final int totalPages;
    private final long timeMs;
    private final int totalDocuments;

    public SearchResponse(
        List<RankedDocument> results, int totalPages, long timeMs, int totalDocuments) {
      this.results = results;
      this.totalPages = totalPages;
      this.timeMs = timeMs;
      this.totalDocuments = totalDocuments;
    }

    public List<RankedDocument> getResults() {
      return results;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public long getTimeMs() {
      return timeMs;
    }

    public int getTotalDocuments() {
      return totalDocuments;
    }
  }
}
