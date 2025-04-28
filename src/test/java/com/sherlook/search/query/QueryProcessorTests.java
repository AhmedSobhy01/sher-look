package com.sherlook.search.query;

import com.sherlook.search.utils.DatabaseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class QueryProcessorTests {
  @Mock
  private DatabaseHelper databaseHelper;
  @InjectMocks
  private QueryProcessor queryProcessor;

  @Test
  void testProcessQuery() {
    // Arrange
    String query = "\"Karim's Car Beautiful\" NOT \"Traveling Traveled Learning\"";

    // Act
    queryProcessor.processQuery(query);
    if (queryProcessor.isPhraseMatching()) {
      String[] result = queryProcessor.getPhrases();
      int[] operators = queryProcessor.getOperators();
      System.out.print(result[0] + " ");
      System.out.print(operators[0] + " ");
      System.out.print(result[1] + " ");
      System.out.print(operators[1]+ " ");
      System.out.println(result[2]);
    } else {
      List<String> stems = queryProcessor.getStems();
      System.out.println(stems);
    }
  }
}