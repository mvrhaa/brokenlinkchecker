# Broken Link Checker

A multithreaded web crawler that finds broken links, written in Java 17 with Maven.

## Build

You need JDK 17 or newer. Maven does not need to be installed.

```sh
./mvnw package
```

## Run

```sh
java -jar target/broken-link-checker.jar <url> [options]
```

| Option          | Meaning                                              | Default |
|-----------------|------------------------------------------------------|---------|
| `--depth <n>`   | How many links deep to crawl from the start page     | 2       |
| `--threads <n>` | Number of pages to crawl at the same time            | 8       |
| `--delay <ms>`  | Pause between requests in milliseconds               | 200     |
| `--help`        | Show the usage message                               |         |

Example:

```sh
java -jar target/broken-link-checker.jar https://example.com --depth 3 --delay 500
```

## How it works

- The start page is downloaded and its links are collected. Pages on the same site are crawled the same way until the chosen depth is reached.
- Links to other sites, and links found on the deepest pages, are only checked, not crawled.
- Pages are crawled in parallel by a thread pool, and every URL is visited only once.
- The delay is shared by all threads, so the whole program sends at most one request per delay period.

## Tests

```sh
./mvnw test
```
