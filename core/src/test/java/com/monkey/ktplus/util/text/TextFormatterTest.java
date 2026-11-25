package com.monkey.ktplus.util.text;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextFormatterTest {
    @Test
    void detectsMiniMessageTags() {
        assertTrue(TextFormatter.looksLikeMiniMessage("<gradient:#38bdf8:#a78bfa>Hello</gradient>"));
        assertTrue(TextFormatter.looksLikeMiniMessage("<#fbbf24>Search</#fbbf24>"));
        assertTrue(TextFormatter.looksLikeMiniMessage("<red>Hi</red>"));
        assertFalse(TextFormatter.looksLikeMiniMessage("&aLegacy only"));
        assertFalse(TextFormatter.looksLikeMiniMessage("plain"));
    }

    @Test
    void legacyStillColors() {
        String colored = TextFormatter.color("&aHello");
        assertTrue(colored.contains("Hello"));
        assertFalse(colored.contains("&a"));
    }
}
