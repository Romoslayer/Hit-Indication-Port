package com.rosymaple.hitindication.forge;

import com.rosymaple.hitindication.client.HitIndicationHud;
import com.rosymaple.hitindication.client.ModKeyBindings;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup, called from {@link HitIndicationForge} on the physical client only. */
final class HitIndicationForgeClient {
    private HitIndicationForgeClient() {
    }

    static void init(FMLJavaModLoadingContext context) {
        // The client config only matters on the client, so dedicated servers never load it.
        ForgeClientConfig config = new ForgeClientConfig();
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(config::onConfigEvent);
        ModConfigEvent.Reloading.getBus(context.getModBusGroup()).addListener(config::onConfigEvent);
        context.registerConfig(ModConfig.Type.CLIENT, ForgeClientConfig.SPEC, "hitindication-client.toml");
        HitIndicatorClientConfigs.set(config);

        RegisterKeyMappingsEvent.BUS.addListener(HitIndicationForgeClient::registerKeyMappings);
        // Added last to the root layer stack, so the indicators draw over the whole HUD, as in the original.
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(HitIndicationHud.LAYER_ID, HitIndicationHud::render));

        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> ModKeyBindings.onClientTick(Minecraft.getInstance()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientLatestHits.clear());
        LevelEvent.Unload.BUS.addListener(event -> {
            if(event.getLevel() instanceof ClientLevel)
                ClientLatestHits.clear();
        });
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(ModKeyBindings.CATEGORY_ID);
        ModKeyBindings.create(category).forEach(event::register);
    }
}
