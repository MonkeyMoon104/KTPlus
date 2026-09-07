package com.monkey.ktplus.economy;

public final class PurchaseResult {
    private final boolean successful;
    private final long balance;
    private final long price;

    private PurchaseResult(boolean successful, long balance, long price) {
        this.successful = successful;
        this.balance = Math.max(0L, balance);
        this.price = Math.max(0L, price);
    }

    public static PurchaseResult ok() {
        return new PurchaseResult(true, 0L, 0L);
    }

    public static PurchaseResult denied() {
        return new PurchaseResult(false, 0L, 0L);
    }

    public static PurchaseResult notEnough(long balance, long price) {
        return new PurchaseResult(false, balance, price);
    }

    public boolean successful() {
        return successful;
    }

    public long balance() {
        return balance;
    }

    public long price() {
        return price;
    }
}
