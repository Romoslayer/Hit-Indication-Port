package com.rosymaple.hitindication.fabric.compat;

import com.rosymaple.hitindication.fabric.FabricClientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static com.rosymaple.hitindication.config.HitIndicatorClientConfigs.*;

/** The Mod Menu config screen, built with Cloth Config. Edits {@code hitindication-client.json}. */
public final class ClothConfigScreen {
    private ClothConfigScreen() {
    }

    private static Component name(String key) {
        return Component.translatable("hitindication.configgui." + key);
    }

    private static Component tooltip(String key) {
        return Component.translatable("hitindication.configgui." + key + ".tooltip");
    }

    public static Screen create(Screen parent) {
        FabricClientConfig config = FabricClientConfig.instance();
        FabricClientConfig.Data data = config.data();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("hitindication.configgui.title"))
                .setSavingRunnable(config::save);
        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("hitindication.configgui.category"));

        general.addEntry(entry.startBooleanToggle(name("enable_hit_indication"), data.enableHitIndication)
                .setDefaultValue(DEFAULT_ENABLE_HIT_INDICATION)
                .setTooltip(tooltip("enable_hit_indication"))
                .setSaveConsumer(v -> data.enableHitIndication = v)
                .build());
        general.addEntry(entry.startIntField(name("max_indicator_count"), data.maxIndicatorCount)
                .setDefaultValue(DEFAULT_MAX_INDICATOR_COUNT)
                .setMin(0)
                .setTooltip(tooltip("max_indicator_count"))
                .setSaveConsumer(v -> data.maxIndicatorCount = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("display_hits_from_negative_potions"), data.displayHitsFromNegativePotions)
                .setDefaultValue(DEFAULT_DISPLAY_HITS_FROM_NEGATIVE_POTIONS)
                .setTooltip(tooltip("display_hits_from_negative_potions"))
                .setSaveConsumer(v -> data.displayHitsFromNegativePotions = v)
                .build());
        general.addEntry(entry.startIntField(name("fade_rate"), data.indicatorFadeRateTicks)
                .setDefaultValue(DEFAULT_FADE_RATE)
                .setMin(0)
                .setTooltip(tooltip("fade_rate"))
                .setSaveConsumer(v -> data.indicatorFadeRateTicks = v)
                .build());
        general.addEntry(entry.startIntSlider(name("indicator_opacity"), data.indicatorOpacity, 0, 100)
                .setDefaultValue(DEFAULT_INDICATOR_OPACITY)
                .setTooltip(tooltip("indicator_opacity"))
                .setSaveConsumer(v -> data.indicatorOpacity = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("display_blue_indicators"), data.showBlockIndicator)
                .setDefaultValue(DEFAULT_SHOW_BLUE_INDICATORS)
                .setTooltip(tooltip("display_blue_indicators"))
                .setSaveConsumer(v -> data.showBlockIndicator = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("size_depends_on_damage"), data.heavyDamageMakesIndicatorLarger)
                .setDefaultValue(DEFAULT_SIZE_DEPENDS_ON_DAMAGE)
                .setTooltip(tooltip("size_depends_on_damage"))
                .setSaveConsumer(v -> data.heavyDamageMakesIndicatorLarger = v)
                .build());
        general.addEntry(entry.startIntSlider(name("indicator_default_scale"), data.indicatorDefaultScale, 0, 100)
                .setDefaultValue(DEFAULT_INDICATOR_DEFAULT_SCALE)
                .setTooltip(tooltip("indicator_default_scale"))
                .setSaveConsumer(v -> data.indicatorDefaultScale = v)
                .build());
        general.addEntry(entry.startIntField(name("distance_from_crosshair"), data.distanceFromCrosshair)
                .setDefaultValue(DEFAULT_DISTANCE_FROM_CROSSHAIR)
                .setMin(MIN_DISTANCE_FROM_CROSSHAIR)
                .setTooltip(tooltip("distance_from_crosshair"))
                .setSaveConsumer(v -> data.distanceFromCrosshair = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("enable_hit_markers"), data.enableCritKillMarkers)
                .setDefaultValue(DEFAULT_ENABLE_HIT_MARKERS)
                .setTooltip(tooltip("enable_hit_markers"))
                .setSaveConsumer(v -> data.enableCritKillMarkers = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("enable_non_directional_damage"), data.enableNonDirectionalDamage)
                .setDefaultValue(DEFAULT_ENABLE_NON_DIRECTIONAL_DAMAGE)
                .setTooltip(tooltip("enable_non_directional_damage"))
                .setSaveConsumer(v -> data.enableNonDirectionalDamage = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("enable_distance_scaling"), data.enableDistanceScaling)
                .setDefaultValue(DEFAULT_ENABLE_DISTANCE_SCALING)
                .setTooltip(tooltip("enable_distance_scaling"))
                .setSaveConsumer(v -> data.enableDistanceScaling = v)
                .build());
        general.addEntry(entry.startIntField(name("distance_scaling_cutoff"), data.distanceScalingCutoff)
                .setDefaultValue(DEFAULT_DISTANCE_SCALING_CUTOFF)
                .setMin(0)
                .setTooltip(tooltip("distance_scaling_cutoff"))
                .setSaveConsumer(v -> data.distanceScalingCutoff = v)
                .build());
        general.addEntry(entry.startBooleanToggle(name("edge_of_screen_mode"), data.edgeOfScreenMode)
                .setDefaultValue(DEFAULT_EDGE_OF_SCREEN_MODE)
                .setTooltip(tooltip("edge_of_screen_mode"))
                .setSaveConsumer(v -> data.edgeOfScreenMode = v)
                .build());
        general.addEntry(entry.startColorField(name("hit_indicator_color"), parseColor(data.hitIndicatorColor, 0xFF0000))
                .setDefaultValue(0xFF0000)
                .setTooltip(tooltip("hit_indicator_color"))
                .setSaveConsumer(v -> data.hitIndicatorColor = formatColor(v))
                .build());
        general.addEntry(entry.startColorField(name("block_indicator_color"), parseColor(data.blockIndicatorColor, 0x0000FF))
                .setDefaultValue(0x0000FF)
                .setTooltip(tooltip("block_indicator_color"))
                .setSaveConsumer(v -> data.blockIndicatorColor = formatColor(v))
                .build());

        return builder.build();
    }

    private static int parseColor(String hex, int fallback) {
        try {
            return Integer.parseInt(hex.trim(), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String formatColor(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }
}
