package com.sherlook.search.utils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHelper {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertDocument(String url, String title, String description, String filePath) {
        String sql = "INSERT INTO documents (url, title, description, file_path, crawl_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sql, url, title, description, filePath);
    }
}