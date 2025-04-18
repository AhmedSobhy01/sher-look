package com.sherlook.search.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class UrlNormalizer {

  private static final Set<String> EXCLUDED_PARAMS;

  static {
    // Initialize the final HashSet in a static block
    Set<String> params = new HashSet<>();
    params.add("ref");
    params.add("fbclid");
    params.add("geo_filter");
    params.add("cId");
    params.add("iId");

    // Optional: wrap as unmodifiable to prevent accidental modification
    EXCLUDED_PARAMS = Collections.unmodifiableSet(params);
  }

  public static String normalize(String urlString) {

    try {
      URI uri = new URI(urlString);
      String scheme = uri.getScheme();

      if (scheme == null
          || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        return null; // Reject unsupported schemes like mailto:, ftp:, javascript:, etc.
      }

      URL url = uri.toURL();

      // Lowercase scheme and host
      scheme = url.getProtocol().toLowerCase();
      String host = url.getHost().toLowerCase();

      // Remove default port
      int port = url.getPort();
      if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))
        port = -1;

      // Remove fragment
      String path = url.getPath();
      if (path != null && path.endsWith("/")) path = path.substring(0, path.length() - 1);

      // Sort query parameters
      String query = url.getQuery();
      if (query != null) {
        String[] params = query.split("&");
        List<String> filtered = new ArrayList<>();
        for (String param : params) {
          String key = param.split("=")[0];
          if (!key.startsWith("utm_") && !EXCLUDED_PARAMS.contains(key)) {
            filtered.add(param);
          }
        }
        if (!filtered.isEmpty()) {
          Collections.sort(filtered);
          query = String.join("&", filtered);
        } else {
          query = null;
        }
      }

      String urlStr =
          scheme
              + "://"
              + host
              + (port != -1 ? ":" + port : "")
              + (path != null ? path : "")
              + (query != null ? "?" + query : "");
      URL normalizedUrl = new URI(urlStr).toURL();
      return normalizedUrl.toString();

    } catch (MalformedURLException | URISyntaxException e) {
      return null;
    }
  }

  public static boolean isAbsolute(String url) {
    return Pattern.compile("^[a-zA-Z][a-zA-Z0-9+-.]*://").matcher(url).find();
  }

  public static String resolve(String baseUrl, String relativeUrl) {
    try {
      URL resolved = ((new URI(baseUrl)).resolve(relativeUrl)).toURL();
      return resolved.toString();
    } catch (MalformedURLException | URISyntaxException e) {
      return null;
    }
  }
}
