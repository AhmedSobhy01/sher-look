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
  boolean intiallyEmpty;
  private long currentPosition = 0;
  private final int maxSize = 50000;

  public PersistentQueue(File queueFile, Set<String> visitedUrlsSet) throws IOException {
    this.queueFile = queueFile;

    if (queueFile.exists()) {
      try (RandomAccessFile file = new RandomAccessFile(queueFile, "r")) {
        String line;
        file.seek(0);

        ConsoleColors.printInfo("PersistentQueue");
        System.out.println("Loading queue from file: " + queueFile.getAbsolutePath());

        while (file.getFilePointer() < file.length()) {
          line = file.readLine();
          if (line != null && line.startsWith("U_")) {
            line = line.substring(2);
            int index = line.lastIndexOf(" ");
            if (index == -1) {
              continue;
            }
            String url = UrlNormalizer.normalize(line.substring(0, index));
            int depth = Integer.parseInt(line.substring(index + 1));
            if (url == null) {
              continue;
            }
            UrlDepthPair urlDepthPair = new UrlDepthPair(url, depth);
            uncrawledSet.add(urlDepthPair);
            queue.offer(urlDepthPair);
            urlPositionMap.put(urlDepthPair, currentPosition);
          } else if (line != null && line.startsWith("V_")) {
            line = line.substring(2);
            int index = line.lastIndexOf(" ");
            if (index == -1) {
              continue;
            }
            String url = UrlNormalizer.normalize(line.substring(0, index));
            int depth = Integer.parseInt(line.substring(index + 1));
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
      ConsoleColors.printInfo("PersistentQueue");
      System.out.println(
          "Queue file does not exist. Creating a new one: " + queueFile.getAbsolutePath());
      queueFile.getParentFile().mkdirs();
      queueFile.createNewFile();
    }

    intiallyEmpty = queue.isEmpty();

    if (intiallyEmpty) {
      ConsoleColors.printWarning("PersistentQueue");
      System.out.println("Queue is empty.");
    } else {
      ConsoleColors.printInfo("PersistentQueue");
      System.out.println("Queue loaded successfully. Size: " + queue.size());
      ConsoleColors.printInfo("PersistentQueue");
      System.out.println("Visited URLs loaded successfully. Size: " + visitedUrlsSet.size());
    }
  }

  public boolean isIntiallyEmpty() {
    return intiallyEmpty;
  }

  public boolean offer(UrlDepthPair urlDepthPair) {
    try {
      if (urlDepthPair == null || uncrawledSet.contains(urlDepthPair) || queue.size() >= maxSize)
        return false;

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
