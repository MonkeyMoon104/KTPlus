package com.monkey.ktplus.review;

public final class ReviewStats {
    private final long count;
    private final double average;

    public ReviewStats(long count, double average) {
        this.count = Math.max(0L, count);
        this.average = Math.max(0.0D, average);
    }

    public long count() {
        return count;
    }

    public double average() {
        return average;
    }
}
