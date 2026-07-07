package com.mtm2.jpodman;

import com.mtm2.jpodman.io.MonsterExeDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PodLimitPolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void missingMonsterExeDoesNotChangeDefaultPodLimit() {
        GameInstall install = MonsterExeDetector.detect(tempDir);

        assertFalse(install.canLaunch());
        assertEquals("Generic POD game", install.versionLabel());
        assertFalse(install.warningPodLimit().isPresent());
        assertEquals(AppPreferences.DEFAULT_POD_LIMIT, AppPreferences.defaults().podLimit());
    }

    @Test
    void knownMonsterExeSizeDoesNotChangeConfiguredPodLimit() throws IOException {
        Path monster = tempDir.resolve("monster.exe");
        try (var out = Files.newOutputStream(monster)) {
            out.write(new byte[1]);
            out.write(new byte[1]);
        }

        GameInstall install = MonsterExeDetector.detect(tempDir);
        AppPreferences preferences = AppPreferences.defaults().withPodLimit(99);

        assertEquals("Unknown monster.exe", install.versionLabel());
        assertEquals("Monster Truck Madness", install.gameName());
        assertEquals(true, install.hasMtmExecutable());
        assertEquals(99, preferences.podLimit());
    }

    @Test
    void readsMonsterIniPodLimitAsWarningOnly() throws IOException {
        Files.writeString(tempDir.resolve("monster.exe"), "");
        Path system = tempDir.resolve("system");
        Files.createDirectories(system);
        Files.writeString(system.resolve("monster.ini"), "[Game]\npodLimit=199\n");

        GameInstall install = MonsterExeDetector.detect(tempDir);
        AppPreferences preferences = AppPreferences.defaults().withPodLimit(99);

        assertEquals(199, install.monsterIniPodLimit().orElseThrow());
        assertEquals(199, install.warningPodLimit().orElseThrow());
        assertEquals(99, preferences.podLimit());
    }

    @Test
    void missingMonsterIniPodLimitDoesNotBreakDetection() {
        GameInstall install = MonsterExeDetector.detect(tempDir);

        assertFalse(install.monsterIniPodLimit().isPresent());
    }

    @Test
    void detectsKnownPodGameExecutablesCaseInsensitively() throws IOException {
        Files.writeString(tempDir.resolve("tv.exe"), "");

        GameInstall install = MonsterExeDetector.detect(tempDir);

        assertEquals("Terminal Velocity", install.versionLabel());
        assertEquals("Terminal Velocity", install.gameName());
        assertEquals(15, install.warningPodLimit().orElseThrow());
        assertFalse(install.hasMtmExecutable());
    }

    @Test
    void detectsCprWithThirtyPodLimit() throws IOException {
        Files.writeString(tempDir.resolve("cart.exe"), "");

        GameInstall install = MonsterExeDetector.detect(tempDir);

        assertEquals("CPR", install.versionLabel());
        assertEquals(30, install.warningPodLimit().orElseThrow());
    }

    @Test
    void unknownLimitKnownGamesHaveNoWarningLimit() throws IOException {
        Files.writeString(tempDir.resolve("4x42.exe"), "");

        GameInstall install = MonsterExeDetector.detect(tempDir);

        assertEquals("4x4 Evo 2", install.versionLabel());
        assertFalse(install.warningPodLimit().isPresent());
    }

    @Test
    void detectsAllConfiguredNonMtmExecutables() throws IOException {
        assertDetectedGame("FURY3.EXE", "Fury3", 15);
        assertDetectedGame("HELLBEND.EXE", "Hellbender", 15);
        assertDetectedGame("nocturne.exe", "Nocturne", null);
        assertDetectedGame("4x4.exe", "4x4 Evo", null);
    }

    private void assertDetectedGame(String executableName, String gameName, Integer podLimit) throws IOException {
        Path folder = Files.createDirectory(tempDir.resolve(executableName.replace('.', '_')));
        Files.writeString(folder.resolve(executableName), "");

        GameInstall install = MonsterExeDetector.detect(folder);

        assertEquals(gameName, install.versionLabel());
        assertEquals(gameName, install.gameName());
        assertFalse(install.hasMtmExecutable());
        if (podLimit == null) {
            assertFalse(install.warningPodLimit().isPresent());
        } else {
            assertEquals(podLimit, install.warningPodLimit().orElseThrow());
        }
    }
}
