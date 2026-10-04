package com.rosymaple.hitindication.latesthits;

import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;

import java.util.ArrayList;

public class ClientLatestHits {
    /** Highest damage percentage accepted from a server; anything above draws the same anyway. */
    static final int MAX_DAMAGE_PERCENT = 1000;

    public static final ArrayList<HitIndicator> latestHitIndicators = new ArrayList<>();
    public static HitMarker currentHitMarker = null;

    public static void addHitIndicator(double x, double y, double z, int type, int damagePercent, boolean hasNegativeEffects) {
        HitIndicatorType hitIndicatorType = HitIndicatorType.fromInt(type);
        HitIndicatorClientConfigs.Values config = HitIndicatorClientConfigs.get();

        // Defensive: a broken or mismatched server must not feed NaN or unknown types into the HUD.
        if(hitIndicatorType == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            return;
        if(!config.enableHitIndication())
            return;
        if(hitIndicatorType == HitIndicatorType.ND_HIT && !config.enableNonDirectionalDamage())
            return;
        if(hitIndicatorType == HitIndicatorType.BLOCK && !config.showBlueIndicators())
            return;
        if(hasNegativeEffects && !config.displayHitsFromNegativePotions())
            return;

        damagePercent = Math.clamp(damagePercent, 0, MAX_DAMAGE_PERCENT);
        latestHitIndicators.add(new HitIndicator(x, y, z, hitIndicatorType, damagePercent, config.fadeRate()));
        trimToCap(config.maxIndicatorCount());
    }

    /** Drops the oldest indicators beyond {@code maxCount}, all at once (0 = unlimited). */
    static void trimToCap(int maxCount) {
        int excess = latestHitIndicators.size() - maxCount;
        if(maxCount > 0 && excess > 0)
            latestHitIndicators.subList(0, excess).clear();
    }

    public static void setHitMarker(int type) {
        HitMarkerType hitMarkerType = HitMarkerType.fromInt(type);
        if(hitMarkerType == null)
            return;

        if(!HitIndicatorClientConfigs.get().enableHitMarkers())
            return;

        // A kill outranks a crit: the server can send a crit for the same blow after the kill
        // (a crit, projectile or block marker), and it must not cut the kill animation short.
        if(hitMarkerType == HitMarkerType.CRIT && currentHitMarker != null && currentHitMarker.getType() == HitMarkerType.KILL)
            return;

        currentHitMarker = new HitMarker(hitMarkerType);
    }

    public static void tick() {
        // Applies a lowered cap straight away, not only when the next indicator arrives.
        trimToCap(HitIndicatorClientConfigs.get().maxIndicatorCount());

        latestHitIndicators.removeIf(hitIndicator -> {
            hitIndicator.tick();
            return hitIndicator.expired();
        });

        if(currentHitMarker != null) {
            currentHitMarker.tick();
            if(currentHitMarker.expired())
                currentHitMarker = null;
        }
    }

    public static void clear() {
        latestHitIndicators.clear();
        currentHitMarker = null;
    }
}
