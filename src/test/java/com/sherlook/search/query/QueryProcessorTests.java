package com.sherlook.search.query;

import com.sherlook.search.utils.DatabaseHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryProcessorTests {

  @Mock private DatabaseHelper databaseHelper;
  @InjectMocks private QueryProcessor queryProcessor;
}
