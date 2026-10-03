package com.rosymaple.hitindication.fabric.mixin;

import com.rosymaple.hitindication.event.HitEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric has no critical hit event. Player#attack calls crit(Entity) exactly when it lands a
 * critical hit, so that is where the crit marker is sent from.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "crit(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void hitindication$onCriticalHit(Entity entity, CallbackInfo ci) {
        HitEvents.onCriticalHit((ServerPlayer)(Object)this);
    }
}
