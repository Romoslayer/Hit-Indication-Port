package com.rosymaple.hitindication.fabric;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.event.HitEvents;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public class HitIndicationFabric implements ModInitializer {
    // One frame per LivingEntity#hurtServer call in progress, innermost first. Collects the damage
    // the hit takes from a player's health (see PlayerMixin), so it can be reported the way
    // NeoForge's getHealthDamage does; Fabric's AFTER_DAMAGE only gives the damage before armor.
    // Only touched on the server thread.
    private static final Deque<HurtFrame> HURTS = new ArrayDeque<>();

    private static final class HurtFrame {
        final LivingEntity entity;
        float healthDamage;
        boolean reported;

        HurtFrame(LivingEntity entity) {
            this.entity = entity;
        }
    }

    @Override
    public void onInitialize() {
        HitIndication.setPlatform((player, payload) -> {
            if(ServerPlayNetworking.canSend(player, payload.type()))
                ServerPlayNetworking.send(player, payload);
        });

        PayloadTypeRegistry.clientboundPlay().register(AddHitIndicatorS2CPacket.TYPE, AddHitIndicatorS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SetHitMarkerS2CPacket.TYPE, SetHitMarkerS2CPacket.STREAM_CODEC);

        // AFTER_DAMAGE skips fatal hits; LivingEntityMixin reports those.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                afterDamage(entity, source, damageTaken, blocked));
        ServerLivingEntityEvents.AFTER_DEATH.register(HitEvents::onKill);

        ServerTickEvents.END_SERVER_TICK.register(server -> HitEvents.onServerTickEnd());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HitEvents.onServerStopped();
            HURTS.clear();
        });
    }

    public static void beginHurt(LivingEntity entity) {
        HURTS.push(new HurtFrame(entity));
    }

    public static void endHurt(LivingEntity entity) {
        HurtFrame frame = HURTS.peek();
        if(frame != null && frame.entity == entity)
            HURTS.pop();
    }

    private static @Nullable HurtFrame currentHurt(LivingEntity entity) {
        HurtFrame frame = HURTS.peek();
        return frame != null && frame.entity == entity ? frame : null;
    }

    /** Damage the current hit takes from a player's health, after armor and absorption. */
    public static void recordHealthDamage(LivingEntity entity, float damage) {
        HurtFrame frame = currentHurt(entity);
        if(frame != null)
            frame.healthDamage += damage;
    }

    /**
     * A hit got through LivingEntity#hurtServer.
     *
     * @param damageTaken damage after shields, before armor
     */
    public static void afterDamage(LivingEntity entity, DamageSource source, float damageTaken, boolean blocked) {
        HurtFrame frame = currentHurt(entity);
        if(frame != null) {
            if(frame.reported)
                return;
            frame.reported = true;
        }

        if(blocked)
            HitEvents.onBlocked(entity, source);

        // Matches vanilla's own "did this hit land" test in LivingEntity#hurtServer.
        if(!blocked || damageTaken > 0.0F)
            HitEvents.onDamageTaken(entity, source, frame != null ? frame.healthDamage : damageTaken);
    }
}
