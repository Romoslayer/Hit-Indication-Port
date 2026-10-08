package com.rosymaple.hitindication.neoforge;

import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.client.HitIndicationHud;
import com.rosymaple.hitindication.client.ModKeyBindings;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(value = HitIndication.MODID, dist = Dist.CLIENT)
public class HitIndicationNeoForgeClient {
    public HitIndicationNeoForgeClient(IEventBus modBus, ModContainer container) {
        // The client config only matters on the client, so it is registered here rather than in the
        // common entrypoint; that also keeps NeoForgeClientConfig off dedicated servers.
        NeoForgeClientConfig config = new NeoForgeClientConfig();
        modBus.addListener(ModConfigEvent.Loading.class, config::onConfigEvent);
        modBus.addListener(ModConfigEvent.Reloading.class, config::onConfigEvent);
        container.registerConfig(ModConfig.Type.CLIENT, NeoForgeClientConfig.SPEC, "hitindication-client.toml");
        HitIndicatorClientConfigs.set(config);

        // NeoForge's built-in config screen, reachable from the Mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(HitIndicationNeoForgeClient::registerKeyMappings);
        modBus.addListener(HitIndicationNeoForgeClient::registerGuiLayers);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> ModKeyBindings.onClientTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientLatestHits.clear());
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if(event.getLevel() instanceof ClientLevel)
                ClientLatestHits.clear();
        });
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyBindings.create().forEach(event::register);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HitIndicationHud.LAYER_ID, HitIndicationHud::render);
    }
}
