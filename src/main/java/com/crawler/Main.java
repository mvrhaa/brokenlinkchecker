package com.crawler;

import com.crawler.config.ArgumentParser;
import com.crawler.config.CrawlerConfig;
import com.crawler.core.CrawlSummary;
import com.crawler.core.WebCrawler;
import com.crawler.http.HttpChecker;
import com.crawler.parser.LinkExtractor;

public class Main {

    public static void main(String[] args) {
        ArgumentParser parser = new ArgumentParser();

        if (args.length == 0 || parser.isHelpRequested(args)) {
            System.out.println(ArgumentParser.USAGE);
            return;
        }

        CrawlerConfig config;
        try {
            config = parser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(ArgumentParser.USAGE);
            System.exit(1);
            return;
        }

        System.out.println("Crawling " + config.getRootUrl()
                + " (depth " + config.getMaxDepth()
                + ", " + config.getThreads() + " threads"
                + ", " + config.getDelayMillis() + " ms between requests)");
        System.out.println();

        WebCrawler crawler = new WebCrawler(config, new HttpChecker(), new LinkExtractor());
        try {
            CrawlSummary summary = crawler.crawl();
            System.out.println();
            System.out.println("Pages crawled: " + summary.getPagesCrawled());
            System.out.println("Links checked: " + summary.getLinksChecked());
            System.out.println("Time:          " + summary.getDurationMillis() / 1000.0 + " s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawl was interrupted");
            System.exit(1);
        }
    }
}
