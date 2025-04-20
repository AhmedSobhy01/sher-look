package com.sherlook.search.crawler;

import com.sherlook.search.utils.ConsoleColors;
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
  private final BlockingQueue<UrlDepthPair> queue = new LinkedBlockingQueue<>();
  private final Set<UrlDepthPair> uncrawledSet = ConcurrentHashMap.newKeySet();
  private final Map<UrlDepthPair, Long> urlPositionMap = new ConcurrentHashMap<>();
  private final File queueFile;
  boolean intiallyEmpty = true;
  private long currentPosition = 0;

  public PersistentQueue(File queueFile, Set<String> visitedUrlsSet) throws IOException {
    this.queueFile = queueFile;

    if (queueFile.exists()) {
      intiallyEmpty = false;
      try (RandomAccessFile file = new RandomAccessFile(queueFile, "r")) {
        String line;
        file.seek(0);

        while (file.getFilePointer() < file.length()) {
          line = file.readLine();
          if (line != null && line.startsWith("U_")) {
            line = line.substring(2);
            String[] parts = line.split(" ");
            String url = UrlNormalizer.normalize(parts[0]);
            int depth = Integer.parseInt(parts[1]);
            if (url == null) {
              continue;
            }
            UrlDepthPair urlDepthPair = new UrlDepthPair(url, depth);
            uncrawledSet.add(urlDepthPair);
            queue.offer(urlDepthPair);
            urlPositionMap.put(urlDepthPair, currentPosition);
          } else if (line != null && line.startsWith("V_")) {
            line = line.substring(2);
            String[] parts = line.split(" ");
            String url = UrlNormalizer.normalize(parts[0]);
            int depth = Integer.parseInt(parts[1]);
            if (url == null) {
              continue;
            }
            UrlDepthPair urlDepthPair = new UrlDepthPair(url, depth);
            visitedUrlsSet.add(urlDepthPair.getUrl());
          }
          currentPosition = file.getFilePointer();
        }
      }
    } else {
      intiallyEmpty = true;
      queueFile.getParentFile().mkdirs();
      queueFile.createNewFile();
    }
  }

  public boolean isIntiallyEmpty() {
    return intiallyEmpty;
  }

  public boolean offer(UrlDepthPair urlDepthPair) {
    try {
      if (urlDepthPair == null || uncrawledSet.contains(urlDepthPair)) return false;

      String urlString = urlDepthPair.getUrl();
      urlString = UrlNormalizer.normalize(urlString);
      if (urlString == null) {
        return false;
      }

      synchronized (queueFile) {
        try (RandomAccessFile file = new RandomAccessFile(queueFile, "rw")) {
          file.seek(currentPosition);
          urlPositionMap.put(urlDepthPair, currentPosition);
          file.writeBytes("U_" + urlDepthPair.getUrl() + " " + urlDepthPair.getDepth() + "\n");
          currentPosition = file.getFilePointer();
          queue.offer(urlDepthPair);
          uncrawledSet.add(urlDepthPair);
        }
      }

      return true;

    } catch (IOException e) {
      ConsoleColors.printError("PersistentQueue");
      System.err.println("Error writing to queue file: " + e.getMessage());
      return false;
    }
  }

  public UrlDepthPair poll(long timeout, TimeUnit unit) throws InterruptedException {
    UrlDepthPair urlDepthPair = queue.poll(timeout, unit);
    if (urlDepthPair == null) return null;

    try {
      synchronized (queueFile) {
        try (RandomAccessFile file = new RandomAccessFile(queueFile, "rw")) {
          file.seek(urlPositionMap.get(urlDepthPair));
          file.writeBytes("V_" + urlDepthPair.getUrl() + " " + urlDepthPair.getDepth() + "\n");
          uncrawledSet.remove(urlDepthPair);
        }
      }
    } catch (IOException e) {
      System.err.println("[Queue] Error writing to queue file: " + e.getMessage());
    }
    return urlDepthPair;
  }
}
