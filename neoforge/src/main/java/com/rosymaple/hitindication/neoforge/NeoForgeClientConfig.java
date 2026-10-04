package com.rosymaple.hitindication.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.logging.LogUtils;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs.Values;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.rosymaple.hitindication.config.HitIndicatorClientConfigs.*;

/**
 * The original mod's ForgeConfigSpec, carried over as a ModConfigSpec with the same section, keys,
 * comments, defaults and ranges, so an existing {@code hitindication-client.toml} still loads.
 */
public final class NeoForgeClientConfig implements Values {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> MaxIndicatorCount;
    public static final ModConfigSpec.ConfigValue<Boolean> DisplayHitsFromNegativePotions;
    public static final ModConfigSpec.ConfigValue<Integer> FadeRate;
    public static final ModConfigSpec.ConfigValue<Integer> IndicatorOpacity;
    public static final ModConfigSpec.ConfigValue<Boolean> ShowBlueIndicators;
    public static final ModConfigSpec.ConfigValue<Boolean> SizeDependsOnDamage;
    public static final ModConfigSpec.ConfigValue<Integer> IndicatorDefaultScale;
    public static final ModConfigSpec.ConfigValue<Boolean> EnableHitMarkers;
    public static final ModConfigSpec.ConfigValue<Boolean> EnableHitIndication;
    public static final ModConfigSpec.ConfigValue<Boolean> EnableDistanceScaling;
    public static final ModConfigSpec.ConfigValue<Integer> DistanceScalingCutoff;
    public static final ModConfigSpec.ConfigValue<Boolean> EnableNonDirectionalDamage;
    public static final ModConfigSpec.ConfigValue<Integer> DistanceFromCrosshair;
    public static final ModConfigSpec.ConfigValue<Boolean> EdgeOfScreenMode;
    public static final ModConfigSpec.ConfigValue<String> HitIndicatorColor;
    public static final ModConfigSpec.ConfigValue<String> BlockIndicatorColor;

