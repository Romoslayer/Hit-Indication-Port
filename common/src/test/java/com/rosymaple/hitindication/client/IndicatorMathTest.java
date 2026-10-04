package com.rosymaple.hitindication.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorMathTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void damageScaleKeepsLightHitsAtNormalSize() {
        assertEquals(1.0F, IndicatorMath.damageScale(0));
        assertEquals(1.0F, IndicatorMath.damageScale(29));
    }

    @Test
    void damageScaleEnlargesFromThirtyPercent() {
        assertEquals(1.24F, IndicatorMath.damageScale(30), EPSILON);
        assertEquals(1.248F, IndicatorMath.damageScale(31), EPSILON);
        assertEquals(1.48F, IndicatorMath.damageScale(60), EPSILON);
        assertEquals(1.8F, IndicatorMath.damageScale(100), EPSILON);
    }

    @Test
    void shieldDisableIsExactlyTwiceTheSize() {
        assertEquals(2.0F, IndicatorMath.damageScale(IndicatorMath.SHIELD_DISABLE_PERCENT), EPSILON);
    }

    @Test
    void damageScaleNeverShrinksAndNeverDecreases() {
        float previous = 0.0F;
        for (int percent = -10; percent <= 1000; percent++) {
            float scale = IndicatorMath.damageScale(percent);
            assertTrue(scale >= 1.0F, "shrinks at " + percent + "%");
            assertTrue(scale <= IndicatorMath.MAX_DAMAGE_SCALE, "unbounded at " + percent + "%");
            assertTrue(scale >= previous, "decreases at " + percent + "%");
            previous = scale;
        }
    }

    @Test
    void distanceScaleMatchesTheOriginal() {
        assertEquals(1.0F, IndicatorMath.distanceScale(0, 10));
        assertEquals(1.0F, IndicatorMath.distanceScale(10, 10));
        assertEquals(0.5F, IndicatorMath.distanceScale(15, 10), EPSILON);
        assertEquals(0.0F, IndicatorMath.distanceScale(20, 10));
        assertEquals(0.0F, IndicatorMath.distanceScale(500, 10));
        assertEquals(0.9F, IndicatorMath.distanceScale(1, 0), EPSILON);
    }

    @Test
    void angleMatchesTheOriginalConvention() {
        // Facing south (+Z): a source in front is 0, east (+X) is to the left, west to the right.
        assertEquals(0.0F, IndicatorMath.angleTo(0, 0, 5), EPSILON);
        assertEquals(-90.0F, IndicatorMath.angleTo(0, 5, 0), EPSILON);
        assertEquals(90.0F, IndicatorMath.angleTo(0, -5, 0), EPSILON);
        assertEquals(180.0F, Math.abs(IndicatorMath.angleTo(0, 0, -5)), EPSILON);
        // Facing west (yaw 90): a source to the west is straight ahead.
        assertEquals(0.0F, IndicatorMath.angleTo(90, -5, 0), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(doubles = {30_000_001.0, -30_000_001.0, 29_999_983.7, -29_999_983.7})
    void angleIsTheSameFarFromTheOrigin(double far) {
        double[][] offsets = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {1, 1}, {-2, 0.5}, {0.25, -4}};
        for (float yaw : new float[] {0, 37.5F, -120, 180}) {
            for (double[] offset : offsets) {
                float nearOrigin = IndicatorMath.angleTo(yaw, offset[0], offset[1]);
                // The HUD subtracts world coordinates in double precision before calling angleTo.
                double dxX = (far + offset[0]) - far;
                double dzZ = (far + offset[1]) - far;
                assertEquals(nearOrigin, IndicatorMath.angleTo(yaw, dxX, offset[1]), 1.0E-3F);
                assertEquals(nearOrigin, IndicatorMath.angleTo(yaw, offset[0], dzZ), 1.0E-3F);
            }
        }
    }

    @Test
    void floatCoordinatesWouldHaveMisdirectedTheAttack() {
        // The bug being guarded against: at 30,000,001 a float rounds the player's X to 30,000,000,
        // giving a source straight ahead a one-block sideways offset.
        double playerX = 30_000_001.0;
        double sourceX = playerX;
        assertNotEquals(0.0, sourceX - (float)playerX);
        assertEquals(0.0, sourceX - playerX);
    }

    @Test
    void zeroOffsetHasAStableDirection() {
        assertEquals(0.0F, IndicatorMath.angleTo(123, 0, 0));
        assertEquals(0.0F, IndicatorMath.angleTo(-45, 1.0E-9, -1.0E-9));
    }

    @Test
    void edgeIndicatorsStayOnScreenAtEveryAngle() {
        float[][] screens = {{427, 240}, {320, 240}, {960, 540}, {200, 400}};
        float[] sizes = {4, 20, 36, 48};
        for (float[] screen : screens) {
            for (float size : sizes) {
                for (int angle = -180; angle <= 180; angle++) {
                    IndicatorMath.EdgePosition position = IndicatorMath.edgePosition(angle, screen[0], screen[1], size, size, 50);
                    double radians = Math.toRadians(angle);
                    double half = size * (Math.abs(Math.cos(radians)) + Math.abs(Math.sin(radians))) / 2.0;
                    String where = "angle " + angle + ", size " + size + ", screen " + screen[0] + "x" + screen[1];
                    assertTrue(position.centerX() - half >= -1.0E-3, "off the left edge at " + where);
                    assertTrue(position.centerX() + half <= screen[0] + 1.0E-3, "off the right edge at " + where);
                    assertTrue(position.centerY() - half >= -1.0E-3, "off the top at " + where);
                    assertTrue(position.centerY() + half <= screen[1] - 50 + 1.0E-3, "over the hotbar at " + where);
                }
            }
        }
    }

    @Test
    void edgePlacementFollowsTheBorder() {
        float width = 400;
        float height = 300;
        IndicatorMath.EdgePosition ahead = IndicatorMath.edgePosition(0, width, height, 16, 16, 50);
        assertEquals(width / 2, ahead.centerX(), EPSILON);
        assertEquals(8, ahead.centerY(), EPSILON);

        IndicatorMath.EdgePosition left = IndicatorMath.edgePosition(-90, width, height, 16, 16, 50);
        assertEquals(8, left.centerX(), EPSILON);
        IndicatorMath.EdgePosition right = IndicatorMath.edgePosition(90, width, height, 16, 16, 50);
        assertEquals(width - 8, right.centerX(), EPSILON);

        IndicatorMath.EdgePosition behind = IndicatorMath.edgePosition(180, width, height, 16, 16, 50);
        assertEquals(width / 2, behind.centerX(), EPSILON);
        assertEquals(height - 50 - 8, behind.centerY(), EPSILON);
    }
}
