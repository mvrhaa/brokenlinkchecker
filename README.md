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

## Tests

```sh
./mvnw test
```
