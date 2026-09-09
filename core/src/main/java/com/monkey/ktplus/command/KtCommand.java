package com.monkey.ktplus.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class KtCommand implements CommandExecutor, TabCompleter {
    private final KtCommandActions actions;

    public KtCommand(KtCommandActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            actions.openGui((Player) sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                actions.reload(sender);
                break;
            case "set":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only.");
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage("/" + label + " set <effect>");
                    break;
                }
                actions.set((Player) sender, args[1]);
                break;
            case "clear":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only.");
                    break;
                }
                actions.clear((Player) sender);
                break;
            case "test":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only.");
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage("/" + label + " test <effect>");
                    break;
                }
                actions.test((Player) sender, args[1]);
                break;
            case "killcoins":
                actions.killCoins(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "review":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(actions.playerOnlyMessage());
                    break;
                }
                if (args.length < 3) {
                    actions.review(sender, args.length >= 2 ? args[1] : "", null);
                    break;
                }
                actions.review(sender, args[1], args[2]);
                break;
            case "migrate":
                actions.migrate(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "import-kt":
                actions.importKt(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                sender.sendMessage("/" + label + " [reload|set|clear|test|killcoins|review|migrate|import-kt]");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return actions.filter(
                    Arrays.asList(
                            "reload", "set", "clear", "test", "killcoins", "review", "migrate", "import-kt"),
                    args[0]);
        }
        if (args.length == 2 && ("set".equalsIgnoreCase(args[0]) || "test".equalsIgnoreCase(args[0]))) {
            return actions.filter(actions.effectIds(), args[1]);
        }
        if (args.length == 2 && "killcoins".equalsIgnoreCase(args[0])) {
            return actions.filter(Arrays.asList("bal", "add", "take", "set", "reset"), args[1]);
        }
        if (args.length == 2 && "review".equalsIgnoreCase(args[0])) {
            return actions.filter(Arrays.asList("github", "spigotmc"), args[1]);
        }
        if (args.length == 2 && "migrate".equalsIgnoreCase(args[0])) {
            return actions.filter(Arrays.asList("sqlite", "mysql"), args[1]);
        }
        if (args.length >= 3 && "migrate".equalsIgnoreCase(args[0])) {
            return actions.filter(
                    Arrays.asList(
                            "--dry-run",
                            "--include-temp-blocks",
                            "--force-pending-inventory",
                            "--allow-nonempty-target"),
                    args[args.length - 1]);
        }
        if (args.length >= 2 && "import-kt".equalsIgnoreCase(args[0])) {
            return actions.filter(
                    Arrays.asList("--dry-run", "--overwrite-balances"), args[args.length - 1]);
        }
        return new ArrayList<String>();
    }
}
