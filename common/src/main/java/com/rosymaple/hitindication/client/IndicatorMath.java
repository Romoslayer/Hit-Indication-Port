package com.rosymaple.hitindication.client;

/**
 * The arithmetic behind the HUD: indicator size, direction and edge-of-screen placement. Kept free
 * of Minecraft classes so it can be unit tested, and safe to load on a dedicated server (the server
 * uses {@link #SHIELD_DISABLE_PERCENT}).
 */
public final class IndicatorMath {
    /** Damage, as a percentage of max health, from which "Heavy damage makes indicator larger" applies. */
    public static final int HEAVY_DAMAGE_PERCENT = 30;
    /**
     * The damage percentage a block indicator carries when the blow disables the shield. With
     * "Heavy damage makes indicator larger" on, it draws the indicator at exactly twice its size.
     */
    public static final int SHIELD_DISABLE_PERCENT = 125;
    public static final float MAX_DAMAGE_SCALE = 3.0F;

    /** Offsets closer than this (in blocks, squared) have no usable direction. */
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-8;

    private IndicatorMath() {
    }

    /**
     * Size multiplier for "Heavy damage makes indicator larger": 1 below 30% of max health, then
     * {@code 1 + percent / 125} (1.24 at 30%, 1.8 at 100%, 2 for {@link #SHIELD_DISABLE_PERCENT}),
     * capped at {@link #MAX_DAMAGE_SCALE}. Never shrinks an indicator and never decreases as damage
     * grows.
     */
    public static float damageScale(int damagePercent) {
        if (damagePercent < HEAVY_DAMAGE_PERCENT)
            return 1.0F;
        return Math.min(1.0F + damagePercent / 125.0F, MAX_DAMAGE_SCALE);
    }

    /**
     * Size multiplier for distance scaling: 1 up to {@code cutoff} blocks away, then shrinking
     * linearly to 0 at {@code cutoff + 10} blocks, as in the original.
     */
    public static float distanceScale(double distance, int cutoff) {
        if (!(distance > cutoff))
            return 1.0F;
        double scale = 1.0 - (distance - cutoff) / 10.0;
        return (float)Math.max(0.0, Math.min(scale, 1.0));
    }

    /**
     * The on-screen angle, in degrees within [-180, 180], from straight ahead to a damage source at
     * horizontal offset ({@code dx}, {@code dz}) from a player facing {@code yawDegrees}. Positive is
     * clockwise (to the right), matching the GUI's rotation direction. The offset must be computed in double precision: world
     * coordinates near the border are too large for a float to hold to the block.
     *
     * @return 0 (straight ahead) for an offset too short to have a direction
     */
    public static float angleTo(float yawDegrees, double dx, double dz) {
        if (dx * dx + dz * dz < MIN_DIRECTION_LENGTH_SQR)
            return 0.0F;

        double yaw = Math.toRadians(-yawDegrees);
        double lookX = Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        double dot = lookX * dx + lookZ * dz;
        double cross = lookX * dz - dx * lookZ;
        return (float)Math.toDegrees(Math.atan2(cross, dot));
    }

    /** Where an edge-of-screen indicator's centre goes. */
    public record EdgePosition(float centerX, float centerY) {
    }

    /**
     * Places an edge-of-screen indicator, walking the screen border as the original did: straight
     * ahead is the top centre, 45 degrees the top corners, the side edges run from 45 to 135
     * degrees, and the bottom (above the hotbar) from there to directly behind. The indicator is
     * inset by its rotated bounding box, so no corner leaves the screen at diagonal angles.
     *
     * @param angle         from {@link #angleTo}
     * @param width         unrotated indicator width
     * @param height        unrotated indicator height
     * @param bottomReserve space kept free at the bottom of the screen for the hotbar
     */
    public static EdgePosition edgePosition(float angle, float screenWidth, float screenHeight,
                                            float width, float height, float bottomReserve) {
        double radians = Math.toRadians(angle);
        double cos = Math.abs(Math.cos(radians));
        double sin = Math.abs(Math.sin(radians));
        float halfExtentX = (float)((width * cos + height * sin) / 2.0);
        float halfExtentY = (float)((width * sin + height * cos) / 2.0);

        float centerX = screenWidth / 2.0F;
        float left = Math.min(halfExtentX, centerX);
        float right = Math.max(screenWidth - halfExtentX, centerX);
        float top = halfExtentY;
        float bottom = Math.max(screenHeight - bottomReserve - halfExtentY, top);

        float target = -angle;
        if (target >= 45.0F && target <= 135.0F)
            return new EdgePosition(left, lerp((target - 45.0F) / 90.0F, top, bottom));
        if (target <= -45.0F && target >= -135.0F)
            return new EdgePosition(right, lerp((target + 45.0F) / -90.0F, top, bottom));
        if (target >= 0.0F && target <= 45.0F)
            return new EdgePosition(lerp(target / 45.0F, centerX, left), top);
        if (target >= -45.0F && target <= 0.0F)
            return new EdgePosition(lerp(target / -45.0F, centerX, right), top);
        if (target >= 135.0F)
            return new EdgePosition(lerp((target - 135.0F) / 45.0F, left, centerX), bottom);
        return new EdgePosition(lerp((target + 135.0F) / -45.0F, right, centerX), bottom);
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }
}
