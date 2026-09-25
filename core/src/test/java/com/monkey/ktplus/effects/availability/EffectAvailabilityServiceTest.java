package com.monkey.ktplus.effects.availability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EffectAvailabilityServiceTest {
    @TempDir
    File tempDir;

    @Test
    void disableEnableRoundTripPersists() {
        EffectAvailabilityService service = newService();
        assertEquals(EffectAvailabilityService.DisableResult.UNKNOWN, service.disable("cloud", false));
        assertEquals(EffectAvailabilityService.DisableResult.DISABLED, service.disable("cloud", true));
        assertTrue(service.isDisabled("cloud"));
        assertEquals(EffectAvailabilityService.DisableResult.ALREADY_DISABLED, service.disable("cloud", true));

        assertEquals(EffectAvailabilityService.EnableResult.ENABLED, service.enable("cloud", true));
        assertTrue(service.isEnabled("cloud"));
        assertEquals(EffectAvailabilityService.EnableResult.ALREADY_ENABLED, service.enable("cloud", true));

        service.disable("smoke", true);
        EffectAvailabilityService again = newService();
        assertTrue(again.isDisabled("smoke"));
        assertEquals(List.of("smoke"), List.copyOf(again.disabledIds()));
    }

    @Test
    void filtersEnabledAndDisabledIds() {
        EffectAvailabilityService service = newService();
        service.disable("cloud", true);
        service.disable("smoke", true);
        assertEquals(List.of("hearts"), List.copyOf(service.filterEnabled(List.of("cloud", "hearts", "smoke"))));
        assertEquals(List.of("cloud", "smoke"), List.copyOf(service.filterDisabled(List.of("cloud", "hearts", "smoke"))));
        assertFalse(service.isEnabled("CLOUD"));
    }

    private EffectAvailabilityService newService() {
        return new EffectAvailabilityService(tempDir, Logger.getLogger("availability-test"));
    }
}
