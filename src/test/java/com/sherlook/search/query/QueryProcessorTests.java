package com.sherlook.search.query;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sherlook.search.utils.DatabaseHelper;

@ExtendWith(MockitoExtension.class)
class QueryProcessorTests {

    @Mock
    private DatabaseHelper databaseHelper;
    @InjectMocks
    private QueryProcessor queryProcessor;
}
