package com.rosymaple.hitindication.forge;

import com.rosymaple.hitindication.client.HitIndicationHud;
import com.rosymaple.hitindication.client.ModKeyBindings;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup, called from {@link HitIndicationForge} on the physical client only. */
final class HitIndicationForgeClient {
    private HitIndicationForgeClient() {
    }

    static void init(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        // The client config only matters on the client, so dedicated servers never load it.
        ForgeClientConfig config = new ForgeClientConfig();
        modBus.addListener(EventPriority.NORMAL, false, ModConfigEvent.Loading.class, config::onConfigEvent);
        modBus.addListener(EventPriority.NORMAL, false, ModConfigEvent.Reloading.class, config::onConfigEvent);
        context.registerConfig(ModConfig.Type.CLIENT, ForgeClientConfig.SPEC, "hitindication-client.toml");
        HitIndicatorClientConfigs.set(config);

        modBus.addListener((RegisterKeyMappingsEvent event) -> ModKeyBindings.create().forEach(event::register));
        // Added last to the root layer stack, so the indicators draw over the whole HUD, as in the original.
        modBus.addListener((AddGuiOverlayLayersEvent event) ->
                event.getLayeredDraw().add(HitIndicationHud.LAYER_ID, HitIndicationHud::render));

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Pre event) -> ModKeyBindings.onClientTick(Minecraft.getInstance()));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientLatestHits.clear());
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if(event.getLevel() instanceof ClientLevel)
                ClientLatestHits.clear();
        });
    }
}
