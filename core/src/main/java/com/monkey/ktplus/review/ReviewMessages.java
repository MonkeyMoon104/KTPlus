package com.monkey.ktplus.review;

import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.text.TextFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReviewMessages {
    private static final String PREFIX = "&8[&bKT+&8] &7";
    private static final String SOUND_OBJECTIVE_COMPLETE = "ui.toast.challenge_complete";
    private static final String SOUND_BROADCAST_EXP = "entity.experience_orb.pickup";

    private ReviewMessages() {}

    public static void usage(CommandSender sender) {
        send(sender, "&eUsage: &f/kt review <github|spigotmc> <platform-username>");
        send(sender, "&7Use your &fGitHub &7or &fSpigotMC &7username — &cnot &7your Minecraft name.");
    }

    public static void playerOnly(CommandSender sender) {
        send(sender, "&cOnly players can use this command.");
    }

    public static void busy(Player player, long secondsLeft) {
        send(player, "&cYou already have an active review session (&f" + secondsLeft + "s &cleft).");
    }

    public static void invalidAccount(Player player) {
        send(player, "&cInvalid username. Enter your real &fGitHub &cor &fSpigotMC &cusername, not Minecraft.");
    }

    public static void alreadyClaimed(Player player) {
        send(player, "&eYou already claimed this reward. Thanks for supporting KT+!");
    }

    public static void accountUsed(Player player) {
        send(player, "&cThat account was already used for a KT+ reward.");
    }

    public static void accountBusy(Player player) {
        send(player, "&cThat account is already being verified by another player.");
    }

    public static void fetchFailed(Player player) {
        send(player, "&cCould not reach GitHub/Spigot right now. Try again in a moment.");
    }

    public static void githubPitch(Player player, long stars) {
        send(player, "&7GitHub stars right now: &f" + stars);
        send(player, "&aStar &fKTPlus &aon GitHub within &f"
                + ReviewConstants.SESSION_SECONDS
                + "s &aand get &f"
                + ReviewConstants.GITHUB_REWARD
                + " free coins &ainstantly!");
        send(player, "&eImportant: &7the username in the command must be your &fGitHub username&7, &cnot &7your Minecraft name.");
        sendLink(player, "Open GitHub:", ReviewConstants.GITHUB_URL);
    }

    public static void spigotPitch(Player player, long reviews, double average) {
        send(player, "&7Spigot reviews: &f" + reviews + " &7| avg rating: &f"
                + String.format(java.util.Locale.ROOT, "%.1f", average) + "&7/5");
        send(player, "&aLeave a &f4+ star &areview on SpigotMC within &f"
                + ReviewConstants.SESSION_SECONDS
                + "s &aand earn free coins instantly!");
        send(player, "&7Rewards: &f5★ = "
                + (5 * ReviewConstants.SPIGOT_COINS_PER_STAR)
                + " coins&7, &f4★ = "
                + (4 * ReviewConstants.SPIGOT_COINS_PER_STAR)
                + " coins&7.");
        send(player, "&eImportant: &7the username in the command must be your &fSpigotMC username&7, &cnot &7your Minecraft name.");
        sendLink(player, "Open SpigotMC:", ReviewConstants.SPIGOT_URL);
    }

    public static void verifying(Player player, String account, ReviewPlatform platform) {
        send(player, "&7Checking &f" + platformUsernameLabel(platform) + " &7account: &f" + account);
        send(player, "&7If this is wrong, wait for the timer to end — Minecraft names will &cnot &7verify.");
    }

    public static void alreadyPresentLocked(Player player, String platformLabel) {
        send(player, "&eThat " + platformLabel
                + " account already has a star/review. Reward locked to stop duplicates.");
    }

    public static void sessionStarted(Player player, ReviewPlatform platform, String account) {
        send(player, "&aTimer started! You have &f"
                + ReviewConstants.SESSION_SECONDS
                + "s &ato complete the action on &f"
                + platformLabel(platform)
                + " &awith account &f"
                + account
                + "&a.");
        send(player, "&7Reminder: verification uses your &f"
                + platformUsernameLabel(platform)
                + "&7, not your Minecraft IGN.");
    }

    public static void success(Player player, String platformLabel, int reward) {
        send(player, "&aThanks for supporting KT+ on &f" + platformLabel
                + "&a! You received &f" + reward + " free coins&a.");
        EntityCompat.playSound(player, player.getLocation(), SOUND_OBJECTIVE_COMPLETE, 1.0f, 1.0f);
    }

    public static void broadcastRewardTeaser(Player rewarded, ReviewPlatform platform, int coinsReceived) {
        String command = suggestCommand(platform);
        String platformUser = platformUsernameLabel(platform);
        Component message = TextFormatter.component(PREFIX
                        + "&e"
                        + rewarded.getName()
                        + " &7supported &f"
                        + ReviewConstants.PLUGIN_NAME
                        + " &7and received &f"
                        + coinsReceived
                        + " coins&7! &aClick here &7to get up to &f"
                        + ReviewConstants.BROADCAST_TEASER_COINS
                        + " free coins &7too — then type your &f"
                        + platformUser
                        + "&7.")
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(
                                "Paste the command, then add your " + platformUser + " (not Minecraft).")
                        .color(NamedTextColor.GRAY)));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(rewarded.getUniqueId())) {
                continue;
            }
            online.sendMessage(message);
            EntityCompat.playSound(online, online.getLocation(), SOUND_BROADCAST_EXP, 0.85f, 1.15f);
        }
    }

    public static void timeout(Player player, String platformLabel) {
        send(player, "&cTime expired — no valid " + platformLabel + " verification detected.");
        send(player, "&7Tip: next time use your real " + platformLabel + " username, not Minecraft.");
    }

    public static void spigotRatingTooLow(Player player, int stars) {
        send(player, "&eFound your review (&f" + stars
                + "★&e), but you need &f4+ stars &efor the reward. Update it before the timer ends!");
    }

    private static String suggestCommand(ReviewPlatform platform) {
        if (platform == ReviewPlatform.GITHUB) {
            return "/kt review github ";
        }
        return "/kt review spigotmc ";
    }

    private static String platformLabel(ReviewPlatform platform) {
        return platform == ReviewPlatform.GITHUB ? "GitHub" : "SpigotMC";
    }

    private static String platformUsernameLabel(ReviewPlatform platform) {
        return platform == ReviewPlatform.GITHUB ? "GitHub username" : "SpigotMC username";
    }

    private static void sendLink(Player player, String intro, String url) {
        Component message = TextFormatter.component(PREFIX + intro)
                .append(Component.space())
                .append(Component.text(url)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url)));
        player.sendMessage(message);
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(TextFormatter.color(PREFIX + message));
    }
}
