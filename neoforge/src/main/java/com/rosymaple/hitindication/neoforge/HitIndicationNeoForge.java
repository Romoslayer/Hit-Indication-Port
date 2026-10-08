package com.rosymaple.hitindication.neoforge;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.event.HitEvents;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(HitIndication.MODID)
public class HitIndicationNeoForge {
    public HitIndicationNeoForge(IEventBus modBus, ModContainer container) {
        HitIndication.setPlatform((player, payload) -> {
            // Vanilla clients can join: the channel is optional, and they are simply skipped.
            if(player.connection.hasChannel(payload))
                PacketDistributor.sendToPlayer(player, payload);
        });

        modBus.addListener(HitIndicationNeoForge::registerPayloads);

        // Lowest priority, so these see the outcome after every other mod has had its say (a
        // cancelled death or projectile impact never reaches them).
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HitIndicationNeoForge::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HitIndicationNeoForge::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HitIndicationNeoForge::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HitIndicationNeoForge::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HitIndicationNeoForge::onEffectAdded);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> HitEvents.onServerTickEnd());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> HitEvents.onServerStopped());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .playToClient(AddHitIndicatorS2CPacket.TYPE, AddHitIndicatorS2CPacket.STREAM_CODEC, (payload, context) ->
                        ClientLatestHits.addHitIndicator(payload.x(), payload.y(), payload.z(),
                                payload.indicatorType(), payload.damagePercent(), payload.negativeEffectPotion()))
                .playToClient(SetHitMarkerS2CPacket.TYPE, SetHitMarkerS2CPacket.STREAM_CODEC, (payload, context) ->
                        ClientLatestHits.setHitMarker(payload.markerType()));
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        if(event.getEntity().level().isClientSide())
            return;

        boolean blocked = event.getBlockedDamage() > 0.0F;
        if(blocked)
            HitEvents.onBlocked(event.getEntity(), event.getSource());

        // Matches vanilla's own "did this hit land" test in LivingEntity#hurt. By now the new damage
        // has had armor, enchantments and absorption taken off: it is what the health loses.
        if(!blocked || event.getOriginalDamage() - event.getBlockedDamage() > 0.0F)
            HitEvents.onDamageTaken(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    private static void onDeath(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide())
            return;

        HitEvents.onKill(event.getEntity(), event.getSource());
    }

    private static void onCriticalHit(CriticalHitEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && event.isCriticalHit())
            HitEvents.onCriticalHit(player);
    }

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if(event.getProjectile() instanceof ThrownPotion potion)
            HitEvents.onPotionImpact(potion);
    }

    // Posted once the effect has passed the entity's immunities and MobEffectEvent.Applicable.
    private static void onEffectAdded(MobEffectEvent.Added event) {
        HitEvents.onEffectApplied(event.getEntity(), event.getEffectInstance(), event.getEffectSource());
    }
}
