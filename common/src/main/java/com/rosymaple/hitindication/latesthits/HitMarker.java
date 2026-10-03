package com.rosymaple.hitindication.latesthits;

public class HitMarker {
    private final HitMarkerType hitMarkerType;
    private int lifetime;

    public HitMarker(HitMarkerType hitMarkerType) {
        this.hitMarkerType = hitMarkerType;
        lifetime = 9;
    }

    public void tick() {
        lifetime--;
    }
    public boolean expired() {
        return lifetime <= 0;
    }
    public int getLifeTime() { return lifetime; }

    public HitMarkerType getType() { return hitMarkerType; }
}
