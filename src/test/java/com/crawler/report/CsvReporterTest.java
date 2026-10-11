package com.crawler.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvReporterTest {

    private final CsvReporter reporter = new CsvReporter();

    @TempDir
    Path tempDir;

    @Test
    void writesHeaderAndOneLinePerBrokenLink() throws IOException {
        List<BrokenLink> links = List.of(
                new BrokenLink("https://example.com/", "https://example.com/missing", 404, "Not Found"),
                new BrokenLink("https://example.com/about", "https://slow.example.org/", 0, "Timeout"));

        Path file = reporter.write(links, tempDir);

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertEquals("Source Page,Broken Link,Status Code,Error Reason", lines.get(0));
        assertEquals("https://example.com/,https://example.com/missing,404,Not Found", lines.get(1));
        assertEquals("https://example.com/about,https://slow.example.org/,,Timeout", lines.get(2));
    }

    @Test
    void fileNameHasTimestamp() throws IOException {
        Path file = reporter.write(List.of(), tempDir);

        String name = file.getFileName().toString();
        assertTrue(name.matches("broken-links-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.csv"), name);
    }

    @Test
    void emptyReportHasOnlyTheHeader() throws IOException {
        Path file = reporter.write(List.of(), tempDir);

        assertEquals(List.of("Source Page,Broken Link,Status Code,Error Reason"), Files.readAllLines(file));
    }

    @Test
    void quotesValuesWithCommasAndQuotes() throws IOException {
        List<BrokenLink> links = List.of(
                new BrokenLink("https://example.com/a,b", "https://example.com/say\"hi\"", 404, "Not Found"));

        Path file = reporter.write(links, tempDir);

        assertEquals("\"https://example.com/a,b\",\"https://example.com/say\"\"hi\"\"\",404,Not Found",
                Files.readAllLines(file).get(1));
    }

    @Test
    void createsTheFolderWhenItDoesNotExist() throws IOException {
        Path folder = tempDir.resolve("reports").resolve("today");

        Path file = reporter.write(List.of(), folder);

        assertTrue(Files.exists(file));
        assertEquals(folder, file.getParent());
    }
}
