package com.crawler.core;

import java.util.List;

import com.crawler.report.BrokenLink;

public class CrawlSummary {

    private final int pagesCrawled;
    private final int linksChecked;
    private final long durationMillis;
    private final List<BrokenLink> brokenLinks;

    public CrawlSummary(int pagesCrawled, int linksChecked, long durationMillis, List<BrokenLink> brokenLinks) {
        this.pagesCrawled = pagesCrawled;
        this.linksChecked = linksChecked;
        this.durationMillis = durationMillis;
        this.brokenLinks = brokenLinks;
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

    public List<BrokenLink> getBrokenLinks() {
        return brokenLinks;
    }
}
