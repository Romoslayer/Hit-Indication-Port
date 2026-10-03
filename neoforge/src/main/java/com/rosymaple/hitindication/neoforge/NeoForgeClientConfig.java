package com.rosymaple.hitindication.neoforge;

import com.rosymaple.hitindication.config.HitIndicatorClientConfigs.Values;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.rosymaple.hitindication.config.HitIndicatorClientConfigs.*;

/**
 * The original mod's ForgeConfigSpec, carried over as a ModConfigSpec with the same section, keys,
 * comments, defaults and ranges, so an existing {@code hitindication-client.toml} still loads.
 */
public final class NeoForgeClientConfig implements Values {
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
    // reloads it on another thread, holding a lock our set() cannot share. A reload that read the file
    // just before one of our saves swaps the older values back in, which could undo a quick second key
    // press. So the mod reads these copies instead. They follow the config on load, on saves made on
    // the game thread (the config screen) and on edits to the file; a watcher reload that matches one
    // of our own recent saves is only an echo or a stale read of the file, and is not applied.
    private static final long OWN_SAVE_ECHO_MILLIS = 5000;
    private volatile boolean enableHitIndication = DEFAULT_ENABLE_HIT_INDICATION;
    private volatile boolean showBlueIndicators = DEFAULT_SHOW_BLUE_INDICATORS;
    private volatile boolean edgeOfScreenMode = DEFAULT_EDGE_OF_SCREEN_MODE;
    private final Deque<long[]> recentOwnSaves = new ArrayDeque<>(); // {time, toggle bits}
    private boolean savingOwnChange;

    /** Mod-bus listener for ModConfigEvent.Loading and ModConfigEvent.Reloading. */
    public void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC || savingOwnChange)
            return;

        boolean enable = EnableHitIndication.get();
        boolean showBlue = ShowBlueIndicators.get();
        boolean edge = EdgeOfScreenMode.get();
        if (event instanceof ModConfigEvent.Reloading && !isGameThread() && isRecentOwnSave(bits(enable, showBlue, edge))) {
            // The file already holds our latest values; put them back in memory as well, so the config
            // screen shows them.
            EnableHitIndication.set(enableHitIndication);
            ShowBlueIndicators.set(showBlueIndicators);
            EdgeOfScreenMode.set(edgeOfScreenMode);
            return;
        }

        enableHitIndication = enable;
        showBlueIndicators = showBlue;
        edgeOfScreenMode = edge;
    }

    private static long bits(boolean enable, boolean showBlue, boolean edge) {
        return (enable ? 1 : 0) | (showBlue ? 2 : 0) | (edge ? 4 : 0);
    }

    private synchronized void rememberOwnSave() {
        recentOwnSaves.addLast(new long[] {System.currentTimeMillis(), bits(enableHitIndication, showBlueIndicators, edgeOfScreenMode)});
    }

    private synchronized boolean isRecentOwnSave(long toggleBits) {
        long now = System.currentTimeMillis();
        recentOwnSaves.removeIf(save -> now - save[0] > OWN_SAVE_ECHO_MILLIS);
        return recentOwnSaves.stream().anyMatch(save -> save[1] == toggleBits);
    }

    private static boolean isGameThread() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.isSameThread();
    }

    private void saveToggles() {
        savingOwnChange = true;
        try {
            EnableHitIndication.set(enableHitIndication);
            ShowBlueIndicators.set(showBlueIndicators);
            EdgeOfScreenMode.set(edgeOfScreenMode);
            rememberOwnSave();
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
