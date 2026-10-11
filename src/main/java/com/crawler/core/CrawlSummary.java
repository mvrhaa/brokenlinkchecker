package com.crawler.core;

public class CrawlSummary {

    private final int pagesCrawled;
    private final int linksChecked;
    private final long durationMillis;

    public CrawlSummary(int pagesCrawled, int linksChecked, long durationMillis) {
        this.pagesCrawled = pagesCrawled;
        this.linksChecked = linksChecked;
        this.durationMillis = durationMillis;
    }

    public int getPagesCrawled() {
        return pagesCrawled;
    }

    public int getLinksChecked() {
        return linksChecked;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
