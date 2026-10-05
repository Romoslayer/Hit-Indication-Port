package com.rosymaple.hitindication.forge;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.event.HitEvents;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

@Mod(HitIndication.MODID)
public final class HitIndicationForge {
    // Optional on both sides: vanilla clients can join a server running the mod, and a client with
    // it can join a server without it. The handlers run on the client's main thread.
    static final Channel<CustomPacketPayload> CHANNEL = ChannelBuilder
            .named(HitIndication.id("main"))
            .networkProtocolVersion(1)
            .optional()
            .payloadChannel()
                .play()
                    .clientbound()
                        .addMain(AddHitIndicatorS2CPacket.TYPE, AddHitIndicatorS2CPacket.STREAM_CODEC, (payload, context) ->
                                ClientLatestHits.addHitIndicator(payload.x(), payload.y(), payload.z(),
                                        payload.indicatorType(), payload.damagePercent(), payload.negativeEffectPotion()))
                        .addMain(SetHitMarkerS2CPacket.TYPE, SetHitMarkerS2CPacket.STREAM_CODEC, (payload, context) ->
                                ClientLatestHits.setHitMarker(payload.markerType()))
            .build();

    public HitIndicationForge(FMLJavaModLoadingContext context) {
        HitIndication.setPlatform(HitIndicationForge::sendToPlayer);

        // Lowest priority, so these see the outcome after every other mod has had its say. A
        // cancelled event stops before reaching them.
        ShieldBlockEvent.BUS.addListener(Priority.LOWEST, HitIndicationForge::onShieldBlock);
        LivingDamageEvent.BUS.addListener(Priority.LOWEST, HitIndicationForge::onDamage);
        LivingDeathEvent.BUS.addListener(Priority.LOWEST, HitIndicationForge::onDeath);
        CriticalHitEvent.BUS.addListener(Priority.LOWEST, HitIndicationForge::onCriticalHit);
        ProjectileImpactEvent.BUS.addListener(Priority.LOWEST, HitIndicationForge::onProjectileImpact);
        MobEffectEvent.Added.BUS.addListener(Priority.LOWEST, HitIndicationForge::onEffectAdded);
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> HitEvents.onServerTickEnd());
        ServerStoppedEvent.BUS.addListener(event -> HitEvents.onServerStopped());

        if (FMLEnvironment.dist == Dist.CLIENT)
            HitIndicationForgeClient.init(context);
    }

    private static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        // Players whose client does not have the mod are simply skipped.
        Connection connection = player.connection.getConnection();
        if (CHANNEL.isRemotePresent(connection))
            CHANNEL.send(payload, connection);
    }

    // Fired while LivingEntity#hurtServer works out how much a shield stops; the hit goes on after.
    private static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getBlockedDamage() > 0.0F && !event.getEntity().level().isClientSide())
            HitEvents.onBlocked(event.getEntity(), event.getDamageSource());
    }

    // Fired once armor, enchantments and absorption have been applied, just before the health
    // drops, also for fatal hits: the amount is what NeoForge's getHealthDamage reports. A hit a
    // shield stopped completely never gets here, matching vanilla's own "did this hit land" test.
    private static void onDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide())
            HitEvents.onDamageTaken(event.getEntity(), event.getSource(), event.getAmount());
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide())
            HitEvents.onKill(event.getEntity(), event.getSource());
    }

    // Fired when the attack is worked out, as in the original and on NeoForge.
    private static void onCriticalHit(CriticalHitEvent event) {
        boolean critical = event.getResult().isAllowed() || (event.isVanillaCritical() && event.getResult().isDefault());
        if (critical && event.getEntity() instanceof ServerPlayer player)
            HitEvents.onCriticalHit(player);
    }

    // The potion only shatters (onHit) when no mod changed the impact result.
    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getImpactResult() == ProjectileImpactEvent.ImpactResult.DEFAULT
                && event.getProjectile() instanceof AbstractThrownPotion potion)
            HitEvents.onPotionImpact(potion);
    }

    // Posted once the effect has passed the entity's immunities and MobEffectEvent.Applicable.
    private static void onEffectAdded(MobEffectEvent.Added event) {
        HitEvents.onEffectApplied(event.getEntity(), event.getEffectInstance(), event.getEffectSource());
    }
}
