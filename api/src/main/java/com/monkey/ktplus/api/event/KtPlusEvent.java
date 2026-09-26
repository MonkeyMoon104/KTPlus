package com.monkey.ktplus.api.event;

import org.bukkit.event.Event;

/**
 * Base type for all KTPlus Bukkit events.
 *
 * <p>Events in this package are fired synchronously on the Bukkit main thread unless a subclass
 * constructs itself with {@link #KtPlusEvent(boolean) async = true}. Listen with standard Bukkit
 * {@code @EventHandler} methods.
 *
 * @since 4.0.3
 */
public abstract class KtPlusEvent extends Event {
    /**
     * Creates a synchronous event.
     */
    protected KtPlusEvent() {}

    /**
     * Creates an event that may be marked async for Bukkit's event bus.
     *
     * @param async {@code true} if handlers may run off the main thread (Bukkit async event)
     */
    protected KtPlusEvent(boolean async) {
        super(async);
    }
}
