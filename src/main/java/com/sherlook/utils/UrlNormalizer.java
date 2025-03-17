package com.sherlook.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public class UrlNormalizer {
    public static String normalize(String urlString) {
        try {
            URL url = (new URI(urlString)).toURL();

            // Lowercase scheme and host
            String scheme = url.getProtocol().toLowerCase();
            String host = url.getHost().toLowerCase();

            // Remove default port
            int port = url.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))
                port = -1;

            // Remove fragment
            String path = url.getPath();
            if (path != null && path.endsWith("/"))
                path = path.substring(0, path.length() - 1);

            // Sort query parameters
            String query = url.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                java.util.Arrays.sort(params);
                query = String.join("&", params);
            }
            
            String urlStr = scheme + "://" + host + (port != -1 ? ":" + port : "") + (path != null ? path : "") + (query != null ? "?" + query : "");
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