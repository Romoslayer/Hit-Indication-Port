package com.rosymaple.hitindication.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rosymaple.hitindication.HitIndication;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The three toggle keys. The loader creates the {@link KeyMapping.Category} (each loader has its
 * own way of registering one) and registers the mappings this class builds.
 */
public class ModKeyBindings {
    public static final Identifier CATEGORY_ID = HitIndication.id("hitindication");

    public static KeyMapping toggleHitIndication;
    public static KeyMapping toggleBlockIndicators;
    public static KeyMapping toggleEdgeOfScreenMode;

    private static LocalPlayer lastPlayer;

    public static List<KeyMapping> create(KeyMapping.Category category) {
        toggleHitIndication = new KeyMapping("key.hitindication.toggle_hit_indication",
                InputConstants.KEY_H, category);
        toggleBlockIndicators = new KeyMapping("key.hitindication.toggle_block_indicators",
                InputConstants.KEY_B, category);
        toggleEdgeOfScreenMode = new KeyMapping("key.hitindication.toggle_edge_of_screen_mode",
                InputConstants.KEY_G, category);
        return List.of(toggleHitIndication, toggleBlockIndicators, toggleEdgeOfScreenMode);
    }

    /** Runs at the start of every client tick, as the original's ClientTickEvent (phase START) did. */
    public static void onClientTick(Minecraft minecraft) {
        if(minecraft.player == null || minecraft.level == null) {
            lastPlayer = null;
            return;
        }

        // Respawning replaces the player (in the same dimension the level stays), and the hits
        // that led up to a death should not follow the player into the new life.
        if(minecraft.player != lastPlayer) {
            if(lastPlayer != null)
                ClientLatestHits.clear();
            lastPlayer = minecraft.player;
        }

        HitIndicatorClientConfigs.Values config = HitIndicatorClientConfigs.get();

        if(toggleHitIndication.consumeClick())
            config.setEnableHitIndication(!config.enableHitIndication());

        if(toggleBlockIndicators.consumeClick())
            config.setShowBlueIndicators(!config.showBlueIndicators());

        if(toggleEdgeOfScreenMode.consumeClick())
            config.setEdgeOfScreenMode(!config.edgeOfScreenMode());

        ClientLatestHits.tick();
    }
}
