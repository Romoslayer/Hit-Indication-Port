package com.rosymaple.hitindication.fabric.mixin;

import com.rosymaple.hitindication.event.HitEvents;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's stand-in for NeoForge's projectile impact event: marks the potion as shattering, so the
 * effects its splash applies can be told apart from other effects.
 */
@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin {
    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"))
    private void hitindication$onPotionImpact(HitResult hitResult, CallbackInfo ci) {
        HitEvents.onPotionImpact((ThrownPotion)(Object)this);
    }
}
