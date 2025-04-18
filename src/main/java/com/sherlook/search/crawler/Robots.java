package com.sherlook.search.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Robots {
  private static Map<String, List<Pattern>> robotsDisallow = new HashMap<>();
  private static Map<String, List<Pattern>> robotsAllow = new HashMap<>();

  public static void fetchRobots(String url) {
    try {
      URL u = new URI(url).toURL();

      String baseUrl = u.getProtocol() + "://" + u.getHost();

      if (robotsDisallow.containsKey(baseUrl) || robotsAllow.containsKey(baseUrl)) {
        System.out.println("[Robots] Robots.txt already fetched for: " + baseUrl);
        return;
      }

      // Fetch the robots.txt file from baseUrl + "/robots.txt"
      String robotsUrl = baseUrl + "/robots.txt";
      System.out.println("[Robots] Fetching robots.txt from: " + robotsUrl);
      // Use Jsoup or any other library to fetch the robots.txt content
      // For example:

      URL robotsURL = new URI(robotsUrl).toURL();

      HttpURLConnection connection = (HttpURLConnection) robotsURL.openConnection();
      connection.setRequestMethod("GET");
      int statusCode = connection.getResponseCode();

      if (statusCode == HttpURLConnection.HTTP_OK) {
        List<Pattern> disallowedUrls = new ArrayList<>();
        List<Pattern> allowedUrls = new ArrayList<>();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("Disallow: ")) {
            String disallowedUrl = line.substring("Disallow: ".length()).trim();
            disallowedUrls.add(Pattern.compile(ruleToRegex(disallowedUrl)));
          } else if (line.startsWith("Allow: ")) {
            String allowedUrl = line.substring("Allow: ".length()).trim();
            allowedUrls.add(Pattern.compile(ruleToRegex(allowedUrl)));
          }
        }
        reader.close();
        robotsAllow.put(baseUrl, allowedUrls);
        robotsDisallow.put(baseUrl, disallowedUrls);
      }

      System.out.println("[Robots] Fetched robots.txt for: " + baseUrl);

    } catch (Exception e) {
      if (e instanceof java.io.FileNotFoundException) {
        System.out.println("[Robots] robots.txt not found for: " + url);
      } else {
        System.out.println("[Robots] Error fetching robots.txt for: " + url);
      }
      if (e instanceof java.net.UnknownHostException) {
        System.out.println("[Robots] Unknown host: " + url);
      } else if (e instanceof java.net.MalformedURLException) {
        System.out.println("[Robots] Malformed URL: " + url);
      } else if (e instanceof java.io.IOException) {
        System.out.println("[Robots] I/O error while fetching robots.txt for: " + url);
      } else {
        System.out.println("[Robots] Error fetching robots.txt for: " + url);
      }
      e.printStackTrace();
    }
  }

  public static boolean isAllowed(String url) {
    try {
      URL u = new URI(url).toURL();
      String baseUrl = u.getProtocol() + "://" + u.getHost();
      String path = u.getPath();
      if (path == "" || path == null) path = "/";

      if (!robotsDisallow.containsKey(baseUrl) && !robotsAllow.containsKey(baseUrl)) {
        fetchRobots(baseUrl);
      }

      if (!robotsDisallow.containsKey(baseUrl) && !robotsAllow.containsKey(baseUrl)) {
        return true; // No rules found, allow by default
      }

      List<Pattern> allowList = robotsAllow.get(baseUrl);
      List<Pattern> disallowList = robotsDisallow.get(baseUrl);

      boolean isAllowed = true; // Default if no rule matches
      int matchedLength = -1;

      for (Pattern pattern : disallowList) {
        if (pattern.matcher(path).matches()) {
          int length = pattern.pattern().length();
          if (length > matchedLength) {
            matchedLength = length;
            isAllowed = false;
          }
        }
      }

      for (Pattern pattern : allowList) {
        if (pattern.matcher(path).matches()) {
          int length = pattern.pattern().length();
          if (length > matchedLength) {
            matchedLength = length;
            isAllowed = true;
          }
        }
      }

      return isAllowed;

    } catch (Exception e) {
      e.printStackTrace();
      return true; // default to allowed on error
    }
  }

  private static String ruleToRegex(String rule) {
    // Escape regex special characters except * and $
    String escaped = rule.replaceAll("([\\\\.+?^{}()\\[\\]|])", "\\\\$1");

    // Replace * with .*
    escaped = escaped.replace("*", ".*");

    // If it ends with $, leave it, else ensure it matches prefix
    if (!escaped.endsWith("$")) {
      escaped = escaped + ".*";
    }
    return "^" + escaped;
  }
}
