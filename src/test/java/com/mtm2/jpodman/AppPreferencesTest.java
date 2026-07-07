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
        assertEquals(AppPreferences.DEFAULT_MINIMAL_SYSTEM_POD_FILES, preferences.minimalSystemPodFiles());
        assertEquals(AppPreferences.DEFAULT_SYSTEM_POD_FILES, preferences.systemPodFiles());
        assertTrue(preferences.includeMinimalSystemPodsOnUse());
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
        assertEquals(AppPreferences.DEFAULT_MINIMAL_SYSTEM_POD_FILES, loaded.minimalSystemPodFiles());
        assertEquals(AppPreferences.DEFAULT_SYSTEM_POD_FILES, loaded.systemPodFiles());
        assertTrue(loaded.includeMinimalSystemPodsOnUse());
    }

    @Test
    void corruptJsonFallsBackToDefaults() throws IOException {
        Path file = tempDir.resolve("preferences.json");
        java.nio.file.Files.writeString(file, "{ not json");

        AppPreferences loaded = AppPreferences.load(file);

        assertEquals(AppPreferences.DEFAULT_POD_LIMIT, loaded.podLimit());
    }

    @Test
    void oldJsonWithoutSavedListsLoadsEmptySavedListCollection() {
        AppPreferences loaded = AppPreferences.parse("""
                {
                  "podLimit": 99,
                  "extraPodFolders": [],
                  "folderDepth": -1,
                  "sortMountedPods": false,
                  "keepWindowOnTop": false,
                  "viewMode": "dualList"
                }
                """);

        assertEquals(List.of(), loaded.savedPodLists());
        assertEquals(AppPreferences.DEFAULT_MINIMAL_SYSTEM_POD_FILES, loaded.minimalSystemPodFiles());
        assertEquals(AppPreferences.DEFAULT_SYSTEM_POD_FILES, loaded.systemPodFiles());
    }

    @Test
    void migratesOldSystemPodsIntoMinimalSystemPods() {
        AppPreferences loaded = AppPreferences.parse("""
                {
                  "podLimit": 99,
                  "extraPodFolders": [],
                  "folderDepth": -1,
                  "sortMountedPods": false,
                  "keepWindowOnTop": false,
                  "viewMode": "dualList",
                  "systemPodFiles": ["ui.pod", "truck2.pod"]
                }
                """);

        assertEquals(List.of("ui.pod", "truck2.pod"), loaded.minimalSystemPodFiles());
        assertEquals(AppPreferences.DEFAULT_SYSTEM_POD_FILES, loaded.systemPodFiles());
    }

    @Test
    void savesAndLoadsSavedPodListsInPreferencesJson() throws IOException {
        Path file = tempDir.resolve("preferences.json");
        SavedPodList list = new SavedPodList("one", "Adoob", List.of("a.pod", "b.pod"), List.of("fixmore4.pod"), "created", "updated");
        AppPreferences preferences = AppPreferences.defaults().withSavedPodLists(List.of(list));

        preferences.save(file);
        AppPreferences loaded = AppPreferences.load(file);

        assertEquals(1, loaded.savedPodLists().size());
        assertEquals("Adoob", loaded.savedPodLists().get(0).name());
        assertEquals(List.of("a.pod", "b.pod"), loaded.savedPodLists().get(0).entries());
        assertEquals(List.of(), loaded.savedPodLists().get(0).alwaysMount());
    }

    @Test
    void savesAndLoadsSystemPodFiles() throws IOException {
        Path file = tempDir.resolve("preferences.json");
        AppPreferences preferences = AppPreferences.defaults()
                .withMinimalSystemPodFiles(List.of("startup.pod", "STARTUP.POD"))
                .withSystemPodFiles(List.of("ui.pod", "UI.POD", "Fixes/truck2.pod"))
                .withIncludeMinimalSystemPodsOnUse(false);

        preferences.save(file);
        AppPreferences loaded = AppPreferences.load(file);

        assertEquals(List.of("startup.pod"), loaded.minimalSystemPodFiles());
        assertEquals(List.of("ui.pod", "Fixes/truck2.pod"), loaded.systemPodFiles());
        assertEquals(false, loaded.includeMinimalSystemPodsOnUse());
    }

    @Test
    void allowsEmptySystemPodFiles() {
        AppPreferences preferences = AppPreferences.defaults()
                .withMinimalSystemPodFiles(List.of())
                .withSystemPodFiles(List.of());

        assertEquals(List.of(), preferences.minimalSystemPodFiles());
        assertEquals(List.of(), preferences.systemPodFiles());
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
