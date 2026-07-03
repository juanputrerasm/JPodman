package com.mtm2.jpodman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPreferencesTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsUseNinetyNinePodLimit() {
        AppPreferences preferences = AppPreferences.defaults();

        assertEquals(99, preferences.podLimit());
        assertEquals(List.of(), preferences.extraPodFolders());
        assertEquals(-1, preferences.folderDepth());
    }

    @Test
    void savesAndLoadsJsonPreferences() throws IOException {
        Path file = tempDir.resolve("preferences.json");
        AppPreferences preferences = new AppPreferences(
                123,
                List.of(tempDir.resolve("pods")),
                2,
                true,
                true,
                "dualList");

        preferences.save(file);
        AppPreferences loaded = AppPreferences.load(file);

        assertEquals(123, loaded.podLimit());
        assertEquals(1, loaded.extraPodFolders().size());
        assertEquals(2, loaded.folderDepth());
        assertTrue(loaded.sortMountedPods());
        assertTrue(loaded.keepWindowOnTop());
    }

    @Test
    void corruptJsonFallsBackToDefaults() throws IOException {
        Path file = tempDir.resolve("preferences.json");
        java.nio.file.Files.writeString(file, "{ not json");

        AppPreferences loaded = AppPreferences.load(file);

        assertEquals(AppPreferences.DEFAULT_POD_LIMIT, loaded.podLimit());
    }

    @Test
    void resolvesOsSpecificPreferencePaths() {
        assertEquals(Path.of("/home/me/Library/Application Support/JPodman/preferences.json"),
                AppPreferences.preferencesPath("Mac OS X", "/home/me", null, null));
        assertEquals(Path.of("C:/Users/me/AppData/Roaming/JPodman/preferences.json"),
                AppPreferences.preferencesPath("Windows 11", "C:/Users/me", null, null));
        assertEquals(Path.of("/tmp/xdg/JPodman/preferences.json"),
                AppPreferences.preferencesPath("Linux", "/home/me", null, "/tmp/xdg"));
    }
}
