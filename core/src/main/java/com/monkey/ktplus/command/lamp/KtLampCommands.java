package com.monkey.ktplus.command.lamp;

import com.monkey.ktplus.command.KtCommandActions;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Suggest;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import java.util.Objects;

public final class KtLampCommands {
    private final KtCommandActions actions;

    public KtLampCommands(KtCommandActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Command("ktplus")
    public void rootKtPlus(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("kt")
    public void rootKt(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("killeffect")
    public void rootKillEffect(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("ktplus reload")
    public void reloadKtPlus(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("kt reload")
    public void reloadKt(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("killeffect reload")
    public void reloadKillEffect(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("ktplus set")
    public void setKtPlus(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("kt set")
    public void setKt(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("killeffect set")
    public void setKillEffect(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("ktplus clear")
    public void clearKtPlus(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("kt clear")
    public void clearKt(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("killeffect clear")
    public void clearKillEffect(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("ktplus test")
    public void testKtPlus(BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("kt test")
    public void testKt(BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("killeffect test")
    public void testKillEffect(
            BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("ktplus killcoins")
    public void killCoinsKtPlus(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("kt killcoins")
    public void killCoinsKt(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("killeffect killcoins")
    public void killCoinsKillEffect(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("ktplus review")
    public void reviewKtPlus(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("kt review")
    public void reviewKt(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("killeffect review")
    public void reviewKillEffect(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("ktplus migrate")
    public void migrateKtPlus(
            BukkitCommandActor actor,
            @Suggest({"sqlite", "mysql"}) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    @Command("kt migrate")
    public void migrateKt(
            BukkitCommandActor actor,
            @Suggest({"sqlite", "mysql"}) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    @Command("killeffect migrate")
    public void migrateKillEffect(
            BukkitCommandActor actor,
            @Suggest({"sqlite", "mysql"}) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    private static String[] migrateArgs(
            String target, String a, String b, String c, String d, String e) {
        java.util.ArrayList<String> args = new java.util.ArrayList<String>();
        args.add(target);
        if (a != null) {
            args.add(a);
        }
        if (b != null) {
            args.add(b);
        }
        if (c != null) {
            args.add(c);
        }
        if (d != null) {
            args.add(d);
        }
        if (e != null) {
            args.add(e);
        }
        return args.toArray(new String[0]);
    }

    private void root(BukkitCommandActor actor) {
        if (!actor.isPlayer()) {
            actor.sender().sendMessage(actions.playerOnlyMessage());
            return;
        }
        actions.openGui(actor.requirePlayer());
    }

    private void set(BukkitCommandActor actor, String effectId, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            if (!actor.isPlayer()) {
                actor.sender().sendMessage(actions.playerOnlyMessage());
                return;
            }
            actions.set(actor.requirePlayer(), effectId);
            return;
        }
        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (target == null) {
            actor.sender().sendMessage(actions.playerNotFoundMessage());
            return;
        }
        actions.set(actor.sender(), target, effectId);
    }

    private void clear(BukkitCommandActor actor, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            if (!actor.isPlayer()) {
                actor.sender().sendMessage(actions.playerOnlyMessage());
                return;
            }
            actions.clear(actor.requirePlayer());
            return;
        }
        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (target == null) {
            actor.sender().sendMessage(actions.playerNotFoundMessage());
            return;
        }
        actions.clear(actor.sender(), target);
    }

    private void test(BukkitCommandActor actor, String effectId) {
        if (!actor.isPlayer()) {
            actor.sender().sendMessage(actions.playerOnlyMessage());
            return;
        }
        actions.test(actor.requirePlayer(), effectId);
    }

    private void killCoins(BukkitCommandActor actor, String action, String player, String amount) {
        if (player == null) {
            actions.killCoins(actor.sender(), new String[] {action});
            return;
        }
        if (amount == null) {
            actions.killCoins(actor.sender(), new String[] {action, player});
            return;
        }
        actions.killCoins(actor.sender(), new String[] {action, player, amount});
    }

    private void review(BukkitCommandActor actor, String target, String account) {
        if (target == null || target.trim().isEmpty()) {
            actions.review(actor.sender(), "", null);
            return;
        }
        actions.review(actor.sender(), target, account);
    }
}
