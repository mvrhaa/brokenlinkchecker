package com.crawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.crawler.config.CrawlerConfig;
import com.crawler.http.HttpChecker;
import com.crawler.parser.LinkExtractor;
import com.sun.net.httpserver.HttpServer;

class WebCrawlerTest {

    private HttpServer server;
    private final Map<String, String> pages = new ConcurrentHashMap<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> requestTimes = Collections.synchronizedList(new ArrayList<>());
    private volatile long pageDelayMillis = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            requests.add(method + " " + path);
            requestTimes.add(System.currentTimeMillis());

            if (pageDelayMillis > 0) {
                try {
                    Thread.sleep(pageDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            String html = pages.get(path);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            if (html == null) {
                exchange.sendResponseHeaders(404, -1);
            } else if (method.equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void crawlsEveryPageOfTheSite() throws InterruptedException {
        pages.put("/", links("/a", "/b"));
        pages.put("/a", links("/c"));
        pages.put("/b", links());
        pages.put("/c", links());

        CrawlSummary summary = crawl(5, 4, 0);

        assertEquals(4, summary.getPagesCrawled());
        assertEquals(1, count("GET /"));
        assertEquals(1, count("GET /a"));
        assertEquals(1, count("GET /b"));
        assertEquals(1, count("GET /c"));
    }

    @Test
    void visitsEachPageOnlyOnceEvenWithLoopsAndDuplicates() throws InterruptedException {
        pages.put("/", links("/a", "/a", "/b", "/"));
        pages.put("/a", links("/b", "/", "/a"));
        pages.put("/b", links("/a", "/", "/b#top"));

        CrawlSummary summary = crawl(10, 8, 0);

        assertEquals(3, summary.getPagesCrawled());
        assertEquals(0, summary.getLinksChecked());
        assertEquals(3, requests.size());
    }

    @Test
    void stopsAtMaxDepth() throws InterruptedException {
        pages.put("/", links("/level1"));
        pages.put("/level1", links("/level2"));
        pages.put("/level2", links("/level3"));
        pages.put("/level3", links("/level4"));

        CrawlSummary summary = crawl(1, 4, 0);

        assertEquals(2, summary.getPagesCrawled());
        assertEquals(1, summary.getLinksChecked());
        assertEquals(1, count("GET /"));
        assertEquals(1, count("GET /level1"));
        assertEquals(1, count("HEAD /level2"));
        assertEquals(0, count("GET /level2"));
        assertEquals(0, count("HEAD /level3"));
        assertEquals(0, count("GET /level3"));
    }

    @Test
    void depthZeroOnlyReadsTheStartPage() throws InterruptedException {
        pages.put("/", links("/a", "/b"));
        pages.put("/a", links("/c"));
        pages.put("/b", links());

        CrawlSummary summary = crawl(0, 4, 0);

        assertEquals(1, summary.getPagesCrawled());
        assertEquals(2, summary.getLinksChecked());
        assertEquals(1, count("GET /"));
        assertEquals(1, count("HEAD /a"));
        assertEquals(1, count("HEAD /b"));
        assertEquals(0, count("HEAD /c"));
    }

    @Test
    void checksLinksToOtherSitesButDoesNotCrawlThem() throws InterruptedException {
        String otherSite = "http://127.0.0.1:" + server.getAddress().getPort();
        pages.put("/", "<a href='" + otherSite + "/external'>other site</a>");
        pages.put("/external", links("/external-child"));
        pages.put("/external-child", links());

        CrawlSummary summary = crawl(5, 4, 0);

        assertEquals(1, summary.getPagesCrawled());
        assertEquals(1, summary.getLinksChecked());
        assertEquals(1, count("HEAD /external"));
        assertEquals(0, count("GET /external"));
        assertEquals(0, count("HEAD /external-child"));
        assertEquals(0, count("GET /external-child"));
    }

    @Test
    void missingPagesDoNotStopTheCrawl() throws InterruptedException {
        pages.put("/", links("/missing", "/a"));
        pages.put("/a", links("/also-missing"));

        CrawlSummary summary = crawl(5, 4, 0);

        assertEquals(4, summary.getPagesCrawled());
        assertEquals(1, count("GET /a"));
    }

    @Test
    void finishesWhenStartPageCannotBeReached() {
        server.stop(0);

        CrawlSummary summary = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> crawl(3, 4, 0));

        assertEquals(1, summary.getPagesCrawled());
    }

    @Test
    void remembersVisitedUrls() throws InterruptedException {
        pages.put("/", links("/a"));
        pages.put("/a", links());

        WebCrawler crawler = newCrawler(3, 2, 0);
        crawler.crawl();

        assertEquals(2, crawler.getVisitedUrls().size());
        assertTrue(crawler.getVisitedUrls().contains(URI.create(siteUrl() + "/a")));
        assertFalse(crawler.getVisitedUrls().contains(URI.create(siteUrl() + "/b")));
    }

    @Test
    void waitsBetweenRequests() throws InterruptedException {
        pages.put("/", links("/a", "/b", "/c", "/d"));
        pages.put("/a", links());
        pages.put("/b", links());
        pages.put("/c", links());
        pages.put("/d", links());

        CrawlSummary summary = crawl(2, 4, 150);

        assertEquals(5, summary.getPagesCrawled());
        assertTrue(summary.getDurationMillis() >= 600, "took " + summary.getDurationMillis() + " ms");

        List<Long> times = new ArrayList<>(requestTimes);
        Collections.sort(times);
        for (int i = 1; i < times.size(); i++) {
            long gap = times.get(i) - times.get(i - 1);
            assertTrue(gap >= 100, "gap between requests was only " + gap + " ms");
        }
    }

    @Test
    void crawlsPagesInParallel() throws InterruptedException {
        pages.put("/", links("/p1", "/p2", "/p3", "/p4", "/p5", "/p6", "/p7", "/p8"));
        for (int i = 1; i <= 8; i++) {
            pages.put("/p" + i, links());
        }
        pageDelayMillis = 300;

        CrawlSummary summary = crawl(1, 8, 0);

        assertEquals(9, summary.getPagesCrawled());
        assertTrue(summary.getDurationMillis() < 1500,
                "9 pages of 300 ms each took " + summary.getDurationMillis() + " ms, so they did not run in parallel");
    }

    private CrawlSummary crawl(int maxDepth, int threads, long delayMillis) throws InterruptedException {
        return newCrawler(maxDepth, threads, delayMillis).crawl();
    }

    private WebCrawler newCrawler(int maxDepth, int threads, long delayMillis) {
        CrawlerConfig config = new CrawlerConfig(URI.create(siteUrl() + "/"), maxDepth, threads, delayMillis);
        return new WebCrawler(config, new HttpChecker(Duration.ofSeconds(5)), new LinkExtractor());
    }

    private String siteUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private String links(String... paths) {
        StringBuilder html = new StringBuilder("<html><body>");
        for (String path : paths) {
            html.append("<a href='").append(path).append("'>link</a>");
        }
        return html.append("</body></html>").toString();
    }

    private long count(String request) {
        return requests.stream().filter(request::equals).count();
    }
}
