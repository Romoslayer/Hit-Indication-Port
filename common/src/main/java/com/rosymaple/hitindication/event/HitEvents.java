package com.rosymaple.hitindication.event;

import com.rosymaple.hitindication.client.IndicatorMath;
import com.rosymaple.hitindication.latesthits.HitIndicatorType;
import com.rosymaple.hitindication.latesthits.HitMarkerType;
import com.rosymaple.hitindication.latesthits.PacketsHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import org.jspecify.annotations.Nullable;

/**
 * Server-side hit detection. The loader modules call these from their own damage, death, critical
 * hit, effect and projectile hooks; everything after that point is shared.
 */
public class HitEvents {
    // The thrown potion that is shattering right now, between its impact and its removal. Its splash
    // effects name the thrower, not the potion, as their source, so this is how they are told apart
    // from effects the same entity applies some other way. Only touched on the server thread.
    private static @Nullable AbstractThrownPotion shatteringPotion;

    /**
     * A living entity took damage that was not entirely stopped by a shield.
     *
     * @param healthDamage damage dealt after armor, enchantments and absorption
     */
    public static void onDamageTaken(LivingEntity target, DamageSource source, float healthDamage) {
        Entity attackerProjectile = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if(isPotion(attackerProjectile)) {
            if(target instanceof ServerPlayer targetPlayer && !isSelf(attacker, target))
                PotionHits.recordDamage(targetPlayer, attackerProjectile, attacker instanceof LivingEntity owner ? owner : null, healthDamage);
            return;
        }

        if(attacker instanceof ServerPlayer attackingPlayer)
            if(attackerProjectile instanceof Projectile && !isSelf(attacker, target))
                PacketsHelper.addHitMarker(attackingPlayer, HitMarkerType.CRIT);

        if(!(target instanceof ServerPlayer targetPlayer))
            return;

        int damagePercent = (int)Math.floor((healthDamage / targetPlayer.getMaxHealth() * 100));
        if(!(attacker instanceof LivingEntity livingAttacker) || isSelf(attacker, target))
            PacketsHelper.addHitIndicator(targetPlayer, null, HitIndicatorType.ND_HIT, damagePercent, false);
        else
            PacketsHelper.addHitIndicator(targetPlayer, livingAttacker, HitIndicatorType.HIT, damagePercent, false);
    }

    /** A living entity blocked some or all of an attack with a shield (or any item that blocks attacks). */
    public static void onBlocked(LivingEntity target, DamageSource source) {
        Entity attackerProjectile = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if(isPotion(attackerProjectile))
            return;
        if(!(attacker instanceof LivingEntity livingAttacker))
            return;

        // Mirrors Player#blockUsingItem: only a melee hit from a weapon that disables blocking (an
        // axe, by default) puts the shield on cooldown.
        boolean shieldAboutToBreak = !source.is(DamageTypeTags.IS_PROJECTILE)
                && attackerProjectile instanceof LivingEntity directAttacker
                && directAttacker.getSecondsToDisableBlocking() > 0.0F;

        if(target instanceof ServerPlayer targetPlayer)
            PacketsHelper.addHitIndicator(targetPlayer, livingAttacker, HitIndicatorType.BLOCK,
                    shieldAboutToBreak ? IndicatorMath.SHIELD_DISABLE_PERCENT : 0, false);

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
        if(isSelf(attacker, entity))
            return;

        PacketsHelper.addHitMarker(player, HitMarkerType.KILL);
    }

    /** A splash or lingering potion is about to shatter (after any mod could cancel the impact). */
    public static void onPotionImpact(AbstractThrownPotion potion) {
        if(potion.level() instanceof ServerLevel)
            shatteringPotion = potion;
    }

    /**
     * An effect is being applied to a living entity: it passed the entity's immunities and any
     * mod's veto. Only effects from a shattering splash potion or from an effect cloud make an
     * indicator; damage they deal arrives through {@link #onDamageTaken}.
     *
     * @param source the source the game passed along with the effect
     */
    public static void onEffectApplied(LivingEntity target, MobEffectInstance effect, @Nullable Entity source) {
        if(!(target instanceof ServerPlayer player) || effect.getEffect().value().isBeneficial())
            return;

        Entity potion;
        LivingEntity owner;
        if(source instanceof AreaEffectCloud cloud) {
            potion = cloud;
            owner = cloud.getOwner();
        } else {
            AbstractThrownPotion splash = shatteringPotion;
            if(splash == null || splash.isRemoved() || source != splash.getEffectSource())
                return;
            potion = splash;
            owner = splash.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        }

        // As in the original, a potion never points at the player who threw it.
        if(owner == null || isSelf(owner, player))
            return;

        // Poison and Wither hurt over time, so the original showed them like damage rather than as
        // a "non-damaging negative potion".
        boolean damaging = effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER);
        PotionHits.recordEffect(player, potion, owner, damaging);
    }

    /** End of every server tick: one indicator for each potion that hit each player this tick. */
    public static void onServerTickEnd() {
        shatteringPotion = null;
        PotionHits.flush();
    }

    /** The server is stopping: forget players and entities it will not tick again. */
    public static void onServerStopped() {
        shatteringPotion = null;
        PotionHits.clear();
    }

    private static boolean isPotion(@Nullable Entity directEntity) {
        return directEntity instanceof AbstractThrownPotion || directEntity instanceof AreaEffectCloud;
    }

    private static boolean isSelf(@Nullable Entity attacker, Entity target) {
        return attacker != null && attacker.getUUID().equals(target.getUUID());
    }
}
