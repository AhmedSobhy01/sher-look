package com.sherlook.search.crawler;

import com.sherlook.search.utils.ConsoleColors;
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
        ConsoleColors.printInfo("Robots");
        System.out.println("Robots.txt already fetched for: " + baseUrl);
        return;
      }

      // Fetch the robots.txt file from baseUrl + "/robots.txt"
      String robotsUrl = baseUrl + "/robots.txt";
      ConsoleColors.printInfo("Robots");
      System.out.println("Fetching robots.txt from: " + robotsUrl);

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

      ConsoleColors.printSuccess("Robots");
      System.out.println("Fetched robots.txt for: " + baseUrl);

    } catch (Exception e) {

      ConsoleColors.printError("Robots");
      if (e instanceof java.io.FileNotFoundException) {
        System.out.print("robots.txt not found for: " + url);
      } else if (e instanceof java.net.UnknownHostException) {
        System.out.print("Unknown host: " + url);
      } else if (e instanceof java.net.MalformedURLException) {
        System.out.print("Malformed URL: " + url);
      } else if (e instanceof java.io.IOException) {
        System.out.print("I/O error while fetching robots.txt for: " + url);
      } else {
        System.out.print("Error fetching robots.txt for: " + url);
      }

      System.out.println(", Error: " + e.getMessage());
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
    // Escape all regex special characters except '*' and '$'
    String escaped = rule.replaceAll("([\\\\.\\+\\?\\^\\{\\}\\(\\)\\[\\]\\|])", "\\\\$1");

    // replace '*' with '.*'
    escaped = escaped.replace("*", ".*");

    if (!escaped.endsWith("$") && !escaped.endsWith(".*")) {
      escaped = escaped + ".*";
    }

    return "^" + escaped;
  }
}
