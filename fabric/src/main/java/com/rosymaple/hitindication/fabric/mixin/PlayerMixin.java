package com.rosymaple.hitindication.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rosymaple.hitindication.fabric.HitIndicationFabric;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Records the damage a hit takes from the player's health, after armor, enchantments and
 * absorption: the value NeoForge's LivingDamageEvent.Post#getHealthDamage reports. Unlike the drop
 * in health, it is not cut short by death or a totem.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void hitindication$recordHealthDamage(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original) {
        HitIndicationFabric.recordHealthDamage((Player)(Object)this, damage);
        original.call(tracker, source, damage);
    }
}
