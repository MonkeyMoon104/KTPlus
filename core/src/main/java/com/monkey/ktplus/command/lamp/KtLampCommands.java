package com.monkey.ktplus.command.lamp;

import com.monkey.ktplus.command.KtCommandActions;
import java.util.Objects;
import org.bukkit.permissions.PermissionDefault;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Suggest;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public final class KtLampCommands {
    private final KtCommandActions actions;

    public KtLampCommands(KtCommandActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Command("ktplus")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void rootKtPlus(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("kt")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void rootKt(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("killeffect")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void rootKillEffect(BukkitCommandActor actor) {
        root(actor);
    }

    @Command("ktplus reload")
    @CommandPermission("ktplus.reload")
    public void reloadKtPlus(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("kt reload")
    @CommandPermission("ktplus.reload")
    public void reloadKt(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("killeffect reload")
    @CommandPermission("ktplus.reload")
    public void reloadKillEffect(BukkitCommandActor actor) {
        actions.reload(actor.sender());
    }

    @Command("ktplus set")
    @CommandPermission(value = "ktplus.set", defaultAccess = PermissionDefault.TRUE)
    public void setKtPlus(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("kt set")
    @CommandPermission(value = "ktplus.set", defaultAccess = PermissionDefault.TRUE)
    public void setKt(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("killeffect set")
    @CommandPermission(value = "ktplus.set", defaultAccess = PermissionDefault.TRUE)
    public void setKillEffect(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            @Optional String playerName) {
        set(actor, effectId, playerName);
    }

    @Command("ktplus clear")
    @CommandPermission(value = "ktplus.clear", defaultAccess = PermissionDefault.TRUE)
    public void clearKtPlus(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("kt clear")
    @CommandPermission(value = "ktplus.clear", defaultAccess = PermissionDefault.TRUE)
    public void clearKt(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("killeffect clear")
    @CommandPermission(value = "ktplus.clear", defaultAccess = PermissionDefault.TRUE)
    public void clearKillEffect(BukkitCommandActor actor, @Optional String playerName) {
        clear(actor, playerName);
    }

    @Command("ktplus test")
    @CommandPermission("ktplus.test")
    public void testKtPlus(BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("kt test")
    @CommandPermission("ktplus.test")
    public void testKt(BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("killeffect test")
    @CommandPermission("ktplus.test")
    public void testKillEffect(
            BukkitCommandActor actor, @SuggestWith(EffectIdSuggestions.class) String effectId) {
        test(actor, effectId);
    }

    @Command("ktplus disable")
    @CommandPermission("ktplus.disable")
    public void disableKtPlus(
            BukkitCommandActor actor, @SuggestWith(EnabledEffectIdSuggestions.class) String effectId) {
        actions.disable(actor.sender(), effectId);
    }

    @Command("kt disable")
    @CommandPermission("ktplus.disable")
    public void disableKt(
            BukkitCommandActor actor, @SuggestWith(EnabledEffectIdSuggestions.class) String effectId) {
        actions.disable(actor.sender(), effectId);
    }

    @Command("killeffect disable")
    @CommandPermission("ktplus.disable")
    public void disableKillEffect(
            BukkitCommandActor actor, @SuggestWith(EnabledEffectIdSuggestions.class) String effectId) {
        actions.disable(actor.sender(), effectId);
    }

    @Command("ktplus enable")
    @CommandPermission("ktplus.enable")
    public void enableKtPlus(
            BukkitCommandActor actor, @SuggestWith(DisabledEffectIdSuggestions.class) String effectId) {
        actions.enable(actor.sender(), effectId);
    }

    @Command("kt enable")
    @CommandPermission("ktplus.enable")
    public void enableKt(
            BukkitCommandActor actor, @SuggestWith(DisabledEffectIdSuggestions.class) String effectId) {
        actions.enable(actor.sender(), effectId);
    }

    @Command("killeffect enable")
    @CommandPermission("ktplus.enable")
    public void enableKillEffect(
            BukkitCommandActor actor, @SuggestWith(DisabledEffectIdSuggestions.class) String effectId) {
        actions.enable(actor.sender(), effectId);
    }

    @Command("ktplus grant")
    @CommandPermission("ktplus.grant")
    public void grantKtPlus(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.grant(actor.sender(), effectId, playerName);
    }

    @Command("kt grant")
    @CommandPermission("ktplus.grant")
    public void grantKt(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.grant(actor.sender(), effectId, playerName);
    }

    @Command("killeffect grant")
    @CommandPermission("ktplus.grant")
    public void grantKillEffect(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.grant(actor.sender(), effectId, playerName);
    }

    @Command("ktplus revoke")
    @CommandPermission("ktplus.revoke")
    public void revokeKtPlus(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.revoke(actor.sender(), effectId, playerName);
    }

    @Command("kt revoke")
    @CommandPermission("ktplus.revoke")
    public void revokeKt(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.revoke(actor.sender(), effectId, playerName);
    }

    @Command("killeffect revoke")
    @CommandPermission("ktplus.revoke")
    public void revokeKillEffect(
            BukkitCommandActor actor,
            @SuggestWith(EffectIdSuggestions.class) String effectId,
            String playerName) {
        actions.revoke(actor.sender(), effectId, playerName);
    }

    @Command("ktplus killcoins")
    @CommandPermission(value = "ktplus.killcoins", defaultAccess = PermissionDefault.TRUE)
    public void killCoinsKtPlus(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("kt killcoins")
    @CommandPermission(value = "ktplus.killcoins", defaultAccess = PermissionDefault.TRUE)
    public void killCoinsKt(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("killeffect killcoins")
    @CommandPermission(value = "ktplus.killcoins", defaultAccess = PermissionDefault.TRUE)
    public void killCoinsKillEffect(
            BukkitCommandActor actor,
            @Suggest({"bal", "add", "take", "set", "reset"}) String action,
            @Optional String player,
            @Optional String amount) {
        killCoins(actor, action, player, amount);
    }

    @Command("ktplus review")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void reviewKtPlus(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("kt review")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void reviewKt(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("killeffect review")
    @CommandPermission(value = "ktplus.use", defaultAccess = PermissionDefault.TRUE)
    public void reviewKillEffect(
            BukkitCommandActor actor,
            @Suggest({"github", "spigotmc"}) @Optional String target,
            @Optional String account) {
        review(actor, target, account);
    }

    @Command("ktplus migrate")
    @CommandPermission("ktplus.migrate")
    public void migrateKtPlus(
            BukkitCommandActor actor,
            @SuggestWith(MigrateTargetSuggestions.class) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    @Command("kt migrate")
    @CommandPermission("ktplus.migrate")
    public void migrateKt(
            BukkitCommandActor actor,
            @SuggestWith(MigrateTargetSuggestions.class) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    @Command("killeffect migrate")
    @CommandPermission("ktplus.migrate")
    public void migrateKillEffect(
            BukkitCommandActor actor,
            @SuggestWith(MigrateTargetSuggestions.class) String target,
            @Optional String a,
            @Optional String b,
            @Optional String c,
            @Optional String d,
            @Optional String e) {
        actions.migrate(actor.sender(), migrateArgs(target, a, b, c, d, e));
    }

    @Command("ktplus import-kt")
    @CommandPermission("ktplus.migrate")
    public void importKtPlus(
            BukkitCommandActor actor,
            @Suggest({"--dry-run", "--overwrite-balances"}) @Optional String a,
            @Optional String b,
            @Optional String c) {
        actions.importKt(actor.sender(), importKtArgs(a, b, c));
    }

    @Command("kt import-kt")
    @CommandPermission("ktplus.migrate")
    public void importKt(
            BukkitCommandActor actor,
            @Suggest({"--dry-run", "--overwrite-balances"}) @Optional String a,
            @Optional String b,
            @Optional String c) {
        actions.importKt(actor.sender(), importKtArgs(a, b, c));
    }

    @Command("killeffect import-kt")
    @CommandPermission("ktplus.migrate")
    public void importKillEffect(
            BukkitCommandActor actor,
            @Suggest({"--dry-run", "--overwrite-balances"}) @Optional String a,
            @Optional String b,
            @Optional String c) {
        actions.importKt(actor.sender(), importKtArgs(a, b, c));
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

    private static String[] importKtArgs(String a, String b, String c) {
        java.util.ArrayList<String> args = new java.util.ArrayList<String>();
        if (a != null) {
            args.add(a);
        }
        if (b != null) {
            args.add(b);
        }
        if (c != null) {
            args.add(c);
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
