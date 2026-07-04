package com.mtm2.jpodman.io;

import java.util.List;
import java.util.StringJoiner;

/** Formats mounted/discovered POD display labels from parsed metadata. */
public final class PodDisplayNameResolver {
    private PodDisplayNameResolver() {}

    public static String displayLabel(String mountPath, PodMetadata metadata) {
        return displayLabel(mountPath, metadata, false);
    }

    public static String displayLabel(String mountPath, PodMetadata metadata, boolean systemPod) {
        if (metadata == null || metadata.isEmpty()) {
            return systemPod ? mountPath + " [System]" : mountPath;
        }
        StringJoiner groups = new StringJoiner("; ");
        if (systemPod) {
            groups.add("System");
        }
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

    public record PodMetadata(List<String> tracks, List<String> trucks) {
        public PodMetadata {
            tracks = tracks == null ? List.of() : List.copyOf(tracks);
            trucks = trucks == null ? List.of() : List.copyOf(trucks);
        }

        public static PodMetadata empty() {
            return new PodMetadata(List.of(), List.of());
        }

        public boolean isEmpty() {
            return tracks.isEmpty() && trucks.isEmpty();
        }
    }
}
