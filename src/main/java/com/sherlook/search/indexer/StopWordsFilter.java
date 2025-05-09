package com.sherlook.search.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class StopWordsFilter {
  private final Set<String> stopWords;

  public StopWordsFilter() {
    try {
      Path path = Paths.get("data/stop-words.txt");

      if (!Files.exists(path))
        throw new RuntimeException("Stop words file not found: " + path.toAbsolutePath());

      stopWords =
          Files.lines(path)
              .map(String::trim)
              .filter(line -> !line.isEmpty())
              .collect(Collectors.toSet());
    } catch (IOException e) {
      throw new RuntimeException("Failed to load stop words", e);
    }
  }

  public boolean isStopWord(String word) {
    return stopWords.contains(word.toLowerCase());
  }
}
