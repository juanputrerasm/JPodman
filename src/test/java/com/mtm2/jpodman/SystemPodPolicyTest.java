package com.mtm2.jpodman;

import com.mtm2.jpodman.io.MonsterExeDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPodPolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void evoGamesUseTheirOwnSystemPodSet() throws IOException {
        for (String executable : List.of("4x4.exe", "4x42.exe")) {
            Path folder = Files.createDirectory(tempDir.resolve(executable.replace('.', '_')));
            Files.writeString(folder.resolve(executable), "");
            AppPreferences preferences = AppPreferences.defaults()
                    .withMinimalSystemPodFiles(List.of("custom-minimal.pod"))
                    .withSystemPodFiles(List.of("custom-extra.pod"));

            SystemPodPolicy.SystemPodSet set = SystemPodPolicy.forInstall(MonsterExeDetector.detect(folder), preferences);

            assertTrue(set.gameDefined());
            assertEquals(SystemPodPolicy.EVO_SYSTEM_POD_FILES, set.minimalFiles());
            assertEquals(List.of(), set.additionalFiles());
            assertEquals(SystemPodPolicy.EVO_SYSTEM_POD_FILES, set.allFiles());
        }
    }

    @Test
    void otherGamesKeepTheConfiguredSystemPodSet() throws IOException {
        Files.writeString(tempDir.resolve("monster.exe"), "");
        AppPreferences preferences = AppPreferences.defaults()
                .withMinimalSystemPodFiles(List.of("ui.pod"))
                .withSystemPodFiles(List.of("extra.pod"));

        SystemPodPolicy.SystemPodSet set = SystemPodPolicy.forInstall(MonsterExeDetector.detect(tempDir), preferences);

        assertFalse(set.gameDefined());
        assertEquals(preferences.minimalSystemPodFiles(), set.minimalFiles());
        assertEquals(preferences.systemPodFiles(), set.additionalFiles());
    }
}
