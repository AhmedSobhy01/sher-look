package com.sherlook.search.crawler;

import com.sherlook.search.utils.UrlNormalizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PersistentQueue {
  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
  private final Set<String> uncrawledSet = ConcurrentHashMap.newKeySet();
  private final File queueFile;

  public PersistentQueue(File queueFile) throws IOException {
    this.queueFile = queueFile;

    if (queueFile.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(queueFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = UrlNormalizer.normalize(line);
          if (line != null) {
            uncrawledSet.add(line);
            queue.offer(line);
          }
        }
      }
    } else {
      queueFile.createNewFile();
    }
  }

  public void offer(String url) {
    try {
      if (url != null) {
        url = UrlNormalizer.normalize(url);
        if (url == null || uncrawledSet.contains(url)) {
          return;
        }
        synchronized (queueFile) {
          try (FileWriter writer = new FileWriter(queueFile, true)) {
            writer.write(url + "\n");
            queue.offer(url);
            uncrawledSet.add(url);
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error writing to queue file: " + e.getMessage());
    }
  }

  public String poll(long timeout, TimeUnit unit) throws InterruptedException {
    return queue.poll(timeout, unit);
  }
}
