package com.crawler.report;

public class BrokenLink {

    private final String sourcePage;
    private final String url;
    private final int statusCode;
    private final String reason;

    public BrokenLink(String sourcePage, String url, int statusCode, String reason) {
        this.sourcePage = sourcePage;
        this.url = url;
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReason() {
        return reason;
    }
}
