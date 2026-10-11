package com.crawler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

class UrlUtilsTest {

    private final URI base = URI.create("https://example.com/docs/guide/index.html");

    @Test
    void keepsAbsoluteLink() {
        assertEquals("https://other.org/page", absolute("https://other.org/page"));
    }

    @Test
    void resolvesLinkRelativeToCurrentFolder() {
        assertEquals("https://example.com/docs/guide/setup.html", absolute("setup.html"));
    }

    @Test
    void resolvesLinkFromSiteRoot() {
        assertEquals("https://example.com/about", absolute("/about"));
    }

    @Test
    void resolvesParentFolderLink() {
        assertEquals("https://example.com/docs/faq.html", absolute("../faq.html"));
    }

    @Test
    void resolvesLinkWithoutScheme() {
        assertEquals("https://cdn.example.com/file", absolute("//cdn.example.com/file"));
    }

    @Test
    void removesFragment() {
        assertEquals("https://example.com/about", absolute("/about#team"));
    }

    @Test
    void keepsQueryString() {
        assertEquals("https://example.com/search?q=java&page=2", absolute("/search?q=java&page=2"));
    }

    @Test
    void encodesSpaces() {
        assertEquals("https://example.com/my%20file.html", absolute("/my file.html"));
    }

    @Test
    void ignoresLinksThatAreNotWebPages() {
        assertNull(UrlUtils.toAbsolute(base, "mailto:someone@example.com"));
        assertNull(UrlUtils.toAbsolute(base, "tel:+123456789"));
        assertNull(UrlUtils.toAbsolute(base, "javascript:void(0)"));
        assertNull(UrlUtils.toAbsolute(base, "ftp://example.com/file"));
    }

    @Test
    void ignoresEmptyAndFragmentOnlyLinks() {
        assertNull(UrlUtils.toAbsolute(base, ""));
        assertNull(UrlUtils.toAbsolute(base, "   "));
        assertNull(UrlUtils.toAbsolute(base, "#top"));
        assertNull(UrlUtils.toAbsolute(base, null));
    }

    @Test
    void ignoresLinksThatCannotBeParsed() {
        assertNull(UrlUtils.toAbsolute(base, "http://exa<mple.com"));
        assertNull(UrlUtils.toAbsolute(base, "https://"));
    }

    @Test
    void normalizeLowersSchemeAndHost() {
        assertEquals("https://example.com/Path", UrlUtils.normalize(URI.create("HTTPS://Example.COM/Path")).toString());
    }

    @Test
    void normalizeRemovesDefaultPortButKeepsOthers() {
        assertEquals("http://example.com/", UrlUtils.normalize(URI.create("http://example.com:80/")).toString());
        assertEquals("https://example.com/", UrlUtils.normalize(URI.create("https://example.com:443/")).toString());
        assertEquals("http://example.com:8080/", UrlUtils.normalize(URI.create("http://example.com:8080/")).toString());
    }

    @Test
    void normalizeAddsSlashToEmptyPath() {
        assertEquals("https://example.com/", UrlUtils.normalize(URI.create("https://example.com")).toString());
    }

    @Test
    void normalizeRemovesDotSegments() {
        assertEquals("https://example.com/a/c", UrlUtils.normalize(URI.create("https://example.com/a/b/../c")).toString());
        assertEquals("https://example.com/a/b", UrlUtils.normalize(URI.create("https://example.com/a/./b")).toString());
    }

    @Test
    void normalizeDoesNotDoubleEncode() {
        assertEquals("https://example.com/my%20file",
                UrlUtils.normalize(URI.create("https://example.com/my%20file")).toString());
    }

    @Test
    void sameUrlWrittenDifferentlyNormalizesToTheSameValue() {
        URI first = UrlUtils.normalize(URI.create("HTTP://Example.com:80/docs/../index.html#top"));
        URI second = UrlUtils.normalize(URI.create("http://example.com/index.html"));

        assertEquals(first, second);
    }

    @Test
    void comparesHosts() {
        assertTrue(UrlUtils.isSameHost(URI.create("https://example.com/a"), URI.create("http://EXAMPLE.com/b")));
        assertFalse(UrlUtils.isSameHost(URI.create("https://example.com/a"), URI.create("https://blog.example.com/")));
        assertFalse(UrlUtils.isSameHost(URI.create("https://example.com/a"), URI.create("https://other.org/")));
    }

    private String absolute(String href) {
        return UrlUtils.toAbsolute(base, href).toString();
    }
}
