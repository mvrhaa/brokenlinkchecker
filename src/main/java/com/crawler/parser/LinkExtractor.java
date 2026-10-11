package com.crawler.parser;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.crawler.util.UrlUtils;

public class LinkExtractor {

    public List<URI> extractLinks(String html, URI pageUrl) {
        Document document = Jsoup.parse(html, pageUrl.toString());
        URI baseUrl = findBaseUrl(document, pageUrl);

        Set<URI> links = new LinkedHashSet<>();
        for (Element anchor : document.select("a[href]")) {
            URI link = UrlUtils.toAbsolute(baseUrl, anchor.attr("href"));
            if (link != null) {
                links.add(link);
            }
        }
        return new ArrayList<>(links);
    }

    private URI findBaseUrl(Document document, URI pageUrl) {
        Element base = document.selectFirst("base[href]");
        if (base == null) {
            return pageUrl;
        }

        URI baseUrl = UrlUtils.toAbsolute(pageUrl, base.attr("href"));
        if (baseUrl == null) {
            return pageUrl;
        }
        return baseUrl;
    }
}
