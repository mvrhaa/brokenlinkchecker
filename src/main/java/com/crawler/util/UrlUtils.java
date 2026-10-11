package com.crawler.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtils {

    private UrlUtils() {
    }

    public static URI toAbsolute(URI baseUrl, String href) {
        if (href == null) {
            return null;
        }

        String trimmed = href.trim().replace(" ", "%20");
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }

        try {
            URI absolute = baseUrl.resolve(new URI(trimmed));
            return normalize(absolute);
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    public static URI normalize(URI url) {
        if (!isHttp(url) || url.getHost() == null) {
            return null;
        }

        URI cleaned = url.normalize();
        String scheme = cleaned.getScheme().toLowerCase();
        String host = cleaned.getHost().toLowerCase();

        StringBuilder result = new StringBuilder();
        result.append(scheme).append("://").append(host);

        int port = cleaned.getPort();
        if (port != -1 && !isDefaultPort(scheme, port)) {
            result.append(":").append(port);
        }

        String path = cleaned.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        result.append(path);

        if (cleaned.getRawQuery() != null) {
            result.append("?").append(cleaned.getRawQuery());
        }

        return URI.create(result.toString());
    }

    public static boolean isHttp(URI url) {
        String scheme = url.getScheme();
        return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    public static boolean isSameHost(URI first, URI second) {
        if (first.getHost() == null || second.getHost() == null) {
            return false;
        }
        return first.getHost().equalsIgnoreCase(second.getHost());
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
    }
}
