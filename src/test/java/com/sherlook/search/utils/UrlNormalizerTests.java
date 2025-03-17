package com.sherlook.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UrlNormalizerTests {
    @Test
    void testNormalize_WithStandardUrl_ShouldRemoveFragmentAndNormalize() {
        String url = "HTTP://EXAMPLE.COM:80/path/?b=2&a=1#fragment";
        String normalized = UrlNormalizer.normalize(url);
        assertEquals("http://example.com/path?a=1&b=2", normalized);
    }

    @Test
    void testIsAbsolute_WithDifferentUrls_ShouldDetermineCorrectly() {
        assertTrue(UrlNormalizer.isAbsolute("http://example.com"));
        assertFalse(UrlNormalizer.isAbsolute("/path/page.html"));
    }

    @Test
    void testResolve_WithRelativePath_ShouldResolveCorrectly() {
        String base = "http://example.com/base/";
        String relative = "../page.html";
        String resolved = UrlNormalizer.resolve(base, relative);
        assertEquals("http://example.com/page.html", resolved);
    }

    @Test
    void testNormalize_WithHttpsUrl_ShouldNormalizeCorrectly() {
        String url = "HTTPS://EXAMPLE.COM:443/secure/?param=value";
        String normalized = UrlNormalizer.normalize(url);
        assertEquals("https://example.com/secure?param=value", normalized);
    }
    
    @Test
    void testNormalize_WithoutQueryParams_ShouldKeepPathIntact() {
        String url = "http://example.com/path/to/page.html";
        String normalized = UrlNormalizer.normalize(url);
        assertEquals("http://example.com/path/to/page.html", normalized);
    }
    
    @Test
    void testNormalize_WithEmptyQueryParam_ShouldPreserveEmptyValue() {
        String url = "http://example.com/path/?empty=&value=something";
        String normalized = UrlNormalizer.normalize(url);
        assertEquals("http://example.com/path?empty=&value=something", normalized);
    }
    
    @Test
    void testNormalize_WithInvalidUrl_ShouldReturnNull() {
        String url = "not a url";
        String normalized = UrlNormalizer.normalize(url);
        assertNull(normalized);
    }
    
    @Test
    void testNormalize_WithNonDefaultPort_ShouldPreservePort() {
        String url = "http://example.com:8080/path/";
        String normalized = UrlNormalizer.normalize(url);
        assertEquals("http://example.com:8080/path", normalized);
    }
    
    @Test
    void testIsAbsolute_WithVariousSchemes_ShouldClassifyCorrectly() {
        assertTrue(UrlNormalizer.isAbsolute("https://example.com"));
        assertTrue(UrlNormalizer.isAbsolute("ftp://files.example.com"));
        assertFalse(UrlNormalizer.isAbsolute("mailto:user@example.com"));
        assertFalse(UrlNormalizer.isAbsolute("//example.com/path"));
        assertFalse(UrlNormalizer.isAbsolute("example.com"));
    }
    
    @Test
    void testResolve_WithAbsoluteUrl_ShouldReturnUnchanged() {
        String base = "http://example.com/base/";
        String absolute = "https://another.com/page.html";
        String resolved = UrlNormalizer.resolve(base, absolute);
        assertEquals("https://another.com/page.html", resolved);
    }
    
    @Test
    void testResolve_WithEmptyRelativeUrl_ShouldReturnBaseUrl() {
        String base = "http://example.com/base/";
        String relative = "";
        String resolved = UrlNormalizer.resolve(base, relative);
        assertEquals("http://example.com/base/", resolved);
    }
    
    @Test
    void testResolve_WithQueryParams_ShouldPreserveParams() {
        String base = "http://example.com/base/";
        String relative = "page.html?param=value";
        String resolved = UrlNormalizer.resolve(base, relative);
        assertEquals("http://example.com/base/page.html?param=value", resolved);
    }
    
    @Test
    void testResolve_WithSpecialCharacters_ShouldHandleEncodingCorrectly() {
        String base = "http://example.com/base/";
        String relative = "search?q=test%20query&lang=en";
        String resolved = UrlNormalizer.resolve(base, relative);
        assertEquals("http://example.com/base/search?q=test%20query&lang=en", resolved);
    }
    
    @Test
    void testResolve_WithInvalidBaseUrl_ShouldReturnNull() {
        String base = "invalid url";
        String relative = "page.html";
        String resolved = UrlNormalizer.resolve(base, relative);
        assertNull(resolved);
    }
}