package com.rosymaple.hitindication.config;

/**
 * Client config, read by the shared rendering and hit-tracking code. Each loader supplies the
 * storage: NeoForge keeps the original {@code hitindication-client.toml} via ModConfigSpec, Fabric
 * keeps {@code hitindication-client.json}. Both use the names, defaults and ranges below, which are
 * the original mod's.
 */
public final class HitIndicatorClientConfigs {
    public static final boolean DEFAULT_ENABLE_HIT_INDICATION = true;
    public static final int DEFAULT_MAX_INDICATOR_COUNT = 0;
    public static final boolean DEFAULT_DISPLAY_HITS_FROM_NEGATIVE_POTIONS = false;
    public static final int DEFAULT_FADE_RATE = 50;
    public static final int DEFAULT_INDICATOR_OPACITY = 25;
    public static final boolean DEFAULT_SHOW_BLUE_INDICATORS = true;
    public static final boolean DEFAULT_SIZE_DEPENDS_ON_DAMAGE = false;
    public static final int DEFAULT_INDICATOR_DEFAULT_SCALE = 25;
    public static final int DEFAULT_DISTANCE_FROM_CROSSHAIR = 30;
    public static final boolean DEFAULT_ENABLE_HIT_MARKERS = false;
    public static final boolean DEFAULT_ENABLE_NON_DIRECTIONAL_DAMAGE = false;
    public static final boolean DEFAULT_ENABLE_DISTANCE_SCALING = true;
    public static final int DEFAULT_DISTANCE_SCALING_CUTOFF = 10;
    public static final boolean DEFAULT_EDGE_OF_SCREEN_MODE = false;
    public static final String DEFAULT_HIT_INDICATOR_COLOR = "FF0000";
    public static final String DEFAULT_BLOCK_INDICATOR_COLOR = "0000FF";

    public static final int MIN_DISTANCE_FROM_CROSSHAIR = 30;

    private static Values values = new Defaults();

    private HitIndicatorClientConfigs() {
    }

    public static Values get() {
        return values;
    }

    /** Installed by the loader's client entrypoint. */
    public static void set(Values values) {
        HitIndicatorClientConfigs.values = values;
    }

    public interface Values {
        boolean enableHitIndication();
        void setEnableHitIndication(boolean value);
        int maxIndicatorCount();
        boolean displayHitsFromNegativePotions();
        int fadeRate();
        int indicatorOpacity();
        boolean showBlueIndicators();
        void setShowBlueIndicators(boolean value);
        boolean sizeDependsOnDamage();
        int indicatorDefaultScale();
        int distanceFromCrosshair();
        boolean enableHitMarkers();
        boolean enableNonDirectionalDamage();
        boolean enableDistanceScaling();
        int distanceScalingCutoff();
        boolean edgeOfScreenMode();
        void setEdgeOfScreenMode(boolean value);
        String hitIndicatorColor();
        String blockIndicatorColor();
    }

    /** Used until a loader installs real storage, and on a dedicated server where nothing does. */
    private static final class Defaults implements Values {
        @Override public boolean enableHitIndication() { return DEFAULT_ENABLE_HIT_INDICATION; }
        @Override public void setEnableHitIndication(boolean value) { }
        @Override public int maxIndicatorCount() { return DEFAULT_MAX_INDICATOR_COUNT; }
        @Override public boolean displayHitsFromNegativePotions() { return DEFAULT_DISPLAY_HITS_FROM_NEGATIVE_POTIONS; }
        @Override public int fadeRate() { return DEFAULT_FADE_RATE; }
        @Override public int indicatorOpacity() { return DEFAULT_INDICATOR_OPACITY; }
        @Override public boolean showBlueIndicators() { return DEFAULT_SHOW_BLUE_INDICATORS; }
        @Override public void setShowBlueIndicators(boolean value) { }
        @Override public boolean sizeDependsOnDamage() { return DEFAULT_SIZE_DEPENDS_ON_DAMAGE; }
        @Override public int indicatorDefaultScale() { return DEFAULT_INDICATOR_DEFAULT_SCALE; }
        @Override public int distanceFromCrosshair() { return DEFAULT_DISTANCE_FROM_CROSSHAIR; }
        @Override public boolean enableHitMarkers() { return DEFAULT_ENABLE_HIT_MARKERS; }
        @Override public boolean enableNonDirectionalDamage() { return DEFAULT_ENABLE_NON_DIRECTIONAL_DAMAGE; }
        @Override public boolean enableDistanceScaling() { return DEFAULT_ENABLE_DISTANCE_SCALING; }
        @Override public int distanceScalingCutoff() { return DEFAULT_DISTANCE_SCALING_CUTOFF; }
        @Override public boolean edgeOfScreenMode() { return DEFAULT_EDGE_OF_SCREEN_MODE; }
        @Override public void setEdgeOfScreenMode(boolean value) { }
        @Override public String hitIndicatorColor() { return DEFAULT_HIT_INDICATOR_COLOR; }
        @Override public String blockIndicatorColor() { return DEFAULT_BLOCK_INDICATOR_COLOR; }
    }
}
