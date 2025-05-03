package com.sherlook.search.web;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.ranker.RankedDocument;
import com.sherlook.search.ranker.Ranker;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SearchController {
  private final QueryProcessor queryProcessor;
  private final Ranker ranker;

  private final Map<String, Ranker.RankingResult> rankingCache = new HashMap<>();
  private final Map<String, Long> cacheTimestamps = new HashMap<>();
  private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes

  @Autowired
  public SearchController(QueryProcessor queryProcessor, Ranker ranker) {
    this.queryProcessor = queryProcessor;
    this.ranker = ranker;
  }

  @GetMapping("/")
  public String index() {
    return "index";
  }

  @GetMapping("/search")
  @ResponseBody
  public List<RankedDocument> search(
      @RequestParam String query,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int resultsPerPage) {

    queryProcessor.processQuery(query);
    List<String> searchTerms = getSearchTerms();
    boolean isPhraseSearch = queryProcessor.isPhraseMatching();
    int offset = (page - 1) * resultsPerPage;

    String cacheKey = getCacheKey(searchTerms, isPhraseSearch);

    Ranker.RankingResult result = ranker.rankAndStoreTotalDocuments(searchTerms, isPhraseSearch);

    rankingCache.put(cacheKey, result);
    cacheTimestamps.put(cacheKey, System.currentTimeMillis());

    return ranker.getPageWithSnippets(result, searchTerms, offset, resultsPerPage);
  }

  @GetMapping("/pagination")
  @ResponseBody
  public List<RankedDocument> paginate(
      @RequestParam String query,
      @RequestParam int page,
      @RequestParam(defaultValue = "10") int resultsPerPage) {
    queryProcessor.processQuery(query);
    List<String> searchTerms = getSearchTerms();
    boolean isPhraseSearch = queryProcessor.isPhraseMatching();

    String cacheKey = getCacheKey(searchTerms, isPhraseSearch);

    if (rankingCache.containsKey(cacheKey)) {
      long timestamp = cacheTimestamps.getOrDefault(cacheKey, 0L);
      if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY_MS) {
        int offset = (page - 1) * resultsPerPage;
        Ranker.RankingResult cachedResult = rankingCache.get(cacheKey);
        return ranker.getPageWithSnippets(cachedResult, searchTerms, offset, resultsPerPage);
      }
    }
    // Cache miss or expired
    return search(query, page, resultsPerPage);
  }

  private String getCacheKey(List<String> queryTerms, Boolean isPhraseSearch) {
    return String.join(",", queryTerms) + "|" + isPhraseSearch;
  }

  private List<String> getSearchTerms() {
    if (queryProcessor.isPhraseMatching()) {
      return Arrays.asList(queryProcessor.getPhrases()[0].split("\\s+"));
    } else {
      return queryProcessor.getTokens();
    }
  }
}
