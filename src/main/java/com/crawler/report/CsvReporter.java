package com.crawler.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvReporter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String HEADER = "Source Page,Broken Link,Status Code,Error Reason";

    public Path write(List<BrokenLink> brokenLinks, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("broken-links-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (BrokenLink link : brokenLinks) {
                writer.write(escape(link.getSourcePage()));
                writer.write(",");
                writer.write(escape(link.getUrl()));
                writer.write(",");
                writer.write(link.getStatusCode() > 0 ? String.valueOf(link.getStatusCode()) : "");
                writer.write(",");
                writer.write(escape(link.getReason()));
                writer.newLine();
            }
        }
        return file;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
