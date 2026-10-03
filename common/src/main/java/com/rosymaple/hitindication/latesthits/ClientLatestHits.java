package com.rosymaple.hitindication.latesthits;

import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;

import java.util.ArrayList;

public class ClientLatestHits {
    public static final ArrayList<HitIndicator> latestHitIndicators = new ArrayList<>();
    public static HitMarker currentHitMarker = null;

    public static void addHitIndicator(double x, double y, double z, int type, int damagePercent, boolean hasNegativeEffects) {
        HitIndicatorType hitIndicatorType = HitIndicatorType.fromInt(type);
        HitIndicatorClientConfigs.Values config = HitIndicatorClientConfigs.get();

        if(!config.enableHitIndication())
            return;
        if(hitIndicatorType == HitIndicatorType.ND_HIT && !config.enableNonDirectionalDamage())
            return;
        if(hitIndicatorType == HitIndicatorType.BLOCK && !config.showBlueIndicators())
            return;
        if(hasNegativeEffects && !config.displayHitsFromNegativePotions())
            return;

        latestHitIndicators.add(new HitIndicator(x, y, z, hitIndicatorType, damagePercent));
        if(config.maxIndicatorCount() > 0 && latestHitIndicators.size() > config.maxIndicatorCount())
            latestHitIndicators.remove(0);
    }

    public static void setHitMarker(int type) {
        HitMarkerType hitMarkerType = HitMarkerType.fromInt(type);

        if(!HitIndicatorClientConfigs.get().enableHitMarkers())
            return;

        currentHitMarker = new HitMarker(hitMarkerType);
    }

    public static void tick() {
        for(int i = latestHitIndicators.size() - 1; i >= 0; i--) {
            HitIndicator hitIndicator = latestHitIndicators.get(i);
            hitIndicator.tick();
            if(hitIndicator.expired())
                latestHitIndicators.remove(i);
        }

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
