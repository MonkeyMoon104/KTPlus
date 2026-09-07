package com.monkey.ktplus.listener.world;

import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class TemporaryBlockProtectionListener implements Listener {
    private final TemporaryBlockService temporaryBlocks;

    public TemporaryBlockProtectionListener(TemporaryBlockService temporaryBlocks) {
        this.temporaryBlocks = temporaryBlocks;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (temporaryBlocks.isTemporary(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(temporaryBlocks::isTemporary);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (temporaryBlocks.isTemporary(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (temporaryBlocks.isTemporary(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent event) {
        if (temporaryBlocks.isTemporary(event.getToBlock()) || temporaryBlocks.isTemporary(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (temporaryBlocks.isTemporary(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (temporaryBlocks.isTemporary(target) || isControlledFire(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!temporaryBlocks.isTemporary(block) && !isControlledFire(block)) {
            return;
        }
        Material hand = event.getItem() == null ? Material.AIR : event.getItem().getType();
        if (hand == Material.WATER_BUCKET
                || hand == Material.POWDER_SNOW_BUCKET
                || hand.name().contains("SHOVEL")
                || hand == Material.FLINT_AND_STEEL) {
            event.setCancelled(true);
        }
    }

    private boolean isControlledFire(Block block) {
        Material type = block.getType();
        return (type == Material.FIRE || type == Material.SOUL_FIRE) && temporaryBlocks.isTemporary(block);
    }
}
