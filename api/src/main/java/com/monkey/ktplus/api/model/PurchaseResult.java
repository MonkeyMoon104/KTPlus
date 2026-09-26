package com.monkey.ktplus.api.model;

/**
 * Outcome of an effect purchase attempt.
 *
 * <p>Factory methods produce immutable instances. For {@link PurchaseStatus#INSUFFICIENT_FUNDS},
 * {@link #balance()} and {@link #price()} carry diagnostic amounts; other statuses typically leave
 * those at {@code 0}.
 *
 * @since 4.0.3
 * @see PurchaseStatus
 */
public final class PurchaseResult {
    private final PurchaseStatus status;
    private final long balance;
    private final long price;

    private PurchaseResult(PurchaseStatus status, long balance, long price) {
        this.status = status;
        this.balance = Math.max(0L, balance);
        this.price = Math.max(0L, price);
    }

    /**
     * Successful purchase (or no-op success when already owned / free, depending on caller).
     *
     * @return success result
     */
    public static PurchaseResult ok() {
        return new PurchaseResult(PurchaseStatus.SUCCESS, 0L, 0L);
    }

    /**
     * Purchase denied (permission, cancelled event, economy disabled policy, etc.).
     *
     * @return denied result
     */
    public static PurchaseResult denied() {
        return new PurchaseResult(PurchaseStatus.DENIED, 0L, 0L);
    }

    /**
     * Purchase failed because the player lacks funds.
     *
     * @param balance player's balance at denial time ({@code >= 0} after clamping)
     * @param price required price ({@code >= 0} after clamping)
     * @return insufficient-funds result
     */
    public static PurchaseResult notEnough(long balance, long price) {
        return new PurchaseResult(PurchaseStatus.INSUFFICIENT_FUNDS, balance, price);
    }

    /**
     * @return purchase status
     */
    public PurchaseStatus status() {
        return status;
    }

    /**
     * @return {@code true} if {@link #status()} is {@link PurchaseStatus#SUCCESS}
     */
    public boolean successful() {
        return status == PurchaseStatus.SUCCESS;
    }

    /**
     * Balance snapshot (meaningful for insufficient funds).
     *
     * @return balance ({@code >= 0})
     */
    public long balance() {
        return balance;
    }

    /**
     * Required price snapshot (meaningful for insufficient funds).
     *
     * @return price ({@code >= 0})
     */
    public long price() {
        return price;
    }
}
