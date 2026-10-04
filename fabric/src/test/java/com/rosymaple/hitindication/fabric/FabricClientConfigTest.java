package com.rosymaple.hitindication.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FabricClientConfigTest {
    @TempDir
    Path dir;

    private FabricClientConfig load(Path path) {
        FabricClientConfig config = new FabricClientConfig(path);
        config.read();
        return config;
    }

    private List<Path> backups() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().contains(".broken-")).toList();
        }
    }

    @Test
    void missingFileIsCreatedWithDefaults() {
        Path path = dir.resolve("hitindication-client.json");
        FabricClientConfig config = load(path);
        assertTrue(Files.exists(path));
        assertTrue(config.enableHitIndication());
        assertEquals(50, config.fadeRate());
    }

    @Test
    void missingKeysGetDefaultsAndValuesAreClamped() throws IOException {
        Path path = dir.resolve("hitindication-client.json");
        Files.writeString(path, "{\"enableHitIndication\": false, \"indicatorOpacity\": 900, \"distanceFromCrosshair\": 1}", StandardCharsets.UTF_8);
        FabricClientConfig config = load(path);
        assertFalse(config.enableHitIndication());
        assertEquals(100, config.indicatorOpacity());
        assertEquals(30, config.distanceFromCrosshair());
        assertEquals(50, config.fadeRate());
        assertEquals("FF0000", config.hitIndicatorColor());
        // Rewritten with every option.
        assertTrue(Files.readString(path).contains("blockIndicatorColor"));
    }

    @Test
    void malformedFileIsKeptForRecovery() throws IOException {
        Path path = dir.resolve("hitindication-client.json");
        String broken = "{\"enableHitIndication\": false, \"maxIndicatorCount\": 3,, oops";
        Files.writeString(path, broken, StandardCharsets.UTF_8);

        FabricClientConfig config = load(path);
        assertTrue(config.enableHitIndication(), "falls back to defaults");
        List<Path> backups = backups();
        assertEquals(1, backups.size());
        assertEquals(broken, Files.readString(backups.get(0)));
        assertTrue(Files.readString(path).contains("\"enableHitIndication\": true"));
    }

    @Test
    void truncatedAndWrongTypeFilesAreKeptToo() throws IOException {
        Path truncated = dir.resolve("truncated.json");
        Files.writeString(truncated, "{\"enableHitIndication\": fal", StandardCharsets.UTF_8);
        load(truncated);

        Path wrongType = dir.resolve("wrong-type.json");
        Files.writeString(wrongType, "{\"maxIndicatorCount\": \"lots\"}", StandardCharsets.UTF_8);
        load(wrongType);

        assertEquals(2, backups().size());
    }

    @Test
    void savesPersistAcrossReloads() {
        Path path = dir.resolve("hitindication-client.json");
        FabricClientConfig config = load(path);
        config.setEnableHitIndication(false);
        config.setShowBlueIndicators(false);
        config.setEdgeOfScreenMode(true);
        config.data().indicatorOpacity = 80;
        config.save();

        FabricClientConfig reloaded = load(path);
        assertFalse(reloaded.enableHitIndication());
        assertFalse(reloaded.showBlueIndicators());
        assertTrue(reloaded.edgeOfScreenMode());
        assertEquals(80, reloaded.indicatorOpacity());
        assertFalse(Files.exists(dir.resolve("hitindication-client.json.tmp")));
    }

    @Test
    void failedSaveLeavesTheOldFileUsable() throws IOException {
        Path path = dir.resolve("hitindication-client.json");
        FabricClientConfig config = load(path);
        config.setEnableHitIndication(false);
        String before = Files.readString(path);

        // A directory where the temporary file would go makes the write fail.
        Files.createDirectory(dir.resolve("hitindication-client.json.tmp"));
        config.setEnableHitIndication(true);

        assertEquals(before, Files.readString(path));
        assertFalse(load(path).enableHitIndication());
    }
}
