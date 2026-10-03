package com.rosymaple.hitindication.fabric;

import com.rosymaple.hitindication.client.HitIndicationHud;
import com.rosymaple.hitindication.client.ModKeyBindings;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;

public class HitIndicationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HitIndicatorClientConfigs.set(FabricClientConfig.load());

        KeyMapping.Category category = KeyMapping.Category.register(ModKeyBindings.CATEGORY_ID);
        ModKeyBindings.create(category).forEach(KeyMappingHelper::registerKeyMapping);
        ClientTickEvents.START_CLIENT_TICK.register(ModKeyBindings::onClientTick);

        HudElementRegistry.addLast(HitIndicationHud.LAYER_ID, HitIndicationHud::render);

        ClientPlayNetworking.registerGlobalReceiver(AddHitIndicatorS2CPacket.TYPE, (payload, context) ->
                ClientLatestHits.addHitIndicator(payload.x(), payload.y(), payload.z(),
                        payload.indicatorType(), payload.damagePercent(), payload.negativeEffectPotion()));
        ClientPlayNetworking.registerGlobalReceiver(SetHitMarkerS2CPacket.TYPE, (payload, context) ->
                ClientLatestHits.setHitMarker(payload.markerType()));

        // The original cleared on any client level unload: leaving the server or changing dimension.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLatestHits.clear());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> ClientLatestHits.clear());
    }
}
