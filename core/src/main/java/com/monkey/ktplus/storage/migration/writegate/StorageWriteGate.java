package com.monkey.ktplus.storage.migration.writegate;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StorageWriteGate {
    private final AtomicBoolean writesFrozen = new AtomicBoolean(false);

    public void freezeWrites() {
        writesFrozen.set(true);
    }

    public void unfreezeWrites() {
        writesFrozen.set(false);
    }

    public boolean writesFrozen() {
        return writesFrozen.get();
    }

    public void requireWritable() {
        if (writesFrozen.get()) {
            throw new StorageWriteFrozenException();
        }
    }
}
