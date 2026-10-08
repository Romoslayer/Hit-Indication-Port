package com.rosymaple.hitindication.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.latesthits.ClientLatestHits;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * The three toggle keys. The loader registers the mappings this class builds. On 1.21.1 a key
 * category is just the translation key of its heading on the Controls screen.
 */
public class ModKeyBindings {
    // The key 26.x derives from its category id "hitindication:hitindication", so the
    // translations are shared.
    public static final String CATEGORY = "key.category.hitindication.hitindication";

    public static KeyMapping toggleHitIndication;
    public static KeyMapping toggleBlockIndicators;
    public static KeyMapping toggleEdgeOfScreenMode;

    private static LocalPlayer lastPlayer;

    public static List<KeyMapping> create() {
        toggleHitIndication = new KeyMapping("key.hitindication.toggle_hit_indication",
                InputConstants.KEY_H, CATEGORY);
        toggleBlockIndicators = new KeyMapping("key.hitindication.toggle_block_indicators",
                InputConstants.KEY_B, CATEGORY);
        toggleEdgeOfScreenMode = new KeyMapping("key.hitindication.toggle_edge_of_screen_mode",
                InputConstants.KEY_G, CATEGORY);
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
