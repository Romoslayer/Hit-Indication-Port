package com.rosymaple.hitindication.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.rosymaple.hitindication.event.HitEvents;
import com.rosymaple.hitindication.fabric.HitIndicationFabric;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract boolean canBeAffected(MobEffectInstance newEffect);

    @Shadow
    public abstract boolean isDeadOrDying();

    /**
     * Fabric has no mob effect event. This is where vanilla decides whether an effect applies at
     * all, matching where NeoForge's MobEffectEvent.Added fires.
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void hitindication$onAddEffect(MobEffectInstance newEffect, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if(!self.level().isClientSide() && canBeAffected(newEffect))
            HitEvents.onEffectApplied(self, newEffect, source);
    }

    /** One frame per hit, so a hit nested inside another (from some mod) keeps its own damage. */
    @WrapMethod(method = "hurtServer")
    private boolean hitindication$trackHit(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity)(Object)this;
        HitIndicationFabric.beginHurt(self);
        try {
            return original.call(level, source, damage);
        } finally {
            HitIndicationFabric.endHurt(self);
        }
    }

    /**
     * Fabric's AFTER_DAMAGE skips hits that leave the entity dying, so those are reported from
     * here, at the same point and with the same values (NeoForge's damage event covers them too).
     */
    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void hitindication$afterFatalDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir,
                                                @Local(ordinal = 0) boolean blocked) {
        if(isDeadOrDying())
            HitIndicationFabric.afterDamage((LivingEntity)(Object)this, source, damage, blocked);
    }
}
