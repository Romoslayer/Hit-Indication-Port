package com.rosymaple.hitindication;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Hit Indication by Hamester and Axovoxel (https://github.com/TheHamester/HitIndicator), licensed
// under CC BY-SA 4.0. This is a modified, unofficial port to Minecraft 26.2/26.3 on Fabric and
// NeoForge, distributed under the same license. See NOTICE.md for the list of changes.
//
// Trans Rights!!

public final class HitIndication {
    public static final String MODID = "hitindication";

    private static Platform platform;

    private HitIndication() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /** Installed by the loader entrypoint before any event can fire. */
    public static void setPlatform(Platform platform) {
        HitIndication.platform = platform;
    }

    public static Platform platform() {
        return platform;
    }

    /** The few things the shared code needs from whichever mod loader is running it. */
    public interface Platform {
        /**
         * Sends a payload to one player, silently skipping players whose client does not have
         * Hit Indication installed (so vanilla clients can still join a server running it).
         */
        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    }
}
