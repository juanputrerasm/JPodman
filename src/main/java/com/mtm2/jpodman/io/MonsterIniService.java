package com.mtm2.jpodman.io;

import com.mtm2.jpodman.MonsterIniSettings;
import com.mtm2.jpodman.MonsterIniSettings.FontSetting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Line-preserving reader/writer for the Monster.ini keys JPodman edits. */
public final class MonsterIniService {
    private MonsterIniService() {}

    public static MonsterIniSettings read(Path monsterIni) throws IOException {
        if (monsterIni == null || !Files.isRegularFile(monsterIni)) {
            return MonsterIniSettings.defaults();
        }
        Map<String, String> fonts = readSection(Files.readAllLines(monsterIni, StandardCharsets.UTF_8), "Fonts");
        Map<String, String> game = readSection(Files.readAllLines(monsterIni, StandardCharsets.UTF_8), "Game");
        List<FontSetting> settings = new ArrayList<>();
        for (FontSetting defaultFont : MonsterIniSettings.defaultFonts()) {
            String suffix = defaultFont.suffix();
            settings.add(new FontSetting(
                    suffix,
                    defaultFont.label(),
                    "1".equals(fonts.getOrDefault("gOSFontUse_" + suffix, defaultFont.enabled() ? "1" : "0")),
                    fonts.getOrDefault("gOSFontName_" + suffix, defaultFont.fontName()),
                    fonts.getOrDefault("gOSFontSize_" + suffix, defaultFont.fontSize())));
        }
        return new MonsterIniSettings(
                "1".equals(fonts.getOrDefault("gOSFontUseGlobal", "1")),
                settings,
                fonts.getOrDefault("gLocalizationFile", MonsterIniSettings.STOCK_LOCALIZATION_FILES.get(0)),
                "1".equals(game.getOrDefault("kookyHornFlag", "1")),
                "1".equals(game.getOrDefault("latencyDisplay", "2")),
                "666".equals(game.getOrDefault("showHiddenTrack", "0")));
    }

    public static void write(Path monsterIni, MonsterIniSettings settings) throws IOException {
        List<String> lines = Files.isRegularFile(monsterIni)
                ? Files.readAllLines(monsterIni, StandardCharsets.UTF_8)
                : new ArrayList<>();
        lines = updateSection(lines, "Fonts", fontValues(settings));
        lines = updateSection(lines, "Game", gameValues(settings));
        Path parent = monsterIni.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(monsterIni, lines, StandardCharsets.UTF_8);
    }

    private static Map<String, String> fontValues(MonsterIniSettings settings) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("gOSFontUseGlobal", settings.useGlobalFonts() ? "1" : "0");
        for (FontSetting font : settings.fonts()) {
            values.put("gOSFontUse_" + font.suffix(), font.enabled() ? "1" : "0");
        }
        for (FontSetting font : settings.fonts()) {
            values.put("gOSFontName_" + font.suffix(), font.fontName());
        }
        for (FontSetting font : settings.fonts()) {
            values.put("gOSFontSize_" + font.suffix(), font.fontSize());
        }
        values.put("gLocalizationFile", settings.localizationFile());
        return values;
    }

    private static Map<String, String> gameValues(MonsterIniSettings settings) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("kookyHornFlag", settings.extraHorn() ? "1" : "0");
        values.put("latencyDisplay", settings.numericLatency() ? "1" : "2");
        values.put("showHiddenTrack", settings.hiddenTrack() ? "666" : "0");
        return values;
    }

    private static Map<String, String> readSection(List<String> lines, String section) {
        Map<String, String> values = new LinkedHashMap<>();
        boolean inSection = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                inSection = trimmed.substring(1, trimmed.length() - 1).equalsIgnoreCase(section);
                continue;
            }
            if (!inSection || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals > 0) {
                values.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        }
        return values;
    }

    private static List<String> updateSection(List<String> original, String section, Map<String, String> values) {
        List<String> lines = new ArrayList<>(original);
        int start = findSectionStart(lines, section);
        if (start < 0) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.add("[" + section + "]");
            for (Map.Entry<String, String> entry : values.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue());
            }
            return lines;
        }

        int end = findSectionEnd(lines, start + 1);
        Map<String, String> remaining = new LinkedHashMap<>(values);
        for (int i = start + 1; i < end; i++) {
            String line = lines.get(i);
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String matched = findKey(remaining, key);
            if (matched != null) {
                lines.set(i, key + "=" + remaining.remove(matched));
            }
        }
        int insert = end;
        for (Map.Entry<String, String> entry : remaining.entrySet()) {
            lines.add(insert++, entry.getKey() + "=" + entry.getValue());
        }
        return lines;
    }

    private static int findSectionStart(List<String> lines, String section) {
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")
                    && trimmed.substring(1, trimmed.length() - 1).equalsIgnoreCase(section)) {
                return i;
            }
        }
        return -1;
    }

    private static int findSectionEnd(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return i;
            }
        }
        return lines.size();
    }

    private static String findKey(Map<String, String> values, String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (String candidate : values.keySet()) {
            if (candidate.toLowerCase(Locale.ROOT).equals(lower)) {
                return candidate;
            }
        }
        return null;
    }
}
