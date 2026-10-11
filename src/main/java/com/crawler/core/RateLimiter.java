package com.crawler.core;

public class RateLimiter {

    private final long delayMillis;
    private long nextFreeTime = 0;

    public RateLimiter(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void acquire() throws InterruptedException {
        long waitMillis = reserveNextSlot();
        if (waitMillis > 0) {
            Thread.sleep(waitMillis);
        }
    }

    private synchronized long reserveNextSlot() {
        long now = System.currentTimeMillis();
        long slot = Math.max(now, nextFreeTime);
        nextFreeTime = slot + delayMillis;
        return slot - now;
    }
}
