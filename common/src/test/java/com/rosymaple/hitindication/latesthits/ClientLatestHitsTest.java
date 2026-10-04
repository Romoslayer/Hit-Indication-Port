package com.rosymaple.hitindication.latesthits;

import com.rosymaple.hitindication.config.HitIndicatorClientConfigs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientLatestHitsTest {
    private TestConfig config;

    @BeforeEach
    void setUp() {
        config = new TestConfig();
        HitIndicatorClientConfigs.set(config);
        ClientLatestHits.clear();
    }

    @AfterEach
    void tearDown() {
        ClientLatestHits.clear();
    }

    private static void addHits(int count) {
        for (int i = 0; i < count; i++)
            ClientLatestHits.addHitIndicator(i, 64, 0, HitIndicatorType.HIT.id(), 10, false);
    }

    @Test
    void loweringTheCapTrimsToTheNewestOnTheNextTick() {
        config.maxIndicatorCount = 0;
        addHits(20);
        assertEquals(20, ClientLatestHits.latestHitIndicators.size());

        config.maxIndicatorCount = 3;
        ClientLatestHits.tick();
        assertEquals(3, ClientLatestHits.latestHitIndicators.size());
        assertEquals(17, ClientLatestHits.latestHitIndicators.get(0).getX());
        assertEquals(19, ClientLatestHits.latestHitIndicators.get(2).getX());
    }

    @Test
    void loweringTheCapTrimsOnTheNextHitToo() {
        addHits(20);
        config.maxIndicatorCount = 3;
        ClientLatestHits.addHitIndicator(100, 64, 0, HitIndicatorType.HIT.id(), 10, false);
        assertEquals(3, ClientLatestHits.latestHitIndicators.size());
        assertEquals(100, ClientLatestHits.latestHitIndicators.get(2).getX());

        addHits(5);
        assertEquals(3, ClientLatestHits.latestHitIndicators.size());
    }

    @Test
    void raisingTheCapAndUnlimitedKeepEverything() {
        config.maxIndicatorCount = 3;
        addHits(10);
        assertEquals(3, ClientLatestHits.latestHitIndicators.size());

        config.maxIndicatorCount = 10;
        addHits(5);
        assertEquals(8, ClientLatestHits.latestHitIndicators.size());

        config.maxIndicatorCount = 0;
        addHits(50);
        ClientLatestHits.tick();
        assertEquals(58, ClientLatestHits.latestHitIndicators.size());
    }

    @Test
    void indicatorsExpireAfterTheFadeRate() {
        config.fadeRate = 3;
        addHits(4);
        ClientLatestHits.tick();
        ClientLatestHits.tick();
        assertEquals(4, ClientLatestHits.latestHitIndicators.size());
        ClientLatestHits.tick();
        assertTrue(ClientLatestHits.latestHitIndicators.isEmpty());
    }

    @Test
    void zeroFadeRateExpiresOnTheFirstTick() {
        config.fadeRate = 0;
        addHits(2);
        ClientLatestHits.tick();
        assertTrue(ClientLatestHits.latestHitIndicators.isEmpty());
    }

    @Test
    void malformedPayloadsAreDropped() {
        ClientLatestHits.addHitIndicator(Double.NaN, 0, 0, HitIndicatorType.HIT.id(), 10, false);
        ClientLatestHits.addHitIndicator(0, Double.POSITIVE_INFINITY, 0, HitIndicatorType.HIT.id(), 10, false);
        ClientLatestHits.addHitIndicator(0, 0, 0, 7, 10, false);
        ClientLatestHits.addHitIndicator(0, 0, 0, -1, 10, false);
        assertTrue(ClientLatestHits.latestHitIndicators.isEmpty());

        ClientLatestHits.setHitMarker(5);
        assertNull(ClientLatestHits.currentHitMarker);
    }

    @Test
    void largeCoordinatesAndPercentagesAreKeptWithinBounds() {
        ClientLatestHits.addHitIndicator(29_999_999.5, -64, -29_999_999.5, HitIndicatorType.HIT.id(), Integer.MAX_VALUE, false);
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.HIT.id(), -40, false);
        assertEquals(29_999_999.5, ClientLatestHits.latestHitIndicators.get(0).getX());
        assertEquals(ClientLatestHits.MAX_DAMAGE_PERCENT, ClientLatestHits.latestHitIndicators.get(0).getDamagePercent());
        assertEquals(0, ClientLatestHits.latestHitIndicators.get(1).getDamagePercent());
    }

    @Test
    void killMarkerIsNotReplacedByACrit() {
        ClientLatestHits.setHitMarker(HitMarkerType.KILL.id());
        ClientLatestHits.setHitMarker(HitMarkerType.CRIT.id());
        assertEquals(HitMarkerType.KILL, ClientLatestHits.currentHitMarker.getType());

        // A new kill restarts the kill animation; once it has played out, crits show again.
        ClientLatestHits.tick();
        ClientLatestHits.setHitMarker(HitMarkerType.KILL.id());
        assertEquals(9, ClientLatestHits.currentHitMarker.getLifeTime());
        for (int i = 0; i < 9; i++)
            ClientLatestHits.tick();
        assertNull(ClientLatestHits.currentHitMarker);
        ClientLatestHits.setHitMarker(HitMarkerType.CRIT.id());
        assertEquals(HitMarkerType.CRIT, ClientLatestHits.currentHitMarker.getType());

        ClientLatestHits.setHitMarker(HitMarkerType.KILL.id());
        assertEquals(HitMarkerType.KILL, ClientLatestHits.currentHitMarker.getType());
    }

    @Test
    void disabledCategoriesAreNotAdded() {
        config.showBlueIndicators = false;
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.BLOCK.id(), 0, false);
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.ND_HIT.id(), 0, false);
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.HIT.id(), 0, true);
        assertTrue(ClientLatestHits.latestHitIndicators.isEmpty());

        config.displayHitsFromNegativePotions = true;
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.HIT.id(), 0, true);
        assertEquals(1, ClientLatestHits.latestHitIndicators.size());

        config.enableHitIndication = false;
        ClientLatestHits.addHitIndicator(0, 0, 0, HitIndicatorType.HIT.id(), 0, false);
        assertEquals(1, ClientLatestHits.latestHitIndicators.size());

        config.enableHitMarkers = false;
        ClientLatestHits.setHitMarker(HitMarkerType.KILL.id());
        assertNull(ClientLatestHits.currentHitMarker);
    }

    /** Mutable config with the original defaults, except crit/kill markers on. */
    static final class TestConfig implements HitIndicatorClientConfigs.Values {
        boolean enableHitIndication = true;
        int maxIndicatorCount = 0;
        boolean displayHitsFromNegativePotions = false;
        int fadeRate = 50;
        boolean showBlueIndicators = true;
        boolean enableHitMarkers = true;
        boolean enableNonDirectionalDamage = false;
        boolean edgeOfScreenMode = false;

        @Override public boolean enableHitIndication() { return enableHitIndication; }
        @Override public void setEnableHitIndication(boolean value) { enableHitIndication = value; }
        @Override public int maxIndicatorCount() { return maxIndicatorCount; }
        @Override public boolean displayHitsFromNegativePotions() { return displayHitsFromNegativePotions; }
        @Override public int fadeRate() { return fadeRate; }
        @Override public int indicatorOpacity() { return 25; }
        @Override public boolean showBlueIndicators() { return showBlueIndicators; }
        @Override public void setShowBlueIndicators(boolean value) { showBlueIndicators = value; }
        @Override public boolean sizeDependsOnDamage() { return false; }
        @Override public int indicatorDefaultScale() { return 25; }
        @Override public int distanceFromCrosshair() { return 30; }
        @Override public boolean enableHitMarkers() { return enableHitMarkers; }
        @Override public boolean enableNonDirectionalDamage() { return enableNonDirectionalDamage; }
        @Override public boolean enableDistanceScaling() { return true; }
        @Override public int distanceScalingCutoff() { return 10; }
        @Override public boolean edgeOfScreenMode() { return edgeOfScreenMode; }
        @Override public void setEdgeOfScreenMode(boolean value) { edgeOfScreenMode = value; }
        @Override public String hitIndicatorColor() { return "FF0000"; }
        @Override public String blockIndicatorColor() { return "0000FF"; }
    }
}
