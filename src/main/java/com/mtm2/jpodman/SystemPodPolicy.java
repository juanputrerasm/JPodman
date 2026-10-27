package com.mtm2.jpodman;

import com.mtm2.jpodman.io.SavedPodListService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Selects system PODs by detected game. User-configured lists remain the MTM/default
 * profile; games with a known fixed stock set replace it here.
 */
public final class SystemPodPolicy {
    /** The shared stock system set for 4x4 Evolution 1 and 2. */
    public static final List<String> EVO_SYSTEM_POD_FILES = List.of(
            "STARTUP.POD",
            "AI.POD",
            "MUSIC.POD",
            "TRUCK.POD",
            "driver.pod",
            "sound.pod",
            "UI.POD");

    private SystemPodPolicy() {}

    public static SystemPodSet forInstall(GameInstall install, AppPreferences preferences) {
        Objects.requireNonNull(install, "install");
        Objects.requireNonNull(preferences, "preferences");
        if (install.game() == GameKind.EVO1 || install.game() == GameKind.EVO2) {
            return new SystemPodSet(EVO_SYSTEM_POD_FILES, List.of(), true);
        }
        return new SystemPodSet(preferences.minimalSystemPodFiles(), preferences.systemPodFiles(), false);
    }

    /** The system PODs active for one detected game. */
    public record SystemPodSet(List<String> minimalFiles, List<String> additionalFiles, boolean gameDefined) {
        public SystemPodSet {
            minimalFiles = List.copyOf(minimalFiles);
            additionalFiles = List.copyOf(additionalFiles);
        }

        /** Minimal and additional files, de-duplicated in mount order. */
        public List<String> allFiles() {
            List<String> files = new ArrayList<>(minimalFiles);
            files.addAll(additionalFiles);
            return SavedPodListService.dedupe(files);
        }
    }
}
