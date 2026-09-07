package com.monkey.ktplus.storage.repository;

final class KillCoinsPurchaseCoordinator {
    private KillCoinsPurchaseCoordinator() {}

    static boolean mustRollbackClaimedPurchase(boolean newlyClaimed, boolean withdrawSucceeded, long price) {
        return newlyClaimed && price > 0L && !withdrawSucceeded;
    }

    static boolean purchaseSucceeded(boolean alreadyOwned, boolean newlyClaimed, boolean withdrawSucceeded, long price) {
        if (alreadyOwned) {
            return true;
        }
        if (!newlyClaimed) {
            return false;
        }
        return price <= 0L || withdrawSucceeded;
    }
}
