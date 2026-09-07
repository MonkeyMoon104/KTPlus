package com.monkey.ktplus.util.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;
import com.monkey.ktplus.util.net.HttpRedirectSupport;

class HttpRedirectSupportTest {
    @Test
    void resolvesRelativeHttpsRedirect() {
        URI resolved = HttpRedirectSupport.resolveRedirect(
                URI.create("https://example.com/a/b"), "/c/d");
        assertEquals("https://example.com/c/d", resolved.toString());
    }

    @Test
    void rejectsNonHttpsRedirect() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HttpRedirectSupport.resolveRedirect(
                        URI.create("https://example.com"), "http://evil.example/"));
    }
}
