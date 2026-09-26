package com.monkey.ktplus.api.model;

/**
 * Outcome of an effect selection attempt via {@link
 * com.monkey.ktplus.api.service.AccessService#select}.
 *
 * @since 4.0.3
 * @see com.monkey.ktplus.api.event.EffectSelectEvent
 */
public enum SelectionResult {
    /** Effect was selected; player already had access. */
    SELECTED,
    /** Effect was purchased and then selected in one flow. */
    PURCHASED_AND_SELECTED,
    /** Selection denied due to missing permission. */
    DENIED_PERMISSION,
    /** Selection denied due to insufficient funds for required purchase. */
    DENIED_FUNDS
}
