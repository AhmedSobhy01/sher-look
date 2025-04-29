package com.sherlook.search.crawler;

import com.sherlook.search.utils.Hash;
import com.sherlook.search.utils.UrlNormalizer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class HtmlSaver {
  private final Path saveDir;

  public HtmlSaver(String saveDirPath) throws IOException {
    this.saveDir = Paths.get(saveDirPath);
    if (!Files.exists(saveDir)) {
      Files.createDirectories(saveDir);
    }
  }

  public void save(String url, String htmlContent) throws IOException, NoSuchAlgorithmException {
    Path filePath = getFilePath(url);
    Files.writeString(filePath, htmlContent);
  }

  public Path getFilePath(String url) throws NoSuchAlgorithmException {
    String normalized = UrlNormalizer.normalize(url);
    String hash = Hash.sha256(normalized);
    return saveDir.resolve(hash + ".html");
  }
}
