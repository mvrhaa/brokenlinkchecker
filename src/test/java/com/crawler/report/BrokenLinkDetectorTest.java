package com.crawler.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.crawler.http.HttpResult;

class BrokenLinkDetectorTest {

    private final BrokenLinkDetector detector = new BrokenLinkDetector();
    private final URI url = URI.create("https://example.com/page");

    @Test
    void successAndRedirectStatusesAreNotBroken() {
        assertFalse(detector.isBroken(HttpResult.response(url, 200, null)));
        assertFalse(detector.isBroken(HttpResult.response(url, 204, null)));
        assertFalse(detector.isBroken(HttpResult.response(url, 304, null)));
        assertFalse(detector.isBroken(HttpResult.response(url, 399, null)));
    }

    @Test
    void status400AndAboveIsBroken() {
        assertTrue(detector.isBroken(HttpResult.response(url, 400, null)));
        assertTrue(detector.isBroken(HttpResult.response(url, 404, null)));
        assertTrue(detector.isBroken(HttpResult.response(url, 500, null)));
        assertTrue(detector.isBroken(HttpResult.response(url, 503, null)));
    }

    @Test
    void networkErrorsAreBroken() {
        assertTrue(detector.isBroken(HttpResult.failure(url, "Timeout")));
        assertTrue(detector.isBroken(HttpResult.failure(url, "Unknown host")));
        assertTrue(detector.isBroken(HttpResult.failure(url, "Too many redirects")));
    }

    @Test
    void describesCommonStatusCodes() {
        assertEquals("Not Found", detector.describeProblem(HttpResult.response(url, 404, null)));
        assertEquals("Forbidden", detector.describeProblem(HttpResult.response(url, 403, null)));
        assertEquals("Internal Server Error", detector.describeProblem(HttpResult.response(url, 500, null)));
        assertEquals("Service Unavailable", detector.describeProblem(HttpResult.response(url, 503, null)));
    }

    @Test
    void describesOtherStatusCodesByGroup() {
        assertEquals("Client Error", detector.describeProblem(HttpResult.response(url, 418, null)));
        assertEquals("Server Error", detector.describeProblem(HttpResult.response(url, 599, null)));
    }

    @Test
    void describesNetworkErrorsWithTheirMessage() {
        assertEquals("Timeout", detector.describeProblem(HttpResult.failure(url, "Timeout")));
        assertEquals("Too many redirects", detector.describeProblem(HttpResult.failure(url, "Too many redirects")));
    }
}
