package com.monkey.ktplus.effects.list.fireworks.animation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class FireworksMarkedTrackerTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static FireworksMarkedTracker newTracker() {
        TemporaryBlockService blocks = new TemporaryBlockService();
        return new FireworksMarkedTracker(blocks, (player, location) -> true);
    }

    @Test
    void startsWithoutActiveMarkedPlayers() {
        FireworksMarkedTracker tracker = newTracker();
        assertFalse(tracker.hasActiveMarkedPlayers());
        assertTrue(tracker.activeMarkedIds().isEmpty());
    }

    @Test
    void isPinnedReturnsFalseWhenNoPinsExist() {
        FireworksMarkedTracker tracker = newTracker();
        Location location = new Location(null, 10, 64, 20);
        assertFalse(tracker.isPinned(location));
    }

    @Test
    void releaseWaitsUntilAllPendingShotsComplete() {
        FireworksMarkedTracker tracker = newTracker();
        tracker.registerMarked(PLAYER_A);
        tracker.initializeFinale(3);

        tracker.completeShot(PLAYER_A);
        assertTrue(tracker.isMarked(PLAYER_A));
        tracker.completeShot(PLAYER_A);
        assertTrue(tracker.isMarked(PLAYER_A));
        tracker.completeShot(PLAYER_A);
        assertFalse(tracker.isMarked(PLAYER_A));
    }
}
