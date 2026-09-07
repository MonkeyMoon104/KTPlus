package com.monkey.ktplus.economy;

final class ExternalPurchaseCoordinator {
    private ExternalPurchaseCoordinator() {}

    static PurchaseResult afterFailedClaim(boolean alreadyOwned, long balance, int price) {
        if (alreadyOwned) {
            return PurchaseResult.ok();
        }
        return PurchaseResult.notEnough(balance, price);
    }

    static boolean mustRollbackClaimedPurchase(boolean claimSucceeded, boolean withdrawSucceeded) {
        return claimSucceeded && !withdrawSucceeded;
    }
}
