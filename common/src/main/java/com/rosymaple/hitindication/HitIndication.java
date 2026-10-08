package com.rosymaple.hitindication;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

// Hit Indication by Hamester and Axovoxel (https://github.com/TheHamester/HitIndicator), licensed
// under CC BY-SA 4.0. This is a modified, unofficial port to Minecraft 1.21.1, 26.2 and 26.3 on
// Fabric, NeoForge and Forge, distributed under the same license. See NOTICE.md for the list of
// changes.
//
// Trans Rights!!

public final class HitIndication {
    public static final String MODID = "hitindication";

    private static Platform platform;

    private HitIndication() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
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

        /**
         * Whether a melee blow from {@code attacker} puts the shield {@code blocker} is holding up
         * on cooldown. Vanilla and NeoForge decide this in LivingEntity#canDisableShield; Forge
         * asks the attacker's weapon instead.
         */
        default boolean disablesShield(LivingEntity attacker, LivingEntity blocker) {
            return attacker.canDisableShield();
        }
    }
}