    static {
        BUILDER.translation("hitindication.configgui.category").push("Hit Indication Config");

        EnableHitIndication = BUILDER.comment("Enables hit indication.")
                .translation("hitindication.configgui.enable_hit_indication")
                .define("Enable Hit Indication", DEFAULT_ENABLE_HIT_INDICATION);

        MaxIndicatorCount = BUILDER.comment("Determines maximum indicator count shown on screen (0 = unlimited).")
                .translation("hitindication.configgui.max_indicator_count")
                .defineInRange("Max Indicator Count", DEFAULT_MAX_INDICATOR_COUNT, 0, Integer.MAX_VALUE);

        DisplayHitsFromNegativePotions = BUILDER.comment("Shows red indicator when an entity hits the player with a non-damaging negative potion.")
                .translation("hitindication.configgui.display_hits_from_negative_potions")
                .define("Display Hits From Non-Damaging Negative Potions", DEFAULT_DISPLAY_HITS_FROM_NEGATIVE_POTIONS);

        FadeRate = BUILDER.comment("Amount of ticks after which indicator disappears.")
                .translation("hitindication.configgui.fade_rate")
                .defineInRange("Indicator Fade Rate (Ticks)", DEFAULT_FADE_RATE, 0, Integer.MAX_VALUE);

        IndicatorOpacity = BUILDER.comment("Determines opacity of the indicators.")
                .translation("hitindication.configgui.indicator_opacity")
                .defineInRange("Indicator Opacity (0-100)", DEFAULT_INDICATOR_OPACITY, 0, 100);

        ShowBlueIndicators = BUILDER.comment("Shows blue indicator when the player blocks incoming damage with a shield.")
                .translation("hitindication.configgui.display_blue_indicators")
                .define("Show Block Indicator", DEFAULT_SHOW_BLUE_INDICATORS);

        SizeDependsOnDamage = BUILDER.comment("Any instance of damage that deals 30 percent or more of max health will result in larger indicators.")
                .translation("hitindication.configgui.size_depends_on_damage")
                .define("Heavy damage makes indicator larger", DEFAULT_SIZE_DEPENDS_ON_DAMAGE);

        IndicatorDefaultScale = BUILDER.comment("Determines scale of indicators.")
                .translation("hitindication.configgui.indicator_default_scale")
                .defineInRange("Indicator Default Scale (0-100)", DEFAULT_INDICATOR_DEFAULT_SCALE, 0, 100);

        DistanceFromCrosshair = BUILDER.comment("Determines distance of an indicator from crosshair.")
                .translation("hitindication.configgui.distance_from_crosshair")
                .defineInRange("Distance From Crosshair", DEFAULT_DISTANCE_FROM_CROSSHAIR, MIN_DISTANCE_FROM_CROSSHAIR, Integer.MAX_VALUE);

        EnableHitMarkers = BUILDER.comment("Enables hit markers on crit/kill.")
                .translation("hitindication.configgui.enable_hit_markers")
                .define("Enable Crit/Kill Markers", DEFAULT_ENABLE_HIT_MARKERS);

        EnableNonDirectionalDamage = BUILDER.comment("Shows a special indicator when hit direction can't be determined")
                .translation("hitindication.configgui.enable_non_directional_damage")
                .define("Enable Non Directional Damage", DEFAULT_ENABLE_NON_DIRECTIONAL_DAMAGE);

        EnableDistanceScaling = BUILDER.comment("Scale of the indicator depends on the distance")
                .translation("hitindication.configgui.enable_distance_scaling")
                .define("Enable Distance Scaling", DEFAULT_ENABLE_DISTANCE_SCALING);

        DistanceScalingCutoff = BUILDER.comment("Distance from entity after which indicator starts to become gradually smaller.")
                .translation("hitindication.configgui.distance_scaling_cutoff")
                .defineInRange("Distance Scaling Cutoff", DEFAULT_DISTANCE_SCALING_CUTOFF, 0, Integer.MAX_VALUE);

        EdgeOfScreenMode = BUILDER.comment("The indicators will appear on the edges of your screen instead of near the crosshair")
                .translation("hitindication.configgui.edge_of_screen_mode")
                .define("Edge of Screen Mode", DEFAULT_EDGE_OF_SCREEN_MODE);

        HitIndicatorColor = BUILDER.comment("Determines the color of the hit indicator")
                .translation("hitindication.configgui.hit_indicator_color")
                .define("Hit Indicator Color", DEFAULT_HIT_INDICATOR_COLOR);

        BlockIndicatorColor = BUILDER.comment("Determines the color of the block indicator")
                .translation("hitindication.configgui.block_indicator_color")
                .define("Block Indicator Color", DEFAULT_BLOCK_INDICATOR_COLOR);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // The three options the toggle keys change. Saving writes the file, and FML's file watcher then
    // reloads it on another thread, holding a lock our saves cannot share. A reload that read the file
    // just before one of our saves swaps the older values back in, which could undo a quick second key
    // press. So the mod reads these copies instead, and only ever changes them on the game thread:
    // on the key press itself, on a save from the config screen, and when the file watcher reports
    // a change. For the last, the game thread reads the file again: our own saves are made on that
    // thread too, so by then the file holds either our latest save or a newer edit from outside the
    // game, never a stale state, and can simply be applied.
    private volatile boolean enableHitIndication = DEFAULT_ENABLE_HIT_INDICATION;
    private volatile boolean showBlueIndicators = DEFAULT_SHOW_BLUE_INDICATORS;
    private volatile boolean edgeOfScreenMode = DEFAULT_EDGE_OF_SCREEN_MODE;
    private volatile @Nullable Path configPath;
    private boolean savingOwnChange; // game thread only

    /** Mod-bus listener for ModConfigEvent.Loading and ModConfigEvent.Reloading. */
    public void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC)
            return;
        configPath = event.getConfig().getFullPath();

