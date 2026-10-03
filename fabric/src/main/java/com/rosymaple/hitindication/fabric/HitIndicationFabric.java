package com.rosymaple.hitindication.fabric;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.event.HitEvents;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class HitIndicationFabric implements ModInitializer {
    // Health of each player at the start of the hit currently being processed,
    // so AFTER_DAMAGE can report the damage that actually landed, as NeoForge's event does.
    private static final Map<LivingEntity, Float> HEALTH_BEFORE_HIT = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        HitIndication.setPlatform((player, payload) -> {
            if(ServerPlayNetworking.canSend(player, payload.type()))
                ServerPlayNetworking.send(player, payload);
        });

        PayloadTypeRegistry.clientboundPlay().register(AddHitIndicatorS2CPacket.TYPE, AddHitIndicatorS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SetHitMarkerS2CPacket.TYPE, SetHitMarkerS2CPacket.STREAM_CODEC);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if(entity instanceof ServerPlayer)
                HEALTH_BEFORE_HIT.put(entity, entity.getHealth());
            return true;
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            Float healthBefore = HEALTH_BEFORE_HIT.remove(entity);

            if(blocked)
                HitEvents.onBlocked(entity, source);

            // Matches vanilla's own "did this hit land" test in LivingEntity#hurtServer.
            if(!blocked || damageTaken > 0.0F) {
                float healthDamage = healthBefore != null
                        ? Math.max(healthBefore - entity.getHealth(), 0.0F)
                        : damageTaken;
                HitEvents.onDamageTaken(entity, source, healthDamage);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(HitEvents::onKill);
    }
}
