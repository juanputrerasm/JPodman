package com.mtm2.jpodman.io;

import com.mtm2.jpodman.PodListItem;
import com.mtm2.jpodman.PodMountList;
import com.mtm2.jpodman.SavedPodList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Utilities for saved POD list import, validation, and mounting. */
public final class SavedPodListService {
    private SavedPodListService() {}

    public static SavedPodList importPodIni(Path podIni, String name, int podLimit) throws IOException {
        PodMountList list = PodIniReader.read(podIni, podLimit);
        String defaultName = podIni.getFileName() == null ? "Imported pod.ini" : podIni.getFileName().toString().replaceFirst("(?i)\\.ini$", "");
        return SavedPodList.create(name == null || name.isBlank() ? defaultName : name, dedupe(list.entries()));
    }

    public static List<String> combineForMount(SavedPodList list) {
        return list == null ? List.of() : dedupe(list.entries());
    }

    public static List<String> dedupe(List<String> entries) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String entry : entries == null ? List.<String>of() : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String key = entry.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(entry.trim());
            }
        }
        return List.copyOf(result);
    }

    public static ValidationResult validate(Path gameRoot, List<String> entries, List<PodListItem> knownItems) {
        Set<String> known = new LinkedHashSet<>();
        for (PodListItem item : knownItems == null ? List.<PodListItem>of() : knownItems) {
            known.add(normalize(item.mountPath()));
        }
        List<String> existing = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String entry : dedupe(entries)) {
            if (known.contains(normalize(entry))) {
                existing.add(entry);
            } else {
                missing.add(entry);
            }
        }
        return new ValidationResult(List.copyOf(existing), List.copyOf(missing));
    }

    public static void writeExistingToPodIni(Path gameRoot, List<String> entries, int podLimit) throws IOException {
        PodIniWriter.write(gameRoot.resolve("pod.ini"), PodMountList.of(entries, podLimit));
    }

    private static String normalize(String entry) {
        return entry == null ? "" : entry.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    public record ValidationResult(List<String> existing, List<String> missing) {}
}
