package com.crawler.report;

import com.crawler.http.HttpResult;

public class BrokenLinkDetector {

    public boolean isBroken(HttpResult result) {
        return result.hasError() || result.getStatusCode() >= 400;
    }

    public String describeProblem(HttpResult result) {
        if (result.hasError()) {
            return result.getError();
        }

        int status = result.getStatusCode();
        switch (status) {
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 410:
                return "Gone";
            case 429:
                return "Too Many Requests";
            case 500:
                return "Internal Server Error";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Timeout";
            default:
                if (status >= 500) {
                    return "Server Error";
                }
                return "Client Error";
        }
    }
}
