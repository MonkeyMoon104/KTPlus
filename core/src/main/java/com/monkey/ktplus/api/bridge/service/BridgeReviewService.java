package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.ReviewService;
import com.monkey.ktplus.review.ReviewRewardService;
import org.jspecify.annotations.Nullable;

public final class BridgeReviewService implements ReviewService {
    private volatile @Nullable ReviewRewardService reviews;

    public BridgeReviewService(@Nullable ReviewRewardService reviews) {
        this.reviews = reviews;
    }

    public void reload(@Nullable ReviewRewardService reviews) {
        this.reviews = reviews;
    }

    @Override
    public boolean available() {
        return reviews != null;
    }
}