        Minecraft minecraft = Minecraft.getInstance();
        if (event instanceof ModConfigEvent.Reloading && minecraft != null && !minecraft.isSameThread()) {
            // The file watcher. What it read may already be out of date: decide on the game thread.
            minecraft.execute(this::reloadTogglesFromFile);
            return;
        }
        if (savingOwnChange)
            return;

        // Initial load, or a save made on the game thread by the config screen.
        enableHitIndication = EnableHitIndication.get();
        showBlueIndicators = ShowBlueIndicators.get();
        edgeOfScreenMode = EdgeOfScreenMode.get();
    }

    /** Game thread: applies the toggle values the config file holds right now. */
    private void reloadTogglesFromFile() {
        Path path = configPath;
        if (path == null)
            return;

        CommentedConfig file;
        try {
            file = new TomlParser().parse(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            // Half-written by an editor, or briefly missing. The watcher reports the next change.
            LOGGER.debug("Could not re-read {} after a change", path, e);
            return;
        }

        enableHitIndication = readToggle(file, EnableHitIndication, enableHitIndication);
        showBlueIndicators = readToggle(file, ShowBlueIndicators, showBlueIndicators);
        edgeOfScreenMode = readToggle(file, EdgeOfScreenMode, edgeOfScreenMode);
        // The watcher may have loaded older values into the spec; show the current ones on the
        // config screen too. The file already holds them, so nothing is written.
        EnableHitIndication.set(enableHitIndication);
        ShowBlueIndicators.set(showBlueIndicators);
        EdgeOfScreenMode.set(edgeOfScreenMode);
    }

    private static boolean readToggle(CommentedConfig file, ModConfigSpec.ConfigValue<Boolean> value, boolean fallback) {
        Object fileValue = file.get(value.getPath());
        return fileValue instanceof Boolean bool ? bool : fallback;
    }

    private void saveToggles() {
        savingOwnChange = true;
        try {
            EnableHitIndication.set(enableHitIndication);
            ShowBlueIndicators.set(showBlueIndicators);
            EdgeOfScreenMode.set(edgeOfScreenMode);
            SPEC.save();
        } finally {
            savingOwnChange = false;
        }
    }

    @Override public boolean enableHitIndication() { return enableHitIndication; }
    @Override public void setEnableHitIndication(boolean value) { enableHitIndication = value; saveToggles(); }
    @Override public int maxIndicatorCount() { return MaxIndicatorCount.get(); }
    @Override public boolean displayHitsFromNegativePotions() { return DisplayHitsFromNegativePotions.get(); }
    @Override public int fadeRate() { return FadeRate.get(); }
    @Override public int indicatorOpacity() { return IndicatorOpacity.get(); }
    @Override public boolean showBlueIndicators() { return showBlueIndicators; }
    @Override public void setShowBlueIndicators(boolean value) { showBlueIndicators = value; saveToggles(); }
    @Override public boolean sizeDependsOnDamage() { return SizeDependsOnDamage.get(); }
    @Override public int indicatorDefaultScale() { return IndicatorDefaultScale.get(); }
    @Override public int distanceFromCrosshair() { return DistanceFromCrosshair.get(); }
    @Override public boolean enableHitMarkers() { return EnableHitMarkers.get(); }
    @Override public boolean enableNonDirectionalDamage() { return EnableNonDirectionalDamage.get(); }
    @Override public boolean enableDistanceScaling() { return EnableDistanceScaling.get(); }
    @Override public int distanceScalingCutoff() { return DistanceScalingCutoff.get(); }
    @Override public boolean edgeOfScreenMode() { return edgeOfScreenMode; }
    @Override public void setEdgeOfScreenMode(boolean value) { edgeOfScreenMode = value; saveToggles(); }
    @Override public String hitIndicatorColor() { return HitIndicatorColor.get(); }
    @Override public String blockIndicatorColor() { return BlockIndicatorColor.get(); }
}
