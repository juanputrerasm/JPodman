package com.mtm2.jpodman.io;

import java.util.List;
import java.util.StringJoiner;

/** Formats mounted/discovered POD display labels from parsed metadata. */
public final class PodDisplayNameResolver {
    private PodDisplayNameResolver() {}

    public static String displayLabel(String mountPath, PodMetadata metadata) {
        return displayLabel(mountPath, metadata, false, List.of());
    }

    public static String displayLabel(String mountPath, PodMetadata metadata, boolean systemPod) {
        return displayLabel(mountPath, metadata, systemPod, List.of());
    }

    public static String displayLabel(String mountPath, PodMetadata metadata, boolean systemPod, List<String> conflicts) {
        List<String> conflictTitles = conflicts == null ? List.of() : List.copyOf(conflicts);
        if (metadata == null || metadata.isEmpty()) {
            if (!systemPod && conflictTitles.isEmpty()) {
                return mountPath;
            }
            StringJoiner groups = new StringJoiner("; ");
            if (systemPod) {
                groups.add("System");
            }
            addConflictGroup(groups, conflictTitles);
            return mountPath + " [" + groups + "]";
        }
        StringJoiner groups = new StringJoiner("; ");
        if (systemPod) {
            groups.add("System");
        }
        addConflictGroup(groups, conflictTitles);
        addGroup(groups, "Track", "Tracks", metadata.tracks());
        addGroup(groups, "Truck", "Trucks", metadata.trucks());
        return mountPath + " [" + groups + "]";
    }

    public static String missingLabel(String mountPath) {
        return mountPath + " [missing]";
    }

    static void addGroup(StringJoiner groups, String singular, String plural, List<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        if (names.size() > 3) {
            groups.add(plural + ":" + names.size());
        } else {
            groups.add((names.size() == 1 ? singular : plural) + ":" + String.join(", ", names));
        }
    }

    static void addConflictGroup(StringJoiner groups, List<String> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        groups.add((conflicts.size() == 1 ? "Conflict: " : "Conflicts: ") + String.join(", ", conflicts));
    }

    public record PodMetadata(List<String> tracks, List<String> trucks, List<String> trackResourceTitles, List<String> truckResourceTitles) {
        public PodMetadata(List<String> tracks, List<String> trucks) {
            this(tracks, trucks, List.of(), List.of());
        }

        public PodMetadata {
            tracks = tracks == null ? List.of() : List.copyOf(tracks);
            trucks = trucks == null ? List.of() : List.copyOf(trucks);
            trackResourceTitles = trackResourceTitles == null ? List.of() : List.copyOf(trackResourceTitles);
            truckResourceTitles = truckResourceTitles == null ? List.of() : List.copyOf(truckResourceTitles);
        }

        public static PodMetadata empty() {
            return new PodMetadata(List.of(), List.of(), List.of(), List.of());
        }

        public boolean isEmpty() {
            return tracks.isEmpty() && trucks.isEmpty() && trackResourceTitles.isEmpty() && truckResourceTitles.isEmpty();
        }

        public List<String> resourceTitles() {
            java.util.ArrayList<String> titles = new java.util.ArrayList<>();
            titles.addAll(trackResourceTitles);
            titles.addAll(truckResourceTitles);
            return List.copyOf(titles);
        }
    }
}
