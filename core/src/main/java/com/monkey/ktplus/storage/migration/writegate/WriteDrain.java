package com.monkey.ktplus.storage.migration.writegate;

import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.repository.TemporaryBlockRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

public final class WriteDrain {
    private WriteDrain() {}

    public static void drain(
            DatabaseService database,
            @Nullable TemporaryBlockRepository temporaryBlocks,
            Duration timeout)
            throws InterruptedException {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(timeout, "timeout");
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        if (temporaryBlocks != null) {
            long remainingMs = remainingMillis(deadlineNanos);
            if (remainingMs <= 0L) {
                throw new InterruptedException("write drain timed out before temp-block queue");
            }
            try {
                temporaryBlocks.drainPendingWrites(remainingMs, TimeUnit.MILLISECONDS);
            } catch (Exception error) {
                throw new InterruptedException("temp-block drain failed: " + error.getMessage());
            }
        }

        while (database.inFlightWrites() > 0) {
            if (System.nanoTime() >= deadlineNanos) {
                throw new InterruptedException(
                        "write drain timed out with " + database.inFlightWrites() + " in-flight writes");
            }
            Thread.sleep(25L);
        }
    }

    private static long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            return 0L;
        }
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }
}
