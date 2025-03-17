package com.sherlook.search.utils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;


@SpringBootTest
class DatabaseHelperTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private DatabaseHelper databaseHelper;
    
    private static final String TEST_URL_PREFIX = "https://test-";
    private static final String TEST_TITLE = "Test Title";
    private static final String TEST_DESCRIPTION = "Test Description";
    private static final String TEST_FILE_PATH = "/path/to/test/file.pdf";
    
    @BeforeEach
    void setUp() {
        databaseHelper = new DatabaseHelper(jdbcTemplate);
        
        jdbcTemplate.update("DELETE FROM documents WHERE url LIKE ?", TEST_URL_PREFIX + "%");
    }
    
    @Test
    void testInsertDocument_WithAllFields_ShouldInsertSuccessfully() {
        String url = TEST_URL_PREFIX + "complete";
        
        databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH);
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM documents WHERE url = ?", url);
        assertEquals(1, results.size(), "Should insert exactly one document");
        
        Map<String, Object> document = results.get(0);
        assertEquals(TEST_TITLE, document.get("title"), "Title should match inserted value");
        assertEquals(TEST_DESCRIPTION, document.get("description"), "Description should match inserted value");
        assertEquals(TEST_FILE_PATH, document.get("file_path"), "File path should match inserted value");
    }
    
    @Test
    void testInsertDocument_WithNullDescription_ShouldInsertWithNullDescription() {
        String url = TEST_URL_PREFIX + "null-description";
        
        databaseHelper.insertDocument(url, TEST_TITLE, null, TEST_FILE_PATH);
        
        Map<String, Object> document = jdbcTemplate.queryForMap("SELECT * FROM documents WHERE url = ?", url);
        assertEquals(TEST_TITLE, document.get("title"));
        assertNull(document.get("description"), "Description should be null");
        assertEquals(TEST_FILE_PATH, document.get("file_path"));
    }
    
    @Test
    void testInsertDocument_WithNullTitle_ShouldInsertWithNullTitle() {
        String url = TEST_URL_PREFIX + "null-title";
        
        databaseHelper.insertDocument(url, null, TEST_DESCRIPTION, TEST_FILE_PATH);
        
        Map<String, Object> document = jdbcTemplate.queryForMap("SELECT * FROM documents WHERE url = ?", url);
        assertNull(document.get("title"), "Title should be null");
        assertEquals(TEST_DESCRIPTION, document.get("description"));
    }
    
    @Test
    void testInsertDocument_WithDuplicateUrl_ShouldThrowException() {
        String url = TEST_URL_PREFIX + "duplicate";
        databaseHelper.insertDocument(url, TEST_TITLE, TEST_DESCRIPTION, TEST_FILE_PATH);
        
        UncategorizedSQLException exception = assertThrows(
            UncategorizedSQLException.class, 
            () -> databaseHelper.insertDocument(url, "Another Title", "Another Description", TEST_FILE_PATH),
            "Should throw exception when inserting document with duplicate URL"
        );
        
        String exceptionMessage = exception.getMessage().toLowerCase();
        boolean hasConstraintViolation = exceptionMessage.contains("unique") || exceptionMessage.contains("duplicate") || exceptionMessage.contains("constraint");
        
        assertEquals(true, hasConstraintViolation, "Exception should mention constraint violation");
    }
}