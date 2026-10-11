package com.crawler.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class HttpCheckerTest {

    private HttpServer server;
    private HttpChecker checker;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/ok", exchange -> respond(exchange, 200, "text/html", "<a href='/next'>next</a>"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "text/html", "not found"));
        server.createContext("/error", exchange -> respond(exchange, 500, "text/html", "error"));
        server.createContext("/file.pdf", exchange -> respond(exchange, 200, "application/pdf", "pdf content"));
        server.createContext("/moved", exchange -> redirect(exchange, "/ok"));
        server.createContext("/moved-twice", exchange -> redirect(exchange, "/moved"));
        server.createContext("/moved-to-missing", exchange -> redirect(exchange, "/missing"));
        server.createContext("/loop-a", exchange -> redirect(exchange, "/loop-b"));
        server.createContext("/loop-b", exchange -> redirect(exchange, "/loop-a"));
        server.createContext("/no-head", exchange -> {
            if (exchange.getRequestMethod().equals("HEAD")) {
                respond(exchange, 405, "text/plain", "");
            } else {
                respond(exchange, 200, "text/html", "works with GET");
            }
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "text/html", "slow");
        });

        server.start();
        checker = new HttpChecker(Duration.ofMillis(500));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void workingLinkReturns200() {
        HttpResult result = checker.check(url("/ok"));

        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertFalse(result.hasError());
    }

    @Test
    void missingPageReturns404() {
        HttpResult result = checker.check(url("/missing"));

        assertEquals(404, result.getStatusCode());
        assertFalse(result.isSuccess());
        assertFalse(result.hasError());
    }

    @Test
    void serverErrorReturns500() {
        HttpResult result = checker.check(url("/error"));

        assertEquals(500, result.getStatusCode());
        assertFalse(result.isSuccess());
    }

    @Test
    void followsRedirectToWorkingPage() {
        HttpResult result = checker.check(url("/moved-twice"));

        assertEquals(200, result.getStatusCode());
        assertEquals(url("/ok"), result.getUrl());
    }

    @Test
    void followsRedirectToMissingPage() {
        HttpResult result = checker.check(url("/moved-to-missing"));

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void redirectLoopIsReportedAsError() {
        HttpResult result = checker.check(url("/loop-a"));

        assertTrue(result.hasError());
        assertEquals("Too many redirects", result.getError());
        assertFalse(result.isSuccess());
    }

    @Test
    void usesGetWhenServerDoesNotAllowHead() {
        HttpResult result = checker.check(url("/no-head"));

        assertEquals(200, result.getStatusCode());
    }

    @Test
    void slowServerIsReportedAsTimeout() {
        HttpResult result = checker.check(url("/slow"));

        assertTrue(result.hasError());
        assertEquals("Timeout", result.getError());
    }

    @Test
    void closedPortIsReportedAsConnectionFailure() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }

        HttpResult result = checker.check(URI.create("http://localhost:" + freePort + "/"));

        assertTrue(result.hasError());
        assertTrue(result.getError().startsWith("Connection failed"));
    }

    @Test
    void unknownHostIsReportedAsConnectionFailure() {
        HttpResult result = checker.check(URI.create("http://this-host-does-not-exist.invalid/"));

        assertTrue(result.hasError());
        assertFalse(result.isSuccess());
    }

    @Test
    void checkDoesNotDownloadTheBody() {
        HttpResult result = checker.check(url("/ok"));

        assertFalse(result.hasBody());
    }

    @Test
    void fetchPageReturnsHtml() {
        HttpResult result = checker.fetchPage(url("/ok"));

        assertEquals(200, result.getStatusCode());
        assertEquals("<a href='/next'>next</a>", result.getBody());
    }

    @Test
    void fetchPageFollowsRedirectAndReportsFinalUrl() {
        HttpResult result = checker.fetchPage(url("/moved"));

        assertEquals(200, result.getStatusCode());
        assertEquals(url("/ok"), result.getUrl());
        assertTrue(result.hasBody());
    }

    @Test
    void fetchPageSkipsBodyThatIsNotHtml() {
        HttpResult result = checker.fetchPage(url("/file.pdf"));

        assertEquals(200, result.getStatusCode());
        assertNull(result.getBody());
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (exchange.getRequestMethod().equals("HEAD") || bytes.length == 0) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
