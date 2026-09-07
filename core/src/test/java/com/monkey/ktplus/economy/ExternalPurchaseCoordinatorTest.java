package com.monkey.ktplus.economy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExternalPurchaseCoordinatorTest {
    @Test
    void failedClaimReturnsNotEnoughWhenNotOwned() {
        assertFalse(ExternalPurchaseCoordinator.afterFailedClaim(false, 10L, 100).successful());
    }

    @Test
    void failedClaimReturnsOkWhenAlreadyOwned() {
        assertTrue(ExternalPurchaseCoordinator.afterFailedClaim(true, 10L, 100).successful());
    }

    @Test
    void rollbackRequiredWhenClaimSucceededButWithdrawFailed() {
        assertTrue(ExternalPurchaseCoordinator.mustRollbackClaimedPurchase(true, false));
    }

    @Test
    void noRollbackWhenWithdrawSucceeded() {
        assertFalse(ExternalPurchaseCoordinator.mustRollbackClaimedPurchase(true, true));
    }
}
