package com.monkey.ktplus.review;

public final class ReviewConstants {
    public static final String PLUGIN_NAME = "KTPlus";

    public static final int SESSION_SECONDS = 300;
    public static final long POLL_TICKS = 15L * 20L;

    public static final int GITHUB_REWARD = 500;
    public static final int SPIGOT_COINS_PER_STAR = 100;
    public static final int SPIGOT_MIN_STARS = 4;
    
    public static final int BROADCAST_TEASER_COINS = GITHUB_REWARD + (5 * SPIGOT_COINS_PER_STAR);

    public static final String GITHUB_OWNER = "MonkeyMoon104";
    public static final String GITHUB_REPO = "KTPlus";
    public static final String GITHUB_URL = "https://github.com/MonkeyMoon104/KTPlus";

    public static final int SPIGOT_RESOURCE_ID = 125998;
    public static final String SPIGOT_URL =
            "https://www.spigotmc.org/resources/%E2%AD%90-1-21-ktplus-enhanced-version-%E2%AD%90.125998/";

    private ReviewConstants() {}

    public static int spigotReward(int stars) {
        if (stars < SPIGOT_MIN_STARS) {
            return 0;
        }
        return Math.min(5, stars) * SPIGOT_COINS_PER_STAR;
    }
}
