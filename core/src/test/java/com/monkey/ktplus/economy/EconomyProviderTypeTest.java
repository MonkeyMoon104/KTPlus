package com.monkey.ktplus.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EconomyProviderTypeTest {
    @Test
    void fromConfigDefaultsToKillCoins() {
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig(null));
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig(""));
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig("   "));
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig("unknown"));
    }

    @Test
    void fromConfigAcceptsVaultAliases() {
        assertEquals(EconomyProviderType.VAULT, EconomyProviderType.fromConfig("VAULT"));
        assertEquals(EconomyProviderType.VAULT, EconomyProviderType.fromConfig("vault"));
        assertEquals(EconomyProviderType.VAULT, EconomyProviderType.fromConfig("EXTERNAL"));
    }

    @Test
    void fromConfigAcceptsKillCoins() {
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig("KILLCOINS"));
        assertEquals(EconomyProviderType.KILLCOINS, EconomyProviderType.fromConfig("killcoins"));
    }
}
