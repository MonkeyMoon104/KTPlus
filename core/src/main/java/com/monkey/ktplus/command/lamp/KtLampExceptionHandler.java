package com.monkey.ktplus.command.lamp;

import com.monkey.ktplus.command.KtCommandActions;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;
import revxrsal.commands.exception.NoPermissionException;

final class KtLampExceptionHandler extends BukkitExceptionHandler {
    private final KtCommandActions actions;

    KtLampExceptionHandler(KtCommandActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Override
    public void onNoPermission(NoPermissionException exception, BukkitCommandActor actor) {
        CommandSender sender = actor.sender();
        sender.sendMessage(actions.noPermissionMessage(sender));
    }

    @Override
    public void onSenderNotPlayer(SenderNotPlayerException exception, BukkitCommandActor actor) {
        actor.sender().sendMessage(actions.playerOnlyMessage());
    }
}
