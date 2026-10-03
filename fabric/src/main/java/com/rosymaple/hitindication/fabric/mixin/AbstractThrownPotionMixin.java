package com.rosymaple.hitindication.fabric.mixin;

import com.rosymaple.hitindication.event.HitEvents;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric's stand-in for the projectile impact event the original listened to for thrown potions. */
@Mixin(AbstractThrownPotion.class)
public abstract class AbstractThrownPotionMixin {
    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"))
    private void hitindication$onPotionImpact(HitResult hitResult, CallbackInfo ci) {
        HitEvents.onPotionImpact((AbstractThrownPotion)(Object)this);
    }
}
