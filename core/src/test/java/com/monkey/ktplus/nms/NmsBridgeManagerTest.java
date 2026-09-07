package com.monkey.ktplus.nms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NmsBridgeManagerTest {
    @ParameterizedTest
    @CsvSource({
        "1.21.11, com.monkey.ktplus.nms.NMSBridge_v1_21_11",
        "26.2, com.monkey.ktplus.nms.NMSBridge_v26_2",
        "1.20.4, ",
        "1.8.8, ",
        "unknown, ",
    })
    void resolveBridgeClassName(String version, String expected) {
        if (expected == null || expected.isEmpty()) {
            assertNull(NmsBridgeManager.resolveBridgeClassName(version));
        } else {
            assertEquals(expected, NmsBridgeManager.resolveBridgeClassName(version));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "1.21.11",
        "26.2",
    })
    void supportedVersionsContains(String version) {
        assertNotNull(NmsBridgeManager.getSupportedVersions());
        assertNotNull(NmsBridgeManager.resolveBridgeClassName(version));
    }
}
