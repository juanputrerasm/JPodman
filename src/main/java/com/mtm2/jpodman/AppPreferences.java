package com.mtm2.jpodman;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persisted application preferences stored as JSON in the user's config area. */
public record AppPreferences(
        int podLimit,
        List<Path> extraPodFolders,
        int folderDepth,
        boolean sortMountedPods,
        boolean keepWindowOnTop,
        String viewMode) {
    public static final int DEFAULT_POD_LIMIT = 99;
    public static final int MIN_POD_LIMIT = 1;
    public static final int MAX_POD_LIMIT = 999;

    private static final Pattern STRING_ARRAY_VALUE = Pattern.compile("\"((?:\\\\.|[^\"])*)\"");

    public AppPreferences {
        podLimit = clampPodLimit(podLimit);
        extraPodFolders = sanitizePaths(extraPodFolders);
        viewMode = viewMode == null || viewMode.isBlank() ? "dualList" : viewMode;
    }

    public static AppPreferences defaults() {
        return new AppPreferences(DEFAULT_POD_LIMIT, List.of(), -1, false, false, "dualList");
    }

    public static Path preferencesPath() {
        return preferencesPath(
                System.getProperty("os.name", ""),
                System.getProperty("user.home", "."),
                System.getenv("APPDATA"),
                System.getenv("XDG_CONFIG_HOME"));
    }

    public static Path preferencesPath(String osName, String home, String appData, String xdgConfigHome) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String safeHome = home == null || home.isBlank() ? "." : home;
        if (os.contains("win")) {
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "JPodman", "preferences.json");
            }
            return Path.of(safeHome, "AppData", "Roaming", "JPodman", "preferences.json");
        }
        if (os.contains("mac")) {
            return Path.of(safeHome, "Library", "Application Support", "JPodman", "preferences.json");
        }
        if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
            return Path.of(xdgConfigHome, "JPodman", "preferences.json");
        }
        return Path.of(safeHome, ".config", "JPodman", "preferences.json");
    }

    public static AppPreferences load() {
        return load(preferencesPath());
    }

    public static AppPreferences load(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return defaults();
        }
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException ex) {
            return defaults();
        }
    }

    public void save() throws IOException {
        save(preferencesPath());
    }

    public void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(), StandardCharsets.UTF_8);
    }

    public AppPreferences withPodLimit(int newLimit) {
        return new AppPreferences(newLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode);
    }

    public AppPreferences withExtraPodFolders(List<Path> folders) {
        return new AppPreferences(podLimit, folders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode);
    }

    public AppPreferences withFolderDepth(int newDepth) {
        return new AppPreferences(podLimit, extraPodFolders, newDepth, sortMountedPods, keepWindowOnTop, viewMode);
    }

    public AppPreferences withSortMountedPods(boolean sort) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sort, keepWindowOnTop, viewMode);
    }

    public AppPreferences withKeepWindowOnTop(boolean keepOnTop) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepOnTop, viewMode);
    }

    public static AppPreferences parse(String json) {
        if (json == null) {
            return defaults();
        }
        AppPreferences defaults = defaults();
        return new AppPreferences(
                parseInt(json, "podLimit", defaults.podLimit),
                parsePathArray(json, "extraPodFolders"),
                parseInt(json, "folderDepth", defaults.folderDepth),
                parseBoolean(json, "sortMountedPods", defaults.sortMountedPods),
                parseBoolean(json, "keepWindowOnTop", defaults.keepWindowOnTop),
                parseString(json, "viewMode", defaults.viewMode));
    }

    public String toJson() {
        return "{\n"
                + "  \"podLimit\": " + podLimit + ",\n"
                + "  \"extraPodFolders\": " + jsonPathArray(extraPodFolders) + ",\n"
                + "  \"folderDepth\": " + folderDepth + ",\n"
                + "  \"sortMountedPods\": " + sortMountedPods + ",\n"
                + "  \"keepWindowOnTop\": " + keepWindowOnTop + ",\n"
                + "  \"viewMode\": " + jsonString(viewMode) + "\n"
                + "}\n";
    }

    private static int clampPodLimit(int value) {
        return Math.max(MIN_POD_LIMIT, Math.min(MAX_POD_LIMIT, value));
    }

    private static List<Path> sanitizePaths(List<Path> folders) {
        if (folders == null || folders.isEmpty()) {
            return List.of();
        }
        List<Path> sanitized = new ArrayList<>();
        for (Path folder : folders) {
            if (folder != null) {
                Path normalized = folder.toAbsolutePath().normalize();
                if (!sanitized.contains(normalized)) {
                    sanitized.add(normalized);
                }
            }
        }
        return List.copyOf(sanitized);
    }

    private static int parseInt(String json, String key, int defaultValue) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : defaultValue;
    }

    private static boolean parseBoolean(String json, String key, boolean defaultValue) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : defaultValue;
    }

    private static String parseString(String json, String key, String defaultValue) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        return matcher.find() ? unescapeJson(matcher.group(1)) : defaultValue;
    }

    private static List<Path> parsePathArray(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', keyIndex);
        if (arrayStart < 0) {
            return List.of();
        }
        int depth = 0;
        for (int i = arrayStart; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    String body = json.substring(arrayStart + 1, i);
                    Matcher matcher = STRING_ARRAY_VALUE.matcher(body);
                    List<Path> values = new ArrayList<>();
                    while (matcher.find()) {
                        values.add(Path.of(unescapeJson(matcher.group(1))));
                    }
                    return values;
                }
            }
        }
        return List.of();
    }

    private static String jsonPathArray(List<Path> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jsonString(values.get(i).toString()));
        }
        return sb.append(']').toString();
    }

    private static String jsonString(String value) {
        return "\"" + escapeJson(value == null ? "" : value) + "\"";
    }

    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String unescapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                switch (ch) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\', '"' -> sb.append(ch);
                    default -> sb.append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                sb.append(ch);
            }
        }
        if (escaping) {
            sb.append('\\');
        }
        return sb.toString();
    }
}
