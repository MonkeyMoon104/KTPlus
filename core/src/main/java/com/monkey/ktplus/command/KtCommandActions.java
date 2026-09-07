package com.monkey.ktplus.command;

import com.monkey.ktplus.bootstrap.PluginBootstrap;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.review.ReviewMessages;
import com.monkey.ktplus.review.ReviewPlatform;
import com.monkey.ktplus.review.ReviewRewardService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class KtCommandActions {
    private final PluginBootstrap bootstrap;

    public KtCommandActions(PluginBootstrap bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    public void openGui(Player player) {
        Objects.requireNonNull(player, "player");
        bootstrap.gui().open(player, 0);
    }

    public void reload(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        if (!sender.hasPermission("ktplus.reload") && !sender.hasPermission("ktplus.admin")) {
            sender.sendMessage(bootstrap.config().message("no-permission"));
            return;
        }
        bootstrap.reload();
        sender.sendMessage(bootstrap.config().message("reload-complete"));
    }

    public void set(Player player, String effectId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effectId, "effectId");
        KillEffect effect = bootstrap.registry().find(effectId).orElse(null);
        if (effect == null) {
            player.sendMessage(bootstrap.config().message("unknown-effect").replace("%effect%", effectId));
            return;
        }
        if (!bootstrap.access().canActivate(player, effect.definition())) {
            player.sendMessage(bootstrap.config().message("no-permission"));
            return;
        }
        bootstrap.users().selectEffect(player, effect.definition().id());
        player.sendMessage(bootstrap.config()
                .message("effect-selected")
                .replace("%effect%", effect.definition().displayName()));
    }

    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        bootstrap.users().clearEffect(player);
        player.sendMessage(bootstrap.config().message("effect-cleared"));
    }

    public void test(Player player, String effectId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effectId, "effectId");
        if (!player.hasPermission("ktplus.test") && !player.hasPermission("ktplus.admin")) {
            player.sendMessage(bootstrap.config().message("no-permission"));
            return;
        }
        KillEffect effect = bootstrap.registry().find(effectId).orElse(null);
        if (effect == null) {
            player.sendMessage(bootstrap.config().message("unknown-effect").replace("%effect%", effectId));
            return;
        }
        bootstrap.runtime().start(player, player, player.getLocation(), effect);
    }

    public void review(CommandSender sender, String target, @Nullable String account) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(target, "target");
        if (!(sender instanceof Player player)) {
            ReviewMessages.playerOnly(sender);
            return;
        }
        ReviewPlatform platform = ReviewPlatform.parse(target);
        if (platform == null || account == null || account.trim().isEmpty()) {
            ReviewMessages.usage(sender);
            return;
        }
        ReviewRewardService reviews = bootstrap.reviews();
        if (reviews == null) {
            ReviewMessages.fetchFailed(player);
            return;
        }
        reviews.start(player, platform, account);
    }

    public void killCoins(CommandSender sender, String[] args) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(args, "args");
        if (args.length < 1) {
            sender.sendMessage("/ktplus killcoins <bal|add|take|set|reset>");
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if ("bal".equals(action)) {
            Player target = args.length >= 2
                    ? Bukkit.getPlayerExact(args[1])
                    : sender instanceof Player ? (Player) sender : null;
            if (target == null) {
                sender.sendMessage("/ktplus killcoins bal [player]");
                return;
            }
            sender.sendMessage(bootstrap.config()
                    .message("killcoins-balance")
                    .replace("%player%", target.getName())
                    .replace("%balance%", Long.toString(bootstrap.economy().balance(target))));
            return;
        }
        if (!sender.hasPermission("ktplus.killcoins.admin") && !sender.hasPermission("ktplus.admin")) {
            sender.sendMessage(bootstrap.config().message("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("/ktplus killcoins " + action + " <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        long amount = parseLong(args[2]);
        switch (action) {
            case "add":
                bootstrap.economy().addBalance(target.getUniqueId(), amount);
                break;
            case "take":
                bootstrap.economy().addBalance(target.getUniqueId(), -amount);
                break;
            case "set":
                bootstrap.economy().setBalance(target.getUniqueId(), amount);
                break;
            case "reset":
                bootstrap.economy().setBalance(target.getUniqueId(), bootstrap.config().startingBalance());
                break;
            default:
                sender.sendMessage("/ktplus killcoins <bal|add|take|set|reset>");
                return;
        }
        String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        sender.sendMessage(bootstrap.config().message("killcoins-updated").replace("%player%", name));
    }

    public void migrate(CommandSender sender, String[] args) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(args, "args");
        if (!sender.hasPermission("ktplus.migrate") && !sender.hasPermission("ktplus.admin")) {
            sender.sendMessage(bootstrap.config().message("no-permission"));
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(
                    "/ktplus migrate <sqlite|mysql> [--dry-run] [--include-temp-blocks] "
                            + "[--force-pending-inventory] [--allow-nonempty-target] <confirmation-token>");
            return;
        }
        try {
            com.monkey.ktplus.storage.migration.MigrationDialect live =
                    com.monkey.ktplus.storage.migration.DatabaseMigrator.liveDialectFromConfig(
                            bootstrap.config());
            com.monkey.ktplus.storage.migration.MigrationRequest request =
                    com.monkey.ktplus.storage.migration.DatabaseMigrator.parseArgs(args, live);
            if (request == null) {
                sender.sendMessage("Invalid migrate args. Target must be sqlite or mysql.");
                return;
            }
            com.monkey.ktplus.storage.migration.DatabaseMigrator migrator =
                    new com.monkey.ktplus.storage.migration.DatabaseMigrator(
                            bootstrap.plugin(),
                            bootstrap.configManager(),
                            bootstrap.database(),
                            bootstrap.migrationLock(),
                            new com.monkey.ktplus.storage.migration.connection.MigrationDataSourceFactory(
                                    bootstrap.plugin()),
                            bootstrap.temporaryBlockRepository(),
                            bootstrap.plugin().getLogger());
            sender.sendMessage("Starting migration…");
            for (String message : java.util.List.of(
                    "This may freeze writes. Follow on-screen confirmation token exactly.")) {
                sender.sendMessage(message);
            }
            com.monkey.ktplus.storage.migration.MigrationOutcome outcome = migrator.migrate(request);
            for (String line : outcome.context().operatorMessages()) {
                sender.sendMessage(line);
            }
            if (outcome.reportPath() != null) {
                sender.sendMessage("Report: " + outcome.reportPath());
            }
            sender.sendMessage(outcome.success() ? "Migration OK: " + outcome.message() : "Migration FAILED: " + outcome.message());
        } catch (Exception error) {
            sender.sendMessage("Migration failed: " + error.getMessage());
        }
    }

    public List<String> effectIds() {
        return bootstrap.registry().all().stream()
                .map(effect -> effect.definition().id())
                .collect(Collectors.toList());
    }

    public List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private long parseLong(String value) {
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public String playerOnlyMessage() {
        return bootstrap.config().message("player-only");
    }

    public String playerNotFoundMessage() {
        return bootstrap.config().message("player-not-found");
    }

    public void set(org.bukkit.command.CommandSender sender, org.bukkit.entity.Player target, String effectId) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(playerOnlyMessage());
            return;
        }
        set(target, effectId);
    }

    public void clear(org.bukkit.command.CommandSender sender, org.bukkit.entity.Player target) {
        clear(target);
    }
}
