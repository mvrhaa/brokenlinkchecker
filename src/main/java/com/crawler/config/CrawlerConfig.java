package com.crawler.config;

import java.net.URI;
import java.nio.file.Path;

public class CrawlerConfig {

    public static final int DEFAULT_MAX_DEPTH = 2;
    public static final int DEFAULT_THREADS = 8;
    public static final long DEFAULT_DELAY_MILLIS = 200;

    private final URI rootUrl;
    private final int maxDepth;
    private final int threads;
    private final long delayMillis;
    private final Path outputDirectory;

    public CrawlerConfig(URI rootUrl, int maxDepth, int threads, long delayMillis, Path outputDirectory) {
        this.rootUrl = rootUrl;
        this.maxDepth = maxDepth;
        this.threads = threads;
        this.delayMillis = delayMillis;
        this.outputDirectory = outputDirectory;
    }

    public URI getRootUrl() {
        return rootUrl;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getThreads() {
        return threads;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }
}
