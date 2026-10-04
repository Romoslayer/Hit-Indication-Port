package com.rosymaple.hitindication.latesthits;

public class HitIndicator {
    private final double x;
    private final double y;
    private final double z;
    private final HitIndicatorType hitIndicatorType;
    private int lifetime;
    private final int damagePercent;

    public HitIndicator(double x, double y, double z, HitIndicatorType hitIndicatorType, int damagePercent, int fadeRate) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hitIndicatorType = hitIndicatorType;
        this.damagePercent = damagePercent;
        // The non-directional indicator always lasted 25 ticks in the original, whatever the fade rate.
        lifetime = hitIndicatorType == HitIndicatorType.ND_HIT ? 25 : fadeRate;
    }

    public void tick() {
        lifetime--;
    }
    public boolean expired() {
        return lifetime <= 0;
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getLifeTime() { return lifetime; }
    public HitIndicatorType getType() { return hitIndicatorType; }
    public int getDamagePercent() { return damagePercent; }
}
