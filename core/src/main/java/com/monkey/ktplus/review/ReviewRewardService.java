package com.monkey.ktplus.review;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.scheduler.ScheduledHandle;
import com.monkey.ktplus.storage.repository.ReviewClaimRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class ReviewRewardService {
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,38}$");

    private final JavaPlugin plugin;
    private final PlatformScheduler scheduler;
    private final ReviewClaimRepository claims;
    private final EconomyService economy;
    private final ReviewApiClient api;
    private final ConcurrentHashMap<UUID, ReviewSession> sessionsByPlayer =
            new ConcurrentHashMap<UUID, ReviewSession>();
    private final ConcurrentHashMap<String, UUID> sessionsByAccount = new ConcurrentHashMap<String, UUID>();
    private final AtomicBoolean pollerRunning = new AtomicBoolean(false);
    private @Nullable ScheduledHandle pollHandle;

    public ReviewRewardService(
            JavaPlugin plugin,
            PlatformScheduler scheduler,
            ReviewClaimRepository claims,
            EconomyService economy,
            ConfigSnapshot config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.claims = Objects.requireNonNull(claims, "claims");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.api = new ReviewApiClient(plugin.getPluginMeta().getVersion());
        Objects.requireNonNull(config, "config");
    }

    public void reload(ConfigSnapshot config) {
        Objects.requireNonNull(config, "config");
    }

    public void shutdown() {
        if (pollHandle != null) {
            pollHandle.cancel();
            pollHandle = null;
        }
        pollerRunning.set(false);
        sessionsByPlayer.clear();
        sessionsByAccount.clear();
    }

    public void start(Player player, ReviewPlatform platform, String rawAccount) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(rawAccount, "rawAccount");

        ReviewSession existing = sessionsByPlayer.get(player.getUniqueId());
        if (existing != null) {
            if (!existing.expired(System.currentTimeMillis())) {
                ReviewMessages.busy(player, existing.remainingSeconds(System.currentTimeMillis()));
                return;
            }
            removeSession(existing);
        }

        String account = rawAccount.trim();
        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            ReviewMessages.invalidAccount(player);
            return;
        }
        String accountKey = account.toLowerCase(Locale.ROOT);

        if (claims.hasClaim(player.getUniqueId(), platform.id())) {
            ReviewMessages.alreadyClaimed(player);
            return;
        }
        if (claims.isAccountClaimed(platform.id(), accountKey)) {
            ReviewMessages.accountUsed(player);
            return;
        }

        String accountLock = accountLockKey(platform, accountKey);
        if (sessionsByAccount.putIfAbsent(accountLock, player.getUniqueId()) != null) {
            ReviewMessages.accountBusy(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        scheduler.runAsync(() -> {
            try {
                ReviewStats stats = fetchStats(platform);
                Presence presence = checkPresence(platform, account);
                scheduler.runGlobal(
                        () -> beginSession(playerId, platform, account, accountKey, stats, presence));
            } catch (Exception ex) {
                sessionsByAccount.remove(accountLock, playerId);
                scheduler.runGlobal(() -> {
                    Player online = Bukkit.getPlayer(playerId);
                    if (online != null) {
                        ReviewMessages.fetchFailed(online);
                    }
                });
                plugin.getLogger()
                        .warning("[Review] lookup failed: " + ex.getClass().getSimpleName() + " - "
                                + ex.getMessage());
            }
        });
    }

    private void beginSession(
            UUID playerId,
            ReviewPlatform platform,
            String account,
            String accountKey,
            ReviewStats stats,
            Presence presence) {
        Player player = Bukkit.getPlayer(playerId);
        String accountLock = accountLockKey(platform, accountKey);
        if (player == null || !player.isOnline()) {
            sessionsByAccount.remove(accountLock, playerId);
            return;
        }
        if (sessionsByPlayer.containsKey(playerId)) {
            sessionsByAccount.remove(accountLock, playerId);
            ReviewMessages.busy(player, 0L);
            return;
        }

        sendPitch(player, platform, stats);
        ReviewMessages.verifying(player, account, platform);
        
        if (presence.present()) {
            claims.claimIfAbsent(playerId, platform.id(), accountKey);
            sessionsByAccount.remove(accountLock, playerId);
            ReviewMessages.alreadyPresentLocked(player, platformLabel(platform));
            return;
        }

        long expiresAt = System.currentTimeMillis() + (ReviewConstants.SESSION_SECONDS * 1000L);
        ReviewSession session = new ReviewSession(playerId, platform, account, accountKey, expiresAt, false);
        sessionsByPlayer.put(playerId, session);
        ReviewMessages.sessionStarted(player, platform, account);
        ensurePoller();
    }

    private void ensurePoller() {
        if (!pollerRunning.compareAndSet(false, true)) {
            return;
        }
        pollHandle = scheduler.runAsyncLater(this::pollTick, ReviewConstants.POLL_TICKS);
    }

    private void pollTick() {
        try {
            List<ReviewSession> snapshot = new ArrayList<ReviewSession>(sessionsByPlayer.values());
            if (snapshot.isEmpty()) {
                pollerRunning.set(false);
                pollHandle = null;
                return;
            }

            long now = System.currentTimeMillis();
            for (ReviewSession session : snapshot) {
                if (session.expired(now)) {
                    finishTimeout(session);
                    continue;
                }
                try {
                    verifySession(session);
                } catch (Exception ex) {
                    plugin.getLogger()
                            .warning("[Review] verify failed for "
                                    + session.playerId()
                                    + ": "
                                    + ex.getClass().getSimpleName());
                }
            }
        } finally {
            if (sessionsByPlayer.isEmpty()) {
                pollerRunning.set(false);
                pollHandle = null;
            } else {
                pollHandle = scheduler.runAsyncLater(this::pollTick, ReviewConstants.POLL_TICKS);
            }
        }
    }

    private void verifySession(ReviewSession session) throws Exception {
        if (session.platform() == ReviewPlatform.GITHUB) {
            boolean starred = api.hasGitHubStar(
                    ReviewConstants.GITHUB_OWNER,
                    ReviewConstants.GITHUB_REPO,
                    session.accountName());
            if (starred) {
                finishSuccess(session, ReviewConstants.GITHUB_REWARD);
            }
            return;
        }

        OptionalInt stars = api.findSpigotReviewStars(ReviewConstants.SPIGOT_RESOURCE_ID, session.accountName());
        if (stars.isEmpty()) {
            return;
        }
        int rating = stars.getAsInt();
        int reward = ReviewConstants.spigotReward(rating);
        if (reward <= 0) {
            if (session.markLowRatingNotified()) {
                scheduler.runGlobal(() -> {
                    Player player = Bukkit.getPlayer(session.playerId());
                    if (player != null && sessionsByPlayer.containsKey(session.playerId())) {
                        ReviewMessages.spigotRatingTooLow(player, rating);
                    }
                });
            }
            return;
        }
        finishSuccess(session, reward);
    }

    private void finishTimeout(ReviewSession session) {
        if (!removeSession(session)) {
            return;
        }
        scheduler.runGlobal(() -> {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player != null) {
                ReviewMessages.timeout(player, platformLabel(session.platform()));
            }
        });
    }

    private void finishSuccess(ReviewSession session, int reward) {
        if (!removeSession(session)) {
            return;
        }
        boolean claimed = claims.claimIfAbsent(session.playerId(), session.platform().id(), session.accountKey());
        if (claimed && reward > 0 && economy.enabled()) {
            economy.addBalance(session.playerId(), reward);
        }
        final boolean rewarded = claimed;
        final int paid = claimed ? reward : 0;
        scheduler.runGlobal(() -> {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player == null) {
                return;
            }
            if (rewarded) {
                ReviewMessages.success(player, platformLabel(session.platform()), paid);
                ReviewMessages.broadcastRewardTeaser(player, session.platform(), paid);
            } else {
                ReviewMessages.alreadyClaimed(player);
            }
        });
    }

    private boolean removeSession(ReviewSession session) {
        ReviewSession removed = sessionsByPlayer.remove(session.playerId());
        if (removed == null) {
            return false;
        }
        sessionsByAccount.remove(accountLockKey(session.platform(), session.accountKey()), session.playerId());
        return true;
    }

    private ReviewStats fetchStats(ReviewPlatform platform) throws Exception {
        if (platform == ReviewPlatform.GITHUB) {
            return api.fetchGitHubStats(ReviewConstants.GITHUB_OWNER, ReviewConstants.GITHUB_REPO);
        }
        return api.fetchSpigotStats(ReviewConstants.SPIGOT_RESOURCE_ID);
    }

    private Presence checkPresence(ReviewPlatform platform, String account) throws Exception {
        if (platform == ReviewPlatform.GITHUB) {
            boolean starred = api.hasGitHubStar(
                    ReviewConstants.GITHUB_OWNER, ReviewConstants.GITHUB_REPO, account);
            return new Presence(starred, 0);
        }
        OptionalInt stars = api.findSpigotReviewStars(ReviewConstants.SPIGOT_RESOURCE_ID, account);
        return new Presence(stars.isPresent(), stars.orElse(0));
    }

    private void sendPitch(Player player, ReviewPlatform platform, ReviewStats stats) {
        if (platform == ReviewPlatform.GITHUB) {
            ReviewMessages.githubPitch(player, stats.count());
            return;
        }
        ReviewMessages.spigotPitch(player, stats.count(), stats.average());
    }

    private static String accountLockKey(ReviewPlatform platform, String accountKey) {
        return platform.id() + ":" + accountKey;
    }

    private static String platformLabel(ReviewPlatform platform) {
        return platform == ReviewPlatform.GITHUB ? "GitHub" : "SpigotMC";
    }

    private record Presence(boolean present, int stars) {}
}
