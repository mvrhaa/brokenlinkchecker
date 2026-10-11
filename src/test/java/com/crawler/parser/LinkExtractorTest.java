package com.crawler.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

class LinkExtractorTest {

    private final LinkExtractor extractor = new LinkExtractor();
    private final URI pageUrl = URI.create("https://example.com/blog/post.html");

    @Test
    void extractsAbsoluteAndRelativeLinks() {
        String html = """
                <html><body>
                  <a href="https://other.org/page">absolute</a>
                  <a href="/about">root relative</a>
                  <a href="next.html">relative</a>
                  <a href="../index.html">parent</a>
                </body></html>
                """;

        List<URI> links = extractor.extractLinks(html, pageUrl);

        assertEquals(List.of(
                URI.create("https://other.org/page"),
                URI.create("https://example.com/about"),
                URI.create("https://example.com/blog/next.html"),
                URI.create("https://example.com/index.html")), links);
    }

    @Test
    void skipsLinksThatAreNotWebPages() {
        String html = """
                <a href="mailto:me@example.com">mail</a>
                <a href="tel:+123">phone</a>
                <a href="javascript:void(0)">script</a>
                <a href="#section">anchor</a>
                <a href="">empty</a>
                <a>no href</a>
                <a href="/real">real</a>
                """;

        List<URI> links = extractor.extractLinks(html, pageUrl);

        assertEquals(List.of(URI.create("https://example.com/real")), links);
    }

    @Test
    void removesDuplicates() {
        String html = """
                <a href="/about">one</a>
                <a href="/about#team">same page, other section</a>
                <a href="https://EXAMPLE.com/about">same page, other spelling</a>
                """;

        List<URI> links = extractor.extractLinks(html, pageUrl);

        assertEquals(List.of(URI.create("https://example.com/about")), links);
    }

    @Test
    void usesBaseTagWhenPresent() {
        String html = """
                <html><head><base href="https://cdn.example.com/assets/"></head>
                <body><a href="file.html">file</a></body></html>
                """;

        List<URI> links = extractor.extractLinks(html, pageUrl);

        assertEquals(List.of(URI.create("https://cdn.example.com/assets/file.html")), links);
    }

    @Test
    void ignoresOtherTagsWithUrls() {
        String html = """
                <img src="/logo.png">
                <link href="/style.css" rel="stylesheet">
                <script src="/app.js"></script>
                """;

        assertTrue(extractor.extractLinks(html, pageUrl).isEmpty());
    }

    @Test
    void handlesBrokenHtml() {
        String html = "<div><a href='/one'>one<p><a href=/two>two</div";

        List<URI> links = extractor.extractLinks(html, pageUrl);

        assertEquals(List.of(URI.create("https://example.com/one"), URI.create("https://example.com/two")), links);
    }

    @Test
    void emptyPageHasNoLinks() {
        assertTrue(extractor.extractLinks("", pageUrl).isEmpty());
    }
}
