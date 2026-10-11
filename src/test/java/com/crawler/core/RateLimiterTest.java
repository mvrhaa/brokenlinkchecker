package com.crawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void firstRequestDoesNotWait() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(500);

        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 200, "took " + elapsed + " ms");
    }

    @Test
    void requestsAreSpacedByTheDelay() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(100);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            limiter.acquire();
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 400, "took " + elapsed + " ms");
    }

    @Test
    void zeroDelayDoesNotWait() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(0);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            limiter.acquire();
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 200, "took " + elapsed + " ms");
    }

    @Test
    void delayIsSharedBetweenThreads() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(100);
        List<Long> times = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 6; i++) {
            executor.submit(() -> {
                try {
                    limiter.acquire();
                    times.add(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(6, times.size());
        Collections.sort(times);
        long total = times.get(5) - times.get(0);
        assertTrue(total >= 450, "6 requests took " + total + " ms");
    }
}
