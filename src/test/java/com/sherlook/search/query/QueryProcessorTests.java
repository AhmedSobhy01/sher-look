package com.sherlook.search.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sherlook.search.utils.DatabaseHelper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryTests {
  @Mock private DatabaseHelper databaseHelper;
  @InjectMocks private QueryProcessor queryProcessor;

  @Test
  void testProcessQuery_WithThreeWords_GetsWordsIds() {
    // Arrange
    when(databaseHelper.getWordID(eq("machine"))).thenReturn(1);
    when(databaseHelper.getWordID(eq("learning"))).thenReturn(2);
    when(databaseHelper.getWordID(eq("algorithms"))).thenReturn(3);

    // Act
    List<Integer> wordsIds = queryProcessor.processQuery("machine learning algorithms");

    // Assert
    assertEquals(List.of(1, 2, 3), wordsIds);
  }
}
