package com.rosymaple.hitindication.latesthits;

import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import net.minecraft.world.phys.Vec3;

public class HitIndicator {
    private final Vec3 damageSourceLocation;
    private final HitIndicatorType hitIndicatorType;
    private int lifetime;
    private final int damagePercent;

    public HitIndicator(double x, double y, double z, HitIndicatorType hitIndicatorType, int damagePercent) {
        this.damageSourceLocation = new Vec3(x, y, z);
        this.hitIndicatorType = hitIndicatorType;
        this.damagePercent = damagePercent;
        lifetime = hitIndicatorType == HitIndicatorType.ND_HIT ? 25 : HitIndicatorClientConfigs.get().fadeRate();
    }

    public void tick() {
        lifetime--;
    }
    public boolean expired() {
        return lifetime <= 0;
    }
    public Vec3 getLocation() {
        return damageSourceLocation;
    }
    public int getLifeTime() { return lifetime; }
    public HitIndicatorType getType() { return hitIndicatorType; }
    public int getDamagePercent() { return damagePercent; }
}
