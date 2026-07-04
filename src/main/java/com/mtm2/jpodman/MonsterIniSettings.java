package com.mtm2.jpodman;

import java.util.List;

/** Editable Monster.ini settings mirrored from Cowpod's Fonts & Things dialog. */
public record MonsterIniSettings(
        boolean useGlobalFonts,
        List<FontSetting> fonts,
        String localizationFile,
        boolean extraHorn,
        boolean numericLatency,
        boolean hiddenTrack) {
    public static final List<String> STOCK_LOCALIZATION_FILES = List.of(
            "UI\\MTM2.loc",
            "UI\\MTM2-FUN.loc",
            "UI\\MTM2-PIG.loc");

    public MonsterIniSettings {
        fonts = fonts == null || fonts.size() != 4 ? defaultFonts() : List.copyOf(fonts);
        localizationFile = localizationFile == null || localizationFile.isBlank() ? STOCK_LOCALIZATION_FILES.get(0) : localizationFile.trim();
    }

    public static MonsterIniSettings defaults() {
        return new MonsterIniSettings(true, defaultFonts(), STOCK_LOCALIZATION_FILES.get(0), true, false, false);
    }

    public MonsterIniSettings withFonts(List<FontSetting> newFonts) {
        return new MonsterIniSettings(useGlobalFonts, newFonts, localizationFile, extraHorn, numericLatency, hiddenTrack);
    }

    public MonsterIniSettings withUseGlobalFonts(boolean value) {
        return new MonsterIniSettings(value, fonts, localizationFile, extraHorn, numericLatency, hiddenTrack);
    }

    public static List<FontSetting> defaultFonts() {
        return List.of(
                new FontSetting("SmallLCD", "Lap times / HUD timer", true, "Arial,Arial,Arial", "-9,-9,-10"),
                new FontSetting("Small", "Speedometer", true, "Arial,Arial,Arial", "-9,-9,-10"),
                new FontSetting("Medium", "Loading screens", true, "Arial,Arial,Arial", "-10,-10,-12"),
                new FontSetting("Large", "Mobile messages", true, "Arial,Arial,Arial", "-10,-10,-14"));
    }

    public record FontSetting(String suffix, String label, boolean enabled, String fontName, String fontSize) {
        public FontSetting {
            fontName = fontName == null || fontName.isBlank() ? "Arial,Arial,Arial" : fontName.trim();
            fontSize = fontSize == null || fontSize.isBlank() ? "-9,-9,-10" : fontSize.trim();
        }
    }
}
