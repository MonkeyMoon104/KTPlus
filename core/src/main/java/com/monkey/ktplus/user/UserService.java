package com.monkey.ktplus.user;

import com.monkey.ktplus.access.effect.UserSelectionWriter;
import com.monkey.ktplus.storage.repository.PlayerEffectRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class UserService implements UserSelectionWriter {
    private final PlayerEffectRepository repository;
    private volatile @Nullable BiConsumer<UUID, @Nullable String> selectionListener;

    public UserService(PlayerEffectRepository repository) {
        this.repository = repository;
    }

    public void setSelectionListener(BiConsumer<UUID, @Nullable String> listener) {
        this.selectionListener = listener;
    }

    public Optional<String> selectedEffect(Player player) {
        return selectedEffect(player.getUniqueId());
    }

    public Optional<String> selectedEffect(UUID uuid) {
        return repository.selectedEffect(uuid);
    }

    public void selectEffect(Player player, String effectId) {
        selectEffect(player.getUniqueId(), effectId);
    }

    public void selectEffect(UUID uuid, String effectId) {
        repository.setSelectedEffect(uuid, effectId);
        notifySelectionChanged(uuid, effectId);
    }

    public void clearEffect(Player player) {
        clearEffect(player.getUniqueId());
    }

    public void clearEffect(UUID uuid) {
        repository.clearSelectedEffect(uuid);
        notifySelectionChanged(uuid, null);
    }

    private void notifySelectionChanged(UUID uuid, @Nullable String effectId) {
        BiConsumer<UUID, @Nullable String> listener = selectionListener;
        if (listener != null) {
            listener.accept(uuid, effectId);
        }
    }
}
