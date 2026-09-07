package com.monkey.ktplus.effects.list.grave.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class GraveCorpseSpawnerTest {
    @Test
    void corpseYaw_cardinalSleepingBodyAlongStem() {
        assertSleepingBodyAlongStem(BlockFace.NORTH, -90.0F);
        assertSleepingBodyAlongStem(BlockFace.SOUTH, 90.0F);
        assertSleepingBodyAlongStem(BlockFace.EAST, 0.0F);
        assertSleepingBodyAlongStem(BlockFace.WEST, 180.0F);
    }

    @Test
    void corpseLocation_shiftedTowardKiller() {
        Location centerSlab = new Location(null, 4, 65, -207);

        Location northKiller = GraveCorpseBedUtil.corpseLocation(centerSlab, BlockFace.NORTH, BlockFace.SOUTH);
        assertEquals(centerSlab.getBlockX() + 0.5D, northKiller.getX(), 0.001D);
        assertEquals(centerSlab.getBlockZ() + 0.5D + GraveCorpseOrientation.KILLER_SHIFT, northKiller.getZ(), 0.001D);

        Location westKiller = GraveCorpseBedUtil.corpseLocation(centerSlab, BlockFace.WEST, BlockFace.EAST);
        assertEquals(centerSlab.getBlockX() + 0.5D + GraveCorpseOrientation.KILLER_SHIFT, westKiller.getX(), 0.001D);
        assertEquals(centerSlab.getBlockZ() + 0.5D, westKiller.getZ(), 0.001D);
    }

    @Test
    void bedHeadBlock_offsetTowardPillarForAllFacings() {
        Location centerSlab = new Location(null, 10, 65, -200);

        assertBedHead(centerSlab, BlockFace.NORTH, 10, 65, -201);
        assertBedHead(centerSlab, BlockFace.SOUTH, 10, 65, -199);
        assertBedHead(centerSlab, BlockFace.EAST, 11, 65, -200);
        assertBedHead(centerSlab, BlockFace.WEST, 9, 65, -200);
    }

    private static void assertSleepingBodyAlongStem(BlockFace viewFace, float expectedYaw) {
        assertEquals(expectedYaw, GraveCorpseOrientation.corpseYaw(viewFace));
        Vector stem = GraveCorpseOrientation.stemAxis(viewFace);
        Vector bodyAxis = GraveCorpseOrientation.sleepingBodyAxis(expectedYaw);
        assertTrue(bodyAxis.dot(stem) > 0.99D, "sleeping body should run toward pillar for " + viewFace);
    }

    @Test
    void hiddenBedHeadBlock_oneBlockBelowVisibleCross() {
        Location centerSlab = new Location(null, 10, 65, -200);

        assertBedHead(centerSlab, BlockFace.NORTH, 10, 65, -201);
        Location hidden = GraveCorpseOrientation.hiddenBedHeadBlock(centerSlab, BlockFace.NORTH);
        assertEquals(10, hidden.getBlockX());
        assertEquals(64, hidden.getBlockY());
        assertEquals(-201, hidden.getBlockZ());
    }

    private static void assertBedHead(Location centerSlab, BlockFace viewFace, int x, int y, int z) {
        Location bedHead = GraveCorpseOrientation.bedHeadBlock(centerSlab, viewFace);
        assertEquals(x, bedHead.getBlockX());
        assertEquals(y, bedHead.getBlockY());
        assertEquals(z, bedHead.getBlockZ());
    }
}
