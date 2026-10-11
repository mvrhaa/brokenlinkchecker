package com.crawler.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.crawler.config.CrawlerConfig;
import com.crawler.http.HttpChecker;
import com.crawler.http.HttpResult;
import com.crawler.parser.LinkExtractor;
import com.crawler.report.BrokenLink;
import com.crawler.report.BrokenLinkDetector;
import com.crawler.util.UrlUtils;

public class WebCrawler {

    private final CrawlerConfig config;
    private final HttpChecker httpChecker;
    private final LinkExtractor linkExtractor;
    private final RateLimiter rateLimiter;
    private final BrokenLinkDetector detector = new BrokenLinkDetector();

    private final Set<URI> visited = ConcurrentHashMap.newKeySet();
    private final Map<URI, Set<URI>> sourcePages = new ConcurrentHashMap<>();
    private final Queue<HttpResult> brokenResults = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingTasks = new AtomicInteger();
    private final AtomicInteger pagesCrawled = new AtomicInteger();
    private final AtomicInteger linksChecked = new AtomicInteger();
    private final CountDownLatch finished = new CountDownLatch(1);

    private ExecutorService executor;
    private volatile URI siteUrl;

    public WebCrawler(CrawlerConfig config, HttpChecker httpChecker, LinkExtractor linkExtractor) {
        this.config = config;
        this.httpChecker = httpChecker;
        this.linkExtractor = linkExtractor;
        this.rateLimiter = new RateLimiter(config.getDelayMillis());
    }

    public CrawlSummary crawl() throws InterruptedException {
        long start = System.currentTimeMillis();
        executor = Executors.newFixedThreadPool(config.getThreads());

        try {
            URI rootUrl = UrlUtils.normalize(config.getRootUrl());
            siteUrl = rootUrl;
            visited.add(rootUrl);
            submit(() -> crawlPage(rootUrl, 0));
            finished.await();
        } finally {
            executor.shutdownNow();
        }

        long duration = System.currentTimeMillis() - start;
        return new CrawlSummary(pagesCrawled.get(), linksChecked.get(), duration, collectBrokenLinks());
    }

    public Set<URI> getVisitedUrls() {
        return Set.copyOf(visited);
    }

    private void submit(Runnable work) {
        pendingTasks.incrementAndGet();
        executor.submit(() -> {
            try {
                work.run();
            } catch (RuntimeException e) {
                System.err.println("Unexpected error: " + e);
            } finally {
                if (pendingTasks.decrementAndGet() == 0) {
                    finished.countDown();
                }
            }
        });
    }

    private void crawlPage(URI url, int depth) {
        if (!waitForTurn()) {
            return;
        }

        HttpResult result = httpChecker.fetchPage(url);
        pagesCrawled.incrementAndGet();
        record(result, url);

        if (depth == 0 && !result.hasError()) {
            siteUrl = result.getUrl();
        }
        if (!result.isSuccess() || !result.hasBody() || !UrlUtils.isSameHost(result.getUrl(), siteUrl)) {
            return;
        }

        visited.add(result.getUrl());
        List<URI> links = linkExtractor.extractLinks(result.getBody(), result.getUrl());
        for (URI link : links) {
            sourcePages.computeIfAbsent(link, key -> ConcurrentHashMap.newKeySet()).add(result.getUrl());
            if (!visited.add(link)) {
                continue;
            }

            if (UrlUtils.isSameHost(link, siteUrl) && depth < config.getMaxDepth()) {
                submit(() -> crawlPage(link, depth + 1));
            } else {
                submit(() -> checkLink(link));
            }
        }
    }

    private void checkLink(URI url) {
        if (!waitForTurn()) {
            return;
        }

        HttpResult result = httpChecker.check(url);
        linksChecked.incrementAndGet();
        record(result, url);
    }

    private void record(HttpResult result, URI requestedUrl) {
        if (detector.isBroken(result)) {
            brokenResults.add(HttpResult.copyFor(requestedUrl, result));
        }
        printResult(result, requestedUrl);
    }

    private List<BrokenLink> collectBrokenLinks() {
        List<BrokenLink> brokenLinks = new ArrayList<>();

        for (HttpResult result : brokenResults) {
            String reason = detector.describeProblem(result);
            Set<URI> sources = sourcePages.get(result.getUrl());

            if (sources == null) {
                brokenLinks.add(new BrokenLink("(start URL)", result.getUrl().toString(),
                        result.getStatusCode(), reason));
                continue;
            }
            for (URI source : sources) {
                brokenLinks.add(new BrokenLink(source.toString(), result.getUrl().toString(),
                        result.getStatusCode(), reason));
            }
        }

        brokenLinks.sort(Comparator.comparing(BrokenLink::getSourcePage).thenComparing(BrokenLink::getUrl));
        return brokenLinks;
    }

    private boolean waitForTurn() {
        try {
            rateLimiter.acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void printResult(HttpResult result, URI url) {
        if (result.hasError()) {
            System.out.println("[ERR] " + url + " (" + result.getError() + ")");
        } else {
            System.out.println("[" + result.getStatusCode() + "] " + url);
        }
    }
}
