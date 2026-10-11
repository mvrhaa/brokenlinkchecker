package com.crawler;

import com.crawler.config.ArgumentParser;
import com.crawler.config.CrawlerConfig;

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

        System.out.println("Start URL: " + config.getRootUrl());
        System.out.println("Max depth: " + config.getMaxDepth());
        System.out.println("Threads:   " + config.getThreads());
        System.out.println("Delay:     " + config.getDelayMillis() + " ms");
    }
}
