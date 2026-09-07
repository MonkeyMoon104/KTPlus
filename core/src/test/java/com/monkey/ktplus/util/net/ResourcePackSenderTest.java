package com.monkey.ktplus.util.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import com.monkey.ktplus.util.net.ResourcePackSender;

final class ResourcePackSenderTest {
    @Test
    void sha1BytesParsesFortyHex() {
        byte[] bytes = ResourcePackSender.sha1Bytes("0123456789abcdef0123456789abcdef01234567");
        assertEquals(20, bytes.length);
        assertEquals(0x01, bytes[0]);
        assertEquals(0x67, bytes[19]);
    }

    @Test
    void sha1BytesRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ResourcePackSender.sha1Bytes("abc"));
        char[] invalid = new char[40];
        java.util.Arrays.fill(invalid, 'g');
        assertThrows(IllegalArgumentException.class, () -> ResourcePackSender.sha1Bytes(new String(invalid)));
    }
}
