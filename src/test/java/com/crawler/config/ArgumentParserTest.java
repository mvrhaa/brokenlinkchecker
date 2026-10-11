package com.crawler.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArgumentParserTest {

    private final ArgumentParser parser = new ArgumentParser();

    @Test
    void urlOnlyUsesDefaults() {
        CrawlerConfig config = parser.parse(new String[] {"https://example.com"});

        assertEquals("https://example.com", config.getRootUrl().toString());
        assertEquals(CrawlerConfig.DEFAULT_MAX_DEPTH, config.getMaxDepth());
        assertEquals(CrawlerConfig.DEFAULT_THREADS, config.getThreads());
        assertEquals(CrawlerConfig.DEFAULT_DELAY_MILLIS, config.getDelayMillis());
    }

    @Test
    void readsAllOptions() {
        CrawlerConfig config = parser.parse(new String[] {
                "https://example.com/docs", "--depth", "3", "--threads", "4", "--delay", "500"});

        assertEquals("https://example.com/docs", config.getRootUrl().toString());
        assertEquals(3, config.getMaxDepth());
        assertEquals(4, config.getThreads());
        assertEquals(500, config.getDelayMillis());
    }

    @Test
    void optionsCanComeBeforeTheUrl() {
        CrawlerConfig config = parser.parse(new String[] {"--depth", "1", "http://example.com"});

        assertEquals("http://example.com", config.getRootUrl().toString());
        assertEquals(1, config.getMaxDepth());
    }

    @Test
    void depthZeroIsAllowed() {
        CrawlerConfig config = parser.parse(new String[] {"https://example.com", "--depth", "0"});

        assertEquals(0, config.getMaxDepth());
    }

    @Test
    void missingUrlIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"--depth", "2"}));

        assertEquals("The URL to crawl is missing", e.getMessage());
    }

    @Test
    void urlWithoutHttpIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[] {"example.com"}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[] {"ftp://example.com"}));
    }

    @Test
    void brokenUrlIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[] {"https://exa mple.com"}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new String[] {"https://"}));
    }

    @Test
    void twoUrlsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "https://example.org"}));
    }

    @Test
    void unknownOptionIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--speed", "5"}));

        assertEquals("Unknown option: --speed", e.getMessage());
    }

    @Test
    void optionWithoutValueIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--depth"}));
    }

    @Test
    void notANumberIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--depth", "deep"}));
    }

    @Test
    void negativeNumbersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--depth", "-1"}));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--delay", "-5"}));
    }

    @Test
    void zeroThreadsIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new String[] {"https://example.com", "--threads", "0"}));
    }

    @Test
    void detectsHelpOption() {
        assertTrue(parser.isHelpRequested(new String[] {"--help"}));
        assertTrue(parser.isHelpRequested(new String[] {"https://example.com", "-h"}));
        assertFalse(parser.isHelpRequested(new String[] {"https://example.com"}));
    }
}
