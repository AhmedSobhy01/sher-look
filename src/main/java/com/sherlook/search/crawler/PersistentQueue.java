package com.sherlook.search.crawler;

import com.sherlook.search.utils.UrlNormalizer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PersistentQueue {
  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
  private final Set<String> uncrawledSet = ConcurrentHashMap.newKeySet();
  private final Map<String, Long> urlPositionMap = new ConcurrentHashMap<>();
  private final File queueFile;
  private long currentPosition = 0;

  public PersistentQueue(File queueFile) throws IOException {
    this.queueFile = queueFile;

    if (queueFile.exists()) {
      try (RandomAccessFile file = new RandomAccessFile(queueFile, "r")) {
        String line;
        file.seek(0);

        while (file.getFilePointer() < file.length()) {
          line = file.readLine();
          if (line != null && line.startsWith("U_")) {
            line = line.substring(2);
            String url = UrlNormalizer.normalize(line);
            if (url == null) {
              continue;
            }
            uncrawledSet.add(url);
            queue.offer(url);
            urlPositionMap.put(url, currentPosition);
          }
          currentPosition = file.getFilePointer();
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
          try (RandomAccessFile file = new RandomAccessFile(queueFile, "rw")) {
            file.seek(currentPosition);
            urlPositionMap.put(url, currentPosition);
            file.writeBytes("U_" + url + "\n");
            currentPosition = file.getFilePointer();
            queue.offer(url);
            uncrawledSet.add(url);
          }
        }
      }
    } catch (IOException e) {
      System.err.println("[Queue] Error writing to queue file: " + e.getMessage());
    }
  }

  public String poll(long timeout, TimeUnit unit) throws InterruptedException {
    String url = queue.poll(timeout, unit);
    if (url == null) return null;

    try {
      synchronized (queueFile) {
        try (RandomAccessFile file = new RandomAccessFile(queueFile, "rw")) {
          file.seek(urlPositionMap.get(url));
          file.writeBytes("V_" + url + "\n");
          uncrawledSet.remove(url);
        }
      }
    } catch (IOException e) {
      System.err.println("[Queue] Error writing to queue file: " + e.getMessage());
    }
    return url;
  }
}
