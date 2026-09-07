package com.monkey.ktplus.lib.relocate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LibraryRelocationRulesTest {
    @Test
    void relocatesPacketEventsOnly() {
        assertEquals(
                "com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents",
                LibraryRelocationRules.relocateClassName("com.github.retrooper.packetevents.PacketEvents"));
        assertEquals(
                "com.monkey.ktplus.libs.packetevents.packetevents.factory.spigot.SpigotPacketEventsBuilder",
                LibraryRelocationRules.relocateClassName(
                        "io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder"));
        assertEquals(
                "com.fasterxml.jackson.databind.ObjectMapper",
                LibraryRelocationRules.relocateClassName("com.fasterxml.jackson.databind.ObjectMapper"));
        assertEquals(
                "com.mysql.cj.jdbc.Driver",
                LibraryRelocationRules.relocateClassName("com.mysql.cj.jdbc.Driver"));
        assertEquals("org.sqlite.JDBC", LibraryRelocationRules.relocateClassName("org.sqlite.JDBC"));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        assertEquals("org.bukkit.Material", LibraryRelocationRules.relocateClassName("org.bukkit.Material"));
    }

    @Test
    void rulesVersionIsSet() {
        assertEquals("10", LibraryRelocationRules.RULES_VERSION);
    }

    @Test
    void guiLibrariesAreNotRuntimeRelocated() {
        assertEquals(
                "fr.minuskube.inv.SmartInventory",
                LibraryRelocationRules.relocateClassName("fr.minuskube.inv.SmartInventory"));
        assertEquals(
                "com.github.stefvanschie.inventoryframework.gui.type.ChestGui",
                LibraryRelocationRules.relocateClassName(
                        "com.github.stefvanschie.inventoryframework.gui.type.ChestGui"));
    }
}
