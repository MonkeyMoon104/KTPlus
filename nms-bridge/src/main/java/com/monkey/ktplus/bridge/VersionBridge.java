package com.monkey.ktplus.bridge;

public interface VersionBridge {
    String id();

    boolean supports(String minecraftVersion);

    String particle(String key);

    String sound(String key);

    String material(String key);

    String entity(String key);
}
