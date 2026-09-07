package com.monkey.ktplus.storage.migration.writegate;

import com.monkey.ktplus.storage.StorageException;

public final class StorageWriteFrozenException extends StorageException {
    private static final long serialVersionUID = 1L;

    public StorageWriteFrozenException() {
        super("Storage writes are frozen during database migration");
    }
}
