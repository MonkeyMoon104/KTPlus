package com.monkey.ktplus.economy.balance;

import com.monkey.ktplus.economy.EconomyProviderType;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import java.util.Objects;
import java.util.logging.Logger;

public final class BalanceProviderFactory {
    private BalanceProviderFactory() {}

    public static BalanceProvider create(
            EconomyProviderType requested,
            KillCoinsRepository repository,
            Logger logger) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(logger, "logger");
        if (requested == EconomyProviderType.VAULT) {
            VaultBalanceProvider vault = VaultBalanceProvider.tryCreate(logger);
            if (vault.available()) {
                logger.info("[Economy] Using Vault balance provider.");
                return vault;
            }
            logger.warning("[Economy] provider=VAULT unavailable; falling back to KillCoins.");
        }
        logger.info("[Economy] Using KillCoins balance provider.");
        return new KillCoinsBalanceProvider(repository);
    }
}
