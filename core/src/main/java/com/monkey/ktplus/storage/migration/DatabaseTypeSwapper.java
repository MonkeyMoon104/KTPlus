package com.monkey.ktplus.storage.migration;

@FunctionalInterface
public interface DatabaseTypeSwapper {
    void saveDatabaseType(String type) throws Exception;
}
