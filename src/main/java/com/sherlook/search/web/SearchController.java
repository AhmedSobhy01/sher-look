package com.sherlook.search.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {
  @GetMapping("/")
  public String index() {
    return "index";
  }

  @GetMapping("/search")
  public String search(@RequestParam("query") String query,
                       @RequestParam("page") int page,
                       Model model) {
    model.addAttribute("query", query);
    model.addAttribute("page", page);
    return "searchResults";
  }
}
