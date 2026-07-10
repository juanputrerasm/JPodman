package com.mtm2.jpodman.io;

import com.mtm2.jpodman.io.PodDisplayNameResolver.PodMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Detects duplicate SIT/TRK resource titles across a POD mount list. */
public final class PodResourceConflictService {
    public ConflictResult detect(Path gameRoot, List<String> mountPaths, PodMetadataService metadataService) {
        Map<String, String> displayTitleByKey = new LinkedHashMap<>();
        Map<String, Set<String>> podKeysByResource = new LinkedHashMap<>();
        Map<String, String> displayMountPathByKey = new LinkedHashMap<>();

        for (String mountPath : mountPaths == null ? List.<String>of() : mountPaths) {
            if (mountPath == null || mountPath.isBlank()) {
                continue;
            }
            String podKey = normalize(mountPath);
            displayMountPathByKey.putIfAbsent(podKey, mountPath);
            Path podPath = PodDiscoveryService.resolveMountedPath(gameRoot, mountPath);
            PodMetadata metadata = metadataService.metadataFor(podPath);
            Set<String> titlesInPod = new LinkedHashSet<>();
            for (String title : metadata.resourceTitles()) {
                if (title == null || title.isBlank()) {
                    continue;
                }
                String resourceKey = title.trim().toUpperCase(Locale.ROOT);
                if (titlesInPod.add(resourceKey)) {
                    displayTitleByKey.putIfAbsent(resourceKey, title.trim().toUpperCase(Locale.ROOT));
                    podKeysByResource.computeIfAbsent(resourceKey, ignored -> new LinkedHashSet<>()).add(podKey);
                }
            }
        }

        Map<String, List<String>> conflictsByPod = new LinkedHashMap<>();
        Set<String> conflictingResourceTitles = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : podKeysByResource.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            String title = displayTitleByKey.get(entry.getKey());
            conflictingResourceTitles.add(title);
            for (String podKey : entry.getValue()) {
                conflictsByPod.computeIfAbsent(podKey, ignored -> new ArrayList<>()).add(title);
            }
        }

        Map<String, List<String>> sortedConflicts = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : displayMountPathByKey.entrySet()) {
            List<String> conflicts = conflictsByPod.get(entry.getKey());
            if (conflicts == null || conflicts.isEmpty()) {
                continue;
            }
            conflicts.sort(String.CASE_INSENSITIVE_ORDER);
            sortedConflicts.put(entry.getKey(), List.copyOf(conflicts));
        }
        List<String> sortedResourceTitles = new ArrayList<>(conflictingResourceTitles);
        sortedResourceTitles.sort(String.CASE_INSENSITIVE_ORDER);
        return new ConflictResult(sortedConflicts, List.copyOf(sortedResourceTitles));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    public record ConflictResult(Map<String, List<String>> conflictsByPod, List<String> conflictingResourceTitles) {
        public ConflictResult {
            conflictsByPod = conflictsByPod == null ? Map.of() : Map.copyOf(conflictsByPod);
            conflictingResourceTitles = conflictingResourceTitles == null ? List.of() : List.copyOf(conflictingResourceTitles);
        }

        public List<String> conflictsFor(String mountPath) {
            return conflictsByPod.getOrDefault(normalize(mountPath), List.of());
        }

        public int conflictCount() {
            return conflictingResourceTitles.size();
        }

        public boolean hasConflicts() {
            return !conflictingResourceTitles.isEmpty();
        }

        public String warningText() {
            List<String> titles = new ArrayList<>(conflictingResourceTitles);
            titles.sort(Comparator.naturalOrder());
            return String.join("\n", titles);
        }
    }
}
