package com.monkey.ktplus.api.model;

/**
 * Status codes for {@link PurchaseResult}.
 *
 * @since 4.0.3
 */
public enum PurchaseStatus {
    /** Purchase completed successfully. */
    SUCCESS,
    /** Purchase blocked (permission, cancelled pre-event, policy, etc.). */
    DENIED,
    /** Player balance was below the required price. */
    INSUFFICIENT_FUNDS
}
