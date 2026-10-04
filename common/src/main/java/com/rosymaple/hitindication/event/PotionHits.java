package com.rosymaple.hitindication.event;

import com.rosymaple.hitindication.latesthits.HitIndicatorType;
import com.rosymaple.hitindication.latesthits.PacketsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects what each splash potion or effect cloud actually did to each player during a server
 * tick, and turns it into one indicator per potion per player at the end of the tick. One splash
 * applies its effects and its damage one by one, so reporting each of them separately would draw
 * several indicators for a single hit.
 */
final class PotionHits {
    private record Key(ServerPlayer player, Entity potion) {
    }

    private static final class Hit {
        final @Nullable LivingEntity owner;
        boolean damaged;
        float healthDamage;
        boolean damagingEffect;
        boolean negativeEffect;

        Hit(@Nullable LivingEntity owner) {
            this.owner = owner;
        }
    }

    // Only touched on the server thread.
    private static final Map<Key, Hit> PENDING = new LinkedHashMap<>();

    private PotionHits() {
    }

    private static Hit hit(ServerPlayer player, Entity potion, @Nullable LivingEntity owner) {
        return PENDING.computeIfAbsent(new Key(player, potion), key -> new Hit(owner));
    }

    /** Health the potion or cloud took, after armor, enchantments and absorption. */
    static void recordDamage(ServerPlayer player, Entity potion, @Nullable LivingEntity owner, float healthDamage) {
        Hit hit = hit(player, potion, owner);
        hit.damaged = true;
        hit.healthDamage += healthDamage;
    }

    /**
     * An effect the potion or cloud applied.
     *
     * @param damaging whether the effect hurts over time, which the original reported like damage
     */
    static void recordEffect(ServerPlayer player, Entity potion, @Nullable LivingEntity owner, boolean damaging) {
        Hit hit = hit(player, potion, owner);
        if (damaging)
            hit.damagingEffect = true;
        else
            hit.negativeEffect = true;
    }

    static void flush() {
        if (PENDING.isEmpty())
            return;

        for (Map.Entry<Key, Hit> entry : PENDING.entrySet()) {
            ServerPlayer player = entry.getKey().player();
            Hit hit = entry.getValue();
            if (player.hasDisconnected())
                continue;

            int damagePercent = (int)Math.floor(hit.healthDamage / player.getMaxHealth() * 100);
            if (hit.owner == null) {
                // Nothing to point at (a dispensed potion, or the owner is gone): only real damage
                // is shown, as a non-directional hit.
                if (hit.damaged)
                    PacketsHelper.addHitIndicator(player, null, HitIndicatorType.ND_HIT, damagePercent, false);
                continue;
            }

            boolean damaging = hit.damaged || hit.damagingEffect;
            PacketsHelper.addHitIndicator(player, hit.owner, HitIndicatorType.HIT, damagePercent, !damaging && hit.negativeEffect);
        }
        PENDING.clear();
    }

    static void clear() {
        PENDING.clear();
    }
}
