package com.sherlook.search.web;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.ranker.RankedDocument;
import com.sherlook.search.ranker.Ranker;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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

  private final Map<String, Ranker.RankingResult> rankingCache = new HashMap<>();
  private final Map<String, Long> cacheTimestamps = new HashMap<>();
  private static final long CACHE_EXPIRY_MS = 120000; // 2 minutes

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
    queryProcessor.processQuery(query);
    List<String> searchTerms = getSearchTerms();
    boolean isPhraseSearch = queryProcessor.isPhraseMatching();

    return getSearchResults(searchTerms, isPhraseSearch, page, resultsPerPage, startTime);
  }

  private SearchResponse getSearchResults(
      List<String> searchTerms,
      boolean isPhraseSearch,
      int page,
      int resultsPerPage,
      long startTime) {

    String cacheKey = getCacheKey(searchTerms, isPhraseSearch);
    int offset = (page - 1) * resultsPerPage;

    List<RankedDocument> results;
    Ranker.RankingResult rankingResult;

    if (rankingCache.containsKey(cacheKey)) {
      long timestamp = cacheTimestamps.getOrDefault(cacheKey, 0L);
      if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY_MS) {
        rankingResult = rankingCache.get(cacheKey);
        results =
            ranker.getPageWithSnippets(rankingResult, searchTerms, offset, resultsPerPage, false);
        return createResponse(
            results, rankingResult, resultsPerPage, System.currentTimeMillis() - startTime);
      }
    }

    // Cache miss or expired
    rankingResult = ranker.rankAndStoreTotalDocuments(searchTerms, isPhraseSearch);
    rankingCache.put(cacheKey, rankingResult);
    cacheTimestamps.put(cacheKey, System.currentTimeMillis());

    results = ranker.getPageWithSnippets(rankingResult, searchTerms, offset, resultsPerPage, false);
    return createResponse(
        results, rankingResult, resultsPerPage, System.currentTimeMillis() - startTime);
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

  private String getCacheKey(List<String> queryTerms, Boolean isPhraseSearch) {
    return queryTerms.isEmpty() ? "empty" : String.join(",", queryTerms) + "|" + isPhraseSearch;
  }

  private List<String> getSearchTerms() {
    if (queryProcessor.isPhraseMatching() && queryProcessor.getPhrases().length > 0) {
      return Arrays.asList(queryProcessor.getPhrases()[0].split("\\s+"));
    } else {
      return queryProcessor.getTokens();
    }
  }

  @Scheduled(fixedRate = 60000) // Run every minute
  public void cleanupExpiredCache() {
    long currentTime = System.currentTimeMillis();
    List<String> keysToRemove =
        cacheTimestamps.entrySet().stream()
            .filter(entry -> currentTime - entry.getValue() >= CACHE_EXPIRY_MS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    for (String key : keysToRemove) {
      rankingCache.remove(key);
      cacheTimestamps.remove(key);
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
