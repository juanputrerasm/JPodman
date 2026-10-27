package com.mtm2.jpodman.io;

import com.mtm2.jpodman.io.PodDisplayNameResolver.PodMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Detects duplicate SIT/SI2/TRK resource titles across a POD mount list. */
public final class PodResourceConflictService {
    /**
     * A resource counts as conflicting when two different PODs define it. Repeats inside
     * a single POD are ignored, and resources with different extensions do not collide
     * because the extension is part of the name. The exception is .SIT and .SI2:
     * Community Patch 3 plays a .SI2 in place of the .SIT with the same stem, so they collide.
     */
    public ConflictResult detect(Path gameRoot, List<String> mountPaths, PodMetadataService metadataService) {
        // Resource key -> the PODs defining it, each with the file names it used for it.
        Map<String, Map<String, Set<String>>> podsByResource = new LinkedHashMap<>();
        Map<String, String> displayMountPathByKey = new LinkedHashMap<>();

        for (String mountPath : mountPaths == null ? List.<String>of() : mountPaths) {
            if (mountPath == null || mountPath.isBlank()) {
                continue;
            }
            String podKey = normalize(mountPath);
            displayMountPathByKey.putIfAbsent(podKey, mountPath);
            Path podPath = PodDiscoveryService.resolveMountedPath(gameRoot, mountPath);
            PodMetadata metadata = metadataService.metadataFor(podPath);
            for (String title : metadata.resourceTitles()) {
                if (title == null || title.isBlank()) {
                    continue;
                }
                podsByResource
                        .computeIfAbsent(TrackScriptNames.resourceKey(title), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(podKey, ignored -> new TreeSet<>())
                        .add(title.trim().toUpperCase(Locale.ROOT));
            }
        }

        Map<String, List<String>> conflictsByPod = new LinkedHashMap<>();
        List<String> conflictingResourceTitles = new ArrayList<>();
        for (Map<String, Set<String>> pods : podsByResource.values()) {
            if (pods.size() < 2) {
                continue;
            }
            // One entry per clashing resource; DEMO.SI2 against DEMO.SIT reads "DEMO.SI2 / DEMO.SIT".
            Set<String> allTitles = new TreeSet<>();
            pods.values().forEach(allTitles::addAll);
            conflictingResourceTitles.add(String.join(" / ", allTitles));
            for (Map.Entry<String, Set<String>> pod : pods.entrySet()) {
                conflictsByPod.computeIfAbsent(pod.getKey(), ignored -> new ArrayList<>()).addAll(pod.getValue());
            }
        }

        Map<String, List<String>> sortedConflicts = new LinkedHashMap<>();
        for (String podKey : displayMountPathByKey.keySet()) {
            List<String> conflicts = conflictsByPod.get(podKey);
            if (conflicts == null || conflicts.isEmpty()) {
                continue;
            }
            conflicts.sort(String.CASE_INSENSITIVE_ORDER);
            sortedConflicts.put(podKey, List.copyOf(conflicts));
        }
        conflictingResourceTitles.sort(String.CASE_INSENSITIVE_ORDER);
        return new ConflictResult(sortedConflicts, List.copyOf(conflictingResourceTitles));
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
