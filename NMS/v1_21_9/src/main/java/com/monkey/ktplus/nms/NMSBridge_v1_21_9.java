package com.monkey.ktplus.nms;

import com.monkey.ktplus.bridge.AbstractPlatformNmsBridge;
import com.monkey.ktplus.bridge.compat.CompatBridgeRegistry;
import com.monkey.ktplus.common.platform.PlatformCapabilitySets;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class NMSBridge_v1_21_9 extends AbstractPlatformNmsBridge {
    static {
        touch(ServerPlayer.class);
        touch(AbstractContainerMenu.class);
        touch(MenuType.class);
        touch(ServerGamePacketListenerImpl.class);
        touch(Component.class);
        touch(ClientboundOpenScreenPacket.class);
        touch(MenuProvider.class);
    }

    public NMSBridge_v1_21_9() {
        super(CompatBridgeRegistry.forModule("v1_21_9"), PlatformCapabilitySets.modernPaper());
    }

    private static void touch(Class<?> type) {
        if (type.getName().isEmpty()) {
            throw new IllegalStateException("unreachable");
        }
    }
}
