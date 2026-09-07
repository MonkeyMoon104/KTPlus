package com.monkey.ktplus.storage.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KillCoinsPurchaseCoordinatorTest {
    @Test
    void rollbackWhenClaimSucceededButWithdrawFailed() {
        assertTrue(KillCoinsPurchaseCoordinator.mustRollbackClaimedPurchase(true, false, 100L));
    }

    @Test
    void noRollbackWhenWithdrawSucceeded() {
        assertFalse(KillCoinsPurchaseCoordinator.mustRollbackClaimedPurchase(true, true, 100L));
    }

    @Test
    void noRollbackForFreeClaim() {
        assertFalse(KillCoinsPurchaseCoordinator.mustRollbackClaimedPurchase(true, false, 0L));
    }

    @Test
    void purchaseOkWhenAlreadyOwned() {
        assertTrue(KillCoinsPurchaseCoordinator.purchaseSucceeded(true, false, false, 100L));
    }

    @Test
    void purchaseOkWhenClaimedAndWithdrawSucceeded() {
        assertTrue(KillCoinsPurchaseCoordinator.purchaseSucceeded(false, true, true, 100L));
    }

    @Test
    void purchaseFailsWhenClaimLost() {
        assertFalse(KillCoinsPurchaseCoordinator.purchaseSucceeded(false, false, false, 100L));
    }

    @Test
    void purchaseFailsWhenWithdrawFailedAfterClaim() {
        assertFalse(KillCoinsPurchaseCoordinator.purchaseSucceeded(false, true, false, 100L));
    }

    @Test
    void freeClaimSucceedsWithoutWithdraw() {
        assertTrue(KillCoinsPurchaseCoordinator.purchaseSucceeded(false, true, false, 0L));
    }
}
