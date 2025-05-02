package com.sherlook.search.web;

import com.sherlook.search.query.QueryProcessor;
import com.sherlook.search.ranker.RankedDocument;
import com.sherlook.search.ranker.Ranker;
import com.sherlook.search.utils.DatabaseHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

  @Autowired
  private Ranker ranker;

  @GetMapping("/")
  public String index() {
    return "index";
  }

  @GetMapping("/search")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> search(
          @RequestParam("query") String query,
          @RequestParam(value = "page", defaultValue = "1") int page) {

    QueryProcessor queryProcessor = new QueryProcessor();
    queryProcessor.processQuery(query);

    if(queryProcessor.getTokens().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "No valid search terms provided."));
    }

    // Calculate offset and limit for pagination
    int limit = 10; // results per page
    int offset = (page - 1) * limit;
    boolean phraseSearch = queryProcessor.isPhraseMatching();
    List<String> tokens = queryProcessor.getTokens();

    List<RankedDocument> results = ranker.rank(tokens, phraseSearch, offset, limit);

    // Build response
    Map<String, Object> response = new HashMap<>();
    response.put("query", query);
    response.put("page", page);
    response.put("phraseSearch", phraseSearch);
    response.put("results", results);
    response.put("totalResults", results.size());

    return ResponseEntity.ok(response);
  }
}
