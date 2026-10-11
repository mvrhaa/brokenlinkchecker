package com.crawler.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class ArgumentParser {

    public static final String USAGE = """
            Usage: java -jar broken-link-checker.jar <url> [options]

            Options:
              --depth <n>     how many links deep to crawl from the start page (default 2)
              --threads <n>   number of pages to crawl at the same time (default 8)
              --delay <ms>    pause between requests in milliseconds (default 200)
              --output <dir>  folder for the CSV report (default: current folder)
              --help          show this message

            Example:
              java -jar broken-link-checker.jar https://example.com --depth 3 --delay 500
            """;

    public CrawlerConfig parse(String[] args) {
        String url = null;
        int maxDepth = CrawlerConfig.DEFAULT_MAX_DEPTH;
        int threads = CrawlerConfig.DEFAULT_THREADS;
        long delayMillis = CrawlerConfig.DEFAULT_DELAY_MILLIS;
        Path outputDirectory = Path.of(".");

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--depth")) {
                maxDepth = (int) parseNumber(arg, valueAfter(args, i), 0, 50);
                i++;
            } else if (arg.equals("--threads")) {
                threads = (int) parseNumber(arg, valueAfter(args, i), 1, 64);
                i++;
            } else if (arg.equals("--delay")) {
                delayMillis = parseNumber(arg, valueAfter(args, i), 0, 60_000);
                i++;
            } else if (arg.equals("--output")) {
                outputDirectory = Path.of(valueAfter(args, i));
                i++;
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown option: " + arg);
            } else if (url == null) {
                url = arg;
            } else {
                throw new IllegalArgumentException("Only one URL can be given, but got: " + url + " and " + arg);
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("The URL to crawl is missing");
        }

        return new CrawlerConfig(parseUrl(url), maxDepth, threads, delayMillis, outputDirectory);
    }

    public boolean isHelpRequested(String[] args) {
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                return true;
            }
        }
        return false;
    }

    private String valueAfter(String[] args, int index) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException("Option " + args[index] + " needs a value");
        }
        return args[index + 1];
    }

    private long parseNumber(String option, String value, long min, long max) {
        long number;
        try {
            number = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option " + option + " needs a whole number, but got: " + value);
        }
        if (number < min || number > max) {
            throw new IllegalArgumentException(
                    "Option " + option + " must be between " + min + " and " + max + ", but got: " + value);
        }
        return number;
    }

    private URI parseUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Not a valid URL: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("The URL must start with http:// or https://, but got: " + url);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("The URL has no host: " + url);
        }
        return uri;
    }
}
