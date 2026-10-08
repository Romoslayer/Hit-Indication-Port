package com.rosymaple.hitindication.fabric;

import com.rosymaple.hitindication.client.HitIndicationHud;
import com.rosymaple.hitindication.client.ModKeyBindings;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import com.rosymaple.hitindication.networking.AddHitIndicatorS2CPacket;
import com.rosymaple.hitindication.networking.SetHitMarkerS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class HitIndicationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HitIndicatorClientConfigs.set(FabricClientConfig.load());

        ModKeyBindings.create().forEach(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.START_CLIENT_TICK.register(ModKeyBindings::onClientTick);

        // Called at the end of Gui#render, after the whole vanilla HUD.
        HudRenderCallback.EVENT.register(HitIndicationHud::render);

        ClientPlayNetworking.registerGlobalReceiver(AddHitIndicatorS2CPacket.TYPE, (payload, context) ->
                ClientLatestHits.addHitIndicator(payload.x(), payload.y(), payload.z(),
                        payload.indicatorType(), payload.damagePercent(), payload.negativeEffectPotion()));
        ClientPlayNetworking.registerGlobalReceiver(SetHitMarkerS2CPacket.TYPE, (payload, context) ->
                ClientLatestHits.setHitMarker(payload.markerType()));

        // The original cleared on any client level unload: leaving the server or changing dimension.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLatestHits.clear());
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, level) -> ClientLatestHits.clear());
    }
}
