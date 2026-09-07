package com.monkey.ktplus.storage.driver;

import com.zaxxer.hikari.HikariCredentialsProvider;
import com.zaxxer.hikari.util.Credentials;

public final class SnapshotCredentialsProvider implements HikariCredentialsProvider {
    private final String username;
    private final String password;

    public SnapshotCredentialsProvider(String username, String password) {
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
    }

    @Override
    public Credentials getCredentials() {
        return new Credentials(username, password);
    }
}
