package com.sherlook.search.crawler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.*;

class HtmlSaverTests {

  private static Path tempDir;
  private HtmlSaver htmlSaver;

  @BeforeAll
  static void setupClass() throws IOException {
    tempDir = Files.createTempDirectory("html-saver-test");
  }

  @AfterAll
  static void cleanupClass() throws IOException {
    // Clean up temporary directory after all tests
    Files.walk(tempDir)
        .map(Path::toFile)
        .sorted((a, b) -> -a.compareTo(b)) // delete files before directories
        .forEach(File::delete);
  }

  @BeforeEach
  void setUp() throws IOException {
    htmlSaver = new HtmlSaver(tempDir.toString());
  }

  @Test
  void testSave_createsFile() throws IOException, NoSuchAlgorithmException {
    String url = "http://example.com/page";
    String html = "<html><body>Test Page</body></html>";

    htmlSaver.save(url, html);

    Path expectedPath = htmlSaver.getFilePath(url);
    assertTrue(Files.exists(expectedPath), "HTML file should be created");
    String savedContent = Files.readString(expectedPath);
    assertEquals(html, savedContent, "Saved HTML content should match");
  }

  @Test
  void testGetFilePath_returnsHashedPath() throws NoSuchAlgorithmException {
    String url = "http://example.com/page";
    Path path = htmlSaver.getFilePath(url);

    assertTrue(path.getFileName().toString().endsWith(".html"), "Filename should end with .html");
    assertEquals(tempDir, path.getParent(), "File should be saved in the specified directory");
  }
}
