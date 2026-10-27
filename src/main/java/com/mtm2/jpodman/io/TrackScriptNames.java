package com.mtm2.jpodman.io;

import java.util.Locale;

/**
 * Naming rules for track scripts. Community Patch 3 writes a track that needs its
 * engine as {@code <stem>.SI2} instead of {@code .SIT}; a stock install only lists
 * {@code .SIT}, so it never sees one. When both spellings of a stem are mounted,
 * CP3 plays the {@code .SI2}, so the two are the same track.
 */
public final class TrackScriptNames {
    public static final String SIT_EXTENSION = ".SIT";
    public static final String SI2_EXTENSION = ".SI2";

    private TrackScriptNames() {}

    public static boolean isTrackScript(String name) {
        return isSit(name) || isSi2(name);
    }

    public static boolean isSit(String name) {
        return name != null && name.toUpperCase(Locale.ROOT).endsWith(SIT_EXTENSION);
    }

    public static boolean isSi2(String name) {
        return name != null && name.toUpperCase(Locale.ROOT).endsWith(SI2_EXTENSION);
    }

    /**
     * The upper-case file name used to compare resources. {@code DEMO.SIT} and
     * {@code DEMO.SI2} share the key {@code DEMO.SIT}; any other name is its own key.
     */
    public static String resourceKey(String title) {
        String key = title.trim().toUpperCase(Locale.ROOT);
        return key.endsWith(SI2_EXTENSION)
                ? key.substring(0, key.length() - SI2_EXTENSION.length()) + SIT_EXTENSION
                : key;
    }
}
