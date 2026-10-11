package com.crawler.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class HttpChecker {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    public static final int MAX_REDIRECTS = 10;

    private static final String USER_AGENT = "BrokenLinkChecker/1.0";

    private final HttpClient client;
    private final Duration timeout;

    public HttpChecker() {
        this(DEFAULT_TIMEOUT);
    }

    public HttpChecker(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public HttpResult check(URI url) {
        HttpResult result = send("HEAD", url, false);
        if (result.isSuccess()) {
            return result;
        }
        return send("GET", url, false);
    }

    public HttpResult fetchPage(URI url) {
        return send("GET", url, true);
    }

    private HttpResult send(String method, URI startUrl, boolean readHtml) {
        URI url = startUrl;

        try {
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                HttpResponse<InputStream> response = client.send(buildRequest(method, url),
                        HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();

                Optional<String> location = response.headers().firstValue("Location");
                if (status >= 300 && status < 400 && location.isPresent()) {
                    response.body().close();
                    URI next = url.resolve(location.get().trim().replace(" ", "%20"));
                    if (next.getHost() == null) {
                        return HttpResult.failure(startUrl, "Redirect to invalid URL: " + location.get());
                    }
                    url = next;
                    continue;
                }

                return HttpResult.response(url, status, readBody(response, readHtml));
            }
            return HttpResult.failure(startUrl, "Too many redirects");
        } catch (HttpTimeoutException e) {
            return HttpResult.failure(startUrl, "Timeout");
        } catch (IOException e) {
            return HttpResult.failure(startUrl, "Connection failed: " + describe(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HttpResult.failure(startUrl, "Interrupted");
        } catch (IllegalArgumentException e) {
            return HttpResult.failure(startUrl, "Invalid URL");
        }
    }

    private HttpRequest buildRequest(String method, URI url) {
        return HttpRequest.newBuilder(url)
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private String readBody(HttpResponse<InputStream> response, boolean readHtml) throws IOException {
        try (InputStream stream = response.body()) {
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (readHtml && contentType.toLowerCase().contains("text/html")) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            return null;
        }
    }

    private String describe(IOException e) {
        if (e.getMessage() != null) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
