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
        String viewMode,
        List<String> minimalSystemPodFiles,
        List<String> systemPodFiles,
        boolean includeMinimalSystemPodsOnUse,
        List<SavedPodList> savedPodLists) {
    public static final int DEFAULT_POD_LIMIT = 99;
    public static final int MIN_POD_LIMIT = 1;
    public static final int MAX_POD_LIMIT = 999;
    public static final List<String> DEFAULT_MINIMAL_SYSTEM_POD_FILES = List.of(
            "fixmore4.pod",
            "startup.pod",
            "music.pod",
            "sound.pod",
            "truck2.pod",
            "cockpit.pod",
            "ui.pod");
    public static final List<String> DEFAULT_SYSTEM_POD_FILES = List.of(
            "tpark.pod",
            "alaska.pod",
            "junk.pod",
            "crazy98.pod",
            "snake.pod",
            "main.pod",
            "aztec.pod",
            "outback.pod",
            "rockqry.pod",
            "baja.pod",
            "summit1.pod",
            "summit2.pod",
            "summit3.pod");
    public static final boolean DEFAULT_INCLUDE_MINIMAL_SYSTEM_PODS_ON_USE = true;

    private static final Pattern STRING_ARRAY_VALUE = Pattern.compile("\"((?:\\\\.|[^\"])*)\"");

    public AppPreferences {
        podLimit = clampPodLimit(podLimit);
        extraPodFolders = sanitizePaths(extraPodFolders);
        viewMode = viewMode == null || viewMode.isBlank() ? "dualList" : viewMode;
        minimalSystemPodFiles = sanitizeSystemPodFiles(minimalSystemPodFiles, DEFAULT_MINIMAL_SYSTEM_POD_FILES);
        systemPodFiles = sanitizeSystemPodFiles(systemPodFiles, DEFAULT_SYSTEM_POD_FILES);
        savedPodLists = savedPodLists == null ? List.of() : List.copyOf(savedPodLists);
    }

    public AppPreferences(int podLimit, List<Path> extraPodFolders, int folderDepth, boolean sortMountedPods, boolean keepWindowOnTop, String viewMode) {
        this(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                DEFAULT_MINIMAL_SYSTEM_POD_FILES, DEFAULT_SYSTEM_POD_FILES, DEFAULT_INCLUDE_MINIMAL_SYSTEM_PODS_ON_USE, List.of());
    }

    public AppPreferences(
            int podLimit,
            List<Path> extraPodFolders,
            int folderDepth,
            boolean sortMountedPods,
            boolean keepWindowOnTop,
            String viewMode,
            List<String> systemPodFiles,
            List<SavedPodList> savedPodLists) {
        this(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                systemPodFiles, DEFAULT_SYSTEM_POD_FILES, DEFAULT_INCLUDE_MINIMAL_SYSTEM_PODS_ON_USE, savedPodLists);
    }

    public static AppPreferences defaults() {
        return new AppPreferences(DEFAULT_POD_LIMIT, List.of(), -1, false, false, "dualList",
                DEFAULT_MINIMAL_SYSTEM_POD_FILES, DEFAULT_SYSTEM_POD_FILES, DEFAULT_INCLUDE_MINIMAL_SYSTEM_PODS_ON_USE, List.of());
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
        return new AppPreferences(newLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withExtraPodFolders(List<Path> folders) {
        return new AppPreferences(podLimit, folders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withFolderDepth(int newDepth) {
        return new AppPreferences(podLimit, extraPodFolders, newDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withSortMountedPods(boolean sort) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sort, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withKeepWindowOnTop(boolean keepOnTop) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withMinimalSystemPodFiles(List<String> files) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                files, systemPodFiles, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withSystemPodFiles(List<String> files) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, files, includeMinimalSystemPodsOnUse, savedPodLists);
    }

    public AppPreferences withIncludeMinimalSystemPodsOnUse(boolean include) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, include, savedPodLists);
    }

    public AppPreferences withSavedPodLists(List<SavedPodList> lists) {
        return new AppPreferences(podLimit, extraPodFolders, folderDepth, sortMountedPods, keepWindowOnTop, viewMode,
                minimalSystemPodFiles, systemPodFiles, includeMinimalSystemPodsOnUse, lists);
    }

    public static AppPreferences parse(String json) {
        if (json == null) {
            return defaults();
        }
        AppPreferences defaults = defaults();
        boolean hasMinimalPods = parseArrayBody(json, "minimalSystemPodFiles") != null;
        List<String> oldSystemPods = parseStringArray(json, "systemPodFiles", null);
        List<String> minimalPods = hasMinimalPods
                ? parseStringArray(json, "minimalSystemPodFiles", DEFAULT_MINIMAL_SYSTEM_POD_FILES)
                : oldSystemPods == null ? DEFAULT_MINIMAL_SYSTEM_POD_FILES : oldSystemPods;
        List<String> systemPods = hasMinimalPods
                ? parseStringArray(json, "systemPodFiles", DEFAULT_SYSTEM_POD_FILES)
                : DEFAULT_SYSTEM_POD_FILES;
        return new AppPreferences(
                parseInt(json, "podLimit", defaults.podLimit),
                parsePathArray(json, "extraPodFolders"),
                parseInt(json, "folderDepth", defaults.folderDepth),
                parseBoolean(json, "sortMountedPods", defaults.sortMountedPods),
                parseBoolean(json, "keepWindowOnTop", defaults.keepWindowOnTop),
                parseString(json, "viewMode", defaults.viewMode),
                minimalPods,
                systemPods,
                parseBoolean(json, "includeMinimalSystemPodsOnUse", DEFAULT_INCLUDE_MINIMAL_SYSTEM_PODS_ON_USE),
                parseSavedPodLists(json));
    }

    public String toJson() {
        return "{\n"
                + "  \"podLimit\": " + podLimit + ",\n"
                + "  \"extraPodFolders\": " + jsonPathArray(extraPodFolders) + ",\n"
                + "  \"folderDepth\": " + folderDepth + ",\n"
                + "  \"sortMountedPods\": " + sortMountedPods + ",\n"
                + "  \"keepWindowOnTop\": " + keepWindowOnTop + ",\n"
                + "  \"viewMode\": " + jsonString(viewMode) + ",\n"
                + "  \"minimalSystemPodFiles\": " + jsonStringArray(minimalSystemPodFiles) + ",\n"
                + "  \"systemPodFiles\": " + jsonStringArray(systemPodFiles) + ",\n"
                + "  \"includeMinimalSystemPodsOnUse\": " + includeMinimalSystemPodsOnUse + ",\n"
                + "  \"savedPodLists\": " + jsonSavedPodListArray(savedPodLists) + "\n"
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

    private static List<String> sanitizeSystemPodFiles(List<String> files, List<String> defaultValue) {
        if (files == null) {
            return defaultValue;
        }
        if (files.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String file : files) {
            if (file == null || file.isBlank()) {
                continue;
            }
            String value = file.trim().replace('\\', '/');
            String key = value.toLowerCase(Locale.ROOT);
            if (!seen.contains(key)) {
                seen.add(key);
                sanitized.add(value);
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
        String body = parseArrayBody(json, key);
        if (body == null) {
            return List.of();
        }
        Matcher matcher = STRING_ARRAY_VALUE.matcher(body);
        List<Path> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(Path.of(unescapeJson(matcher.group(1))));
        }
        return values;
    }

    private static List<String> parseStringArray(String json, String key, List<String> defaultValue) {
        String body = parseArrayBody(json, key);
        if (body == null) {
            return defaultValue;
        }
        Matcher matcher = STRING_ARRAY_VALUE.matcher(body);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(unescapeJson(matcher.group(1)));
        }
        return List.copyOf(values);
    }

    private static List<SavedPodList> parseSavedPodLists(String json) {
        String body = parseArrayBody(json, "savedPodLists");
        if (body == null || body.isBlank()) {
            return List.of();
        }
        List<SavedPodList> lists = new ArrayList<>();
        for (String object : splitTopLevelObjects(body)) {
            lists.add(new SavedPodList(
                    parseString(object, "id", ""),
                    parseString(object, "name", "Untitled List"),
                    parseStringListFromObject(object, "entries"),
                    List.of(),
                    parseString(object, "createdAt", ""),
                    parseString(object, "updatedAt", "")));
        }
        return List.copyOf(lists);
    }

    private static List<String> parseStringListFromObject(String json, String key) {
        String body = parseArrayBody(json, key);
        if (body == null) {
            return List.of();
        }
        Matcher matcher = STRING_ARRAY_VALUE.matcher(body);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(unescapeJson(matcher.group(1)));
        }
        return List.copyOf(values);
    }

    private static String parseArrayBody(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int arrayStart = json.indexOf('[', keyIndex);
        if (arrayStart < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = arrayStart; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaping = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(arrayStart + 1, i);
                }
            }
        }
        return null;
    }

    private static List<String> splitTopLevelObjects(String body) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaping = false;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaping = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(body.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private static String jsonSavedPodListArray(List<SavedPodList> lists) {
        if (lists == null || lists.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < lists.size(); i++) {
            SavedPodList list = lists.get(i);
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("    {")
                    .append("\"id\": ").append(jsonString(list.id())).append(", ")
                    .append("\"name\": ").append(jsonString(list.name())).append(", ")
                    .append("\"entries\": ").append(jsonStringArray(list.entries())).append(", ")
                    .append("\"createdAt\": ").append(jsonString(list.createdAt())).append(", ")
                    .append("\"updatedAt\": ").append(jsonString(list.updatedAt()))
                    .append("}");
        }
        sb.append("\n  ]");
        return sb.toString();
    }

    private static String jsonStringArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jsonString(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
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
