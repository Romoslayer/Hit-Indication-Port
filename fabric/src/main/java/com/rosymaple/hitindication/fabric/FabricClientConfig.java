package com.rosymaple.hitindication.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import com.rosymaple.hitindication.config.HitIndicatorClientConfigs.Values;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.rosymaple.hitindication.config.HitIndicatorClientConfigs.*;

/**
 * Fabric storage for the client config: {@code config/hitindication-client.json}. Same options,
 * defaults and ranges as the NeoForge build's {@code hitindication-client.toml}.
 */
public final class FabricClientConfig implements Values {
    private static final Logger LOGGER = LoggerFactory.getLogger("Hit Indication");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private Data data = new Data();

    private FabricClientConfig(Path path) {
        this.path = path;
    }

    public static FabricClientConfig load() {
        FabricClientConfig config = new FabricClientConfig(FabricLoader.getInstance().getConfigDir().resolve("hitindication-client.json"));
        config.read();
        return config;
    }

    /** The plain fields the file holds. Public so the config screen can edit them in place. */
    public static final class Data {
        public boolean enableHitIndication = DEFAULT_ENABLE_HIT_INDICATION;
        public int maxIndicatorCount = DEFAULT_MAX_INDICATOR_COUNT;
        public boolean displayHitsFromNegativePotions = DEFAULT_DISPLAY_HITS_FROM_NEGATIVE_POTIONS;
        public int indicatorFadeRateTicks = DEFAULT_FADE_RATE;
        public int indicatorOpacity = DEFAULT_INDICATOR_OPACITY;
        public boolean showBlockIndicator = DEFAULT_SHOW_BLUE_INDICATORS;
        public boolean heavyDamageMakesIndicatorLarger = DEFAULT_SIZE_DEPENDS_ON_DAMAGE;
        public int indicatorDefaultScale = DEFAULT_INDICATOR_DEFAULT_SCALE;
        public int distanceFromCrosshair = DEFAULT_DISTANCE_FROM_CROSSHAIR;
        public boolean enableCritKillMarkers = DEFAULT_ENABLE_HIT_MARKERS;
        public boolean enableNonDirectionalDamage = DEFAULT_ENABLE_NON_DIRECTIONAL_DAMAGE;
        public boolean enableDistanceScaling = DEFAULT_ENABLE_DISTANCE_SCALING;
        public int distanceScalingCutoff = DEFAULT_DISTANCE_SCALING_CUTOFF;
        public boolean edgeOfScreenMode = DEFAULT_EDGE_OF_SCREEN_MODE;
        public String hitIndicatorColor = DEFAULT_HIT_INDICATOR_COLOR;
        public String blockIndicatorColor = DEFAULT_BLOCK_INDICATOR_COLOR;

        /** Pulls every value back into the range the original's config spec allowed. */
        void clamp() {
            maxIndicatorCount = Math.max(maxIndicatorCount, 0);
            indicatorFadeRateTicks = Math.max(indicatorFadeRateTicks, 0);
            indicatorOpacity = Mth.clamp(indicatorOpacity, 0, 100);
            indicatorDefaultScale = Mth.clamp(indicatorDefaultScale, 0, 100);
            distanceFromCrosshair = Math.max(distanceFromCrosshair, MIN_DISTANCE_FROM_CROSSHAIR);
            distanceScalingCutoff = Math.max(distanceScalingCutoff, 0);
            if (hitIndicatorColor == null) hitIndicatorColor = DEFAULT_HIT_INDICATOR_COLOR;
            if (blockIndicatorColor == null) blockIndicatorColor = DEFAULT_BLOCK_INDICATOR_COLOR;
        }
    }

    public Data data() {
        return data;
    }

    private void read() {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null)
                    data = loaded;
            } catch (IOException | JsonParseException e) {
                LOGGER.error("Could not read {}, using defaults", path, e);
            }
        }
        data.clamp();
        // Rewrite so new options appear in the file and out-of-range values are corrected.
        save();
    }

    public void save() {
        data.clamp();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
        }
    }

    @Override public boolean enableHitIndication() { return data.enableHitIndication; }
    @Override public void setEnableHitIndication(boolean value) { data.enableHitIndication = value; save(); }
    @Override public int maxIndicatorCount() { return data.maxIndicatorCount; }
    @Override public boolean displayHitsFromNegativePotions() { return data.displayHitsFromNegativePotions; }
    @Override public int fadeRate() { return data.indicatorFadeRateTicks; }
    @Override public int indicatorOpacity() { return data.indicatorOpacity; }
    @Override public boolean showBlueIndicators() { return data.showBlockIndicator; }
    @Override public void setShowBlueIndicators(boolean value) { data.showBlockIndicator = value; save(); }
    @Override public boolean sizeDependsOnDamage() { return data.heavyDamageMakesIndicatorLarger; }
    @Override public int indicatorDefaultScale() { return data.indicatorDefaultScale; }
    @Override public int distanceFromCrosshair() { return data.distanceFromCrosshair; }
    @Override public boolean enableHitMarkers() { return data.enableCritKillMarkers; }
    @Override public boolean enableNonDirectionalDamage() { return data.enableNonDirectionalDamage; }
    @Override public boolean enableDistanceScaling() { return data.enableDistanceScaling; }
    @Override public int distanceScalingCutoff() { return data.distanceScalingCutoff; }
    @Override public boolean edgeOfScreenMode() { return data.edgeOfScreenMode; }
    @Override public void setEdgeOfScreenMode(boolean value) { data.edgeOfScreenMode = value; save(); }
    @Override public String hitIndicatorColor() { return data.hitIndicatorColor; }
    @Override public String blockIndicatorColor() { return data.blockIndicatorColor; }

    public static FabricClientConfig instance() {
        return (FabricClientConfig) HitIndicatorClientConfigs.get();
    }
}
