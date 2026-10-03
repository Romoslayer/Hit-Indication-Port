package com.rosymaple.hitindication.event;

import com.rosymaple.hitindication.latesthits.HitIndicatorType;
import com.rosymaple.hitindication.latesthits.HitMarkerType;
import com.rosymaple.hitindication.latesthits.PacketsHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Server-side hit detection. The loader modules call these from their own damage, death, critical
 * hit and projectile hooks; everything after that point is shared.
 */
public class HitEvents {
    /**
     * A living entity took damage that was not entirely stopped by a shield.
     *
     * @param healthDamage damage dealt after armor, enchantments and absorption
     */
    public static void onDamageTaken(LivingEntity target, DamageSource source, float healthDamage) {
        Entity attackerProjectile = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if(attackerProjectile instanceof AbstractThrownPotion)
            return;

        if(attacker instanceof ServerPlayer attackingPlayer)
            if(attackerProjectile instanceof Projectile && !attacker.getUUID().equals(target.getUUID()))
                PacketsHelper.addHitMarker(attackingPlayer, HitMarkerType.CRIT);

        if(!(target instanceof ServerPlayer targetPlayer))
            return;

        int damagePercent = (int)Math.floor((healthDamage / targetPlayer.getMaxHealth() * 100));
        if(!(attacker instanceof LivingEntity livingAttacker) || attacker.getUUID().equals(target.getUUID()))
            PacketsHelper.addHitIndicator(targetPlayer, null, HitIndicatorType.ND_HIT, damagePercent, false);
        else
            PacketsHelper.addHitIndicator(targetPlayer, livingAttacker, HitIndicatorType.HIT, damagePercent, false);
    }

    /** A living entity blocked some or all of an attack with a shield (or any item that blocks attacks). */
    public static void onBlocked(LivingEntity target, DamageSource source) {
        Entity attackerProjectile = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if(attackerProjectile instanceof AbstractThrownPotion)
            return;
        if(!(attacker instanceof LivingEntity livingAttacker))
            return;

        // Mirrors Player#blockUsingItem: only a melee hit from a weapon that disables blocking (an
        // axe, by default) puts the shield on cooldown.
        boolean shieldAboutToBreak = !source.is(DamageTypeTags.IS_PROJECTILE)
                && attackerProjectile instanceof LivingEntity directAttacker
                && directAttacker.getSecondsToDisableBlocking() > 0.0F;

        if(target instanceof ServerPlayer targetPlayer)
            PacketsHelper.addHitIndicator(targetPlayer, livingAttacker, HitIndicatorType.BLOCK, shieldAboutToBreak ? 125 : 0, false);

        if(livingAttacker instanceof ServerPlayer attackingPlayer)
            PacketsHelper.addHitMarker(attackingPlayer, HitMarkerType.CRIT);
    }

    public static void onCriticalHit(ServerPlayer player) {
        PacketsHelper.addHitMarker(player, HitMarkerType.CRIT);
    }

    public static void onKill(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();

        if(!(attacker instanceof ServerPlayer player))
            return;
        if(attacker.getUUID().equals(entity.getUUID()))
            return;

        PacketsHelper.addHitMarker(player, HitMarkerType.KILL);
    }

    /** A splash or lingering potion is about to shatter. */
    public static void onPotionImpact(AbstractThrownPotion potion) {
        if(!(potion.getOwner() instanceof LivingEntity source) || !(potion.level() instanceof ServerLevel level))
            return;

        AABB axisalignedbb = potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<ServerPlayer> list = level.getEntitiesOfClass(ServerPlayer.class, axisalignedbb);
        if(list.isEmpty())
            return;

        List<MobEffectInstance> effects = new ArrayList<>();
        potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(effects::add);

        boolean hasNegativeEffects = effects.stream().anyMatch((x) -> !x.getEffect().value().isBeneficial());
        boolean damagingPotion = effects.stream().anyMatch((x) -> x.is(MobEffects.POISON)
                || x.is(MobEffects.INSTANT_DAMAGE)
                || x.is(MobEffects.WITHER));

        Optional<MobEffectInstance> instantDamage = effects.stream().filter((x) -> x.is(MobEffects.INSTANT_DAMAGE)).findFirst();
        for(ServerPlayer player : list) {
            if(!player.isAffectedByPotions() || player.getUUID().equals(source.getUUID()))
                continue;

            if(damagingPotion || hasNegativeEffects) {
                int damagePercent = 0;
                if(instantDamage.isPresent()) {
                    float damage = 6 << instantDamage.get().getAmplifier();
                    damagePercent = (int)Math.floor(applyPotionDamageCalculations(player, level, level.damageSources().magic(), damage) / player.getMaxHealth() * 100);
                }

                PacketsHelper.addHitIndicator(player, source, HitIndicatorType.HIT, damagePercent, hasNegativeEffects && !damagingPotion);
            }
        }
    }

    // A side-effect-free copy of the Resistance and enchantment steps of
    // LivingEntity#getDamageAfterMagicAbsorb, used to predict how hard Instant Damage will hit.
    private static float applyPotionDamageCalculations(ServerPlayer player, ServerLevel level, DamageSource damageSource, float damage) {
        if(damageSource.is(DamageTypeTags.BYPASSES_EFFECTS))
            return damage;

        MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
        if(resistance != null && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int absorbValue = (resistance.getAmplifier() + 1) * 5;
            int absorb = 25 - absorbValue;
            damage = Math.max(damage * absorb / 25.0F, 0.0F);
        }

        if(damage <= 0.0F)
            return 0.0F;
        if(damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS))
            return damage;

        float enchantmentArmor = EnchantmentHelper.getDamageProtection(level, player, damageSource);
        if(enchantmentArmor > 0.0F)
            damage = CombatRules.getDamageAfterMagicAbsorb(damage, enchantmentArmor);

        return damage;
    }
}
