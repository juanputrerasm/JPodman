package com.mtm2.jpodman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ordered, duplicate-free list of mounted POD paths. */
public final class PodMountList {
    public static final List<String> STOCK_PODS = List.of(
            "fixmore4.pod",
            "startup.pod",
            "music.pod",
            "sound.pod",
            "truck2.pod",
            "cockpit.pod",
            "ui.pod",
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

    private final List<String> entries = new ArrayList<>();
    private final Set<String> normalizedEntries = new LinkedHashSet<>();

    public static PodMountList empty() {
        return new PodMountList();
    }

    public static PodMountList of(List<String> paths, int podLimit) {
        PodMountList list = new PodMountList();
        if (paths != null) {
            for (String path : paths) {
                list.add(path, podLimit);
            }
        }
        return list;
    }

    public boolean add(String path, int podLimit) {
        if (path == null || path.isBlank() || entries.size() >= podLimit) {
            return false;
        }
        String normalized = normalizeKey(path);
        if (!normalizedEntries.add(normalized)) {
            return false;
        }
        entries.add(path.trim());
        return true;
    }

    public boolean remove(String path) {
        String normalized = normalizeKey(path);
        if (!normalizedEntries.remove(normalized)) {
            return false;
        }
        entries.removeIf(value -> normalizeKey(value).equals(normalized));
        return true;
    }

    public boolean contains(String path) {
        return normalizedEntries.contains(normalizeKey(path));
    }

    public void clear() {
        entries.clear();
        normalizedEntries.clear();
    }

    public void sortCaseInsensitive() {
        entries.sort(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)));
        normalizedEntries.clear();
        for (String entry : entries) {
            normalizedEntries.add(normalizeKey(entry));
        }
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    public boolean isFull(int podLimit) {
        return entries.size() >= podLimit;
    }

    public static List<String> stockEntries(boolean minimal) {
        int count = minimal ? 7 : STOCK_PODS.size();
        List<String> result = new ArrayList<>();
        for (String pod : STOCK_PODS.subList(0, count)) {
            result.add("Fixes/" + pod);
        }
        return List.copyOf(result);
    }

    private static String normalizeKey(String path) {
        return path == null ? "" : path.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
