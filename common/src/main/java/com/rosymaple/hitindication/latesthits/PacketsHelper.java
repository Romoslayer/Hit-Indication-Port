package com.rosymaple.hitindication.latesthits;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class PacketsHelper {
    public static void addHitIndicator(ServerPlayer player, @Nullable LivingEntity damageSource, HitIndicatorType hitIndicatorType, int damagePercent, boolean hasNegativeEffects) {
        double x = damageSource != null ? damageSource.getX() : 0;
        double y = damageSource != null ? damageSource.getY() : 0;
        double z = damageSource != null ? damageSource.getZ() : 0;
        HitIndication.platform().sendToPlayer(player,
                new AddHitIndicatorS2CPacket(x, y, z, hitIndicatorType.type, damagePercent, hasNegativeEffects));
    }

    public static void addHitMarker(ServerPlayer player, HitMarkerType hitMarkerType) {
        HitIndication.platform().sendToPlayer(player, new SetHitMarkerS2CPacket(hitMarkerType.type));
    }
}
