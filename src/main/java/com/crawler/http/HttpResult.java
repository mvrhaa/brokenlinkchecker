package com.crawler.http;

import java.net.URI;

public class HttpResult {

    private final URI url;
    private final int statusCode;
    private final String body;
    private final String error;

    private HttpResult(URI url, int statusCode, String body, String error) {
        this.url = url;
        this.statusCode = statusCode;
        this.body = body;
        this.error = error;
    }

    public static HttpResult response(URI url, int statusCode, String body) {
        return new HttpResult(url, statusCode, body, null);
    }

    public static HttpResult failure(URI url, String error) {
        return new HttpResult(url, 0, null, error);
    }

    public boolean isSuccess() {
        return error == null && statusCode >= 200 && statusCode < 300;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean hasBody() {
        return body != null;
    }

    public URI getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public String getError() {
        return error;
    }
}
