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
        assertEquals(99, preferences.podLimit());
    }
}
