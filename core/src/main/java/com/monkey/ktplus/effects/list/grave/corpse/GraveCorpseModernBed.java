package com.monkey.ktplus.effects.list.grave.corpse;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;

final class GraveCorpseModernBed {
    private GraveCorpseModernBed() {}

    static void show(Player observer, Location bedHead, BlockFace headFacing) {
        Bed bed = (Bed) Bukkit.createBlockData(Material.RED_BED);
        bed.setFacing(headFacing);
        bed.setPart(Bed.Part.HEAD);
        observer.sendBlockChange(bedHead, bed);
    }

    static void hide(Player observer, Location bedHead) {
        observer.sendBlockChange(bedHead, bedHead.getBlock().getBlockData());
    }
}
