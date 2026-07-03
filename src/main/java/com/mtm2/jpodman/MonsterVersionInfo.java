package com.mtm2.jpodman;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Parsed monster.exe ProductVersion metadata and suggested POD capacity. */
public record MonsterVersionInfo(String productVersion, String label, int suggestedPodLimit) {
    private static final VersionNumber MTM2_RETAIL = VersionNumber.parse("2.0.41.0").orElseThrow();
    private static final VersionNumber MTM2_PATCHED = VersionNumber.parse("2.0.42.0").orElseThrow();

    public static Optional<MonsterVersionInfo> fromProductVersion(String productVersion) {
        Optional<VersionNumber> parsed = VersionNumber.parse(productVersion);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        VersionNumber version = parsed.get();
        if (version.major() == 1) {
            return Optional.of(new MonsterVersionInfo(productVersion, "MTM1", 15));
        }
        if (version.compareTo(MTM2_RETAIL) < 0) {
            return Optional.of(new MonsterVersionInfo(productVersion, "MTM2 beta", 30));
        }
        if (version.compareTo(MTM2_RETAIL) == 0) {
            return Optional.of(new MonsterVersionInfo(productVersion, "MTM2 retail/trial", 30));
        }
        if (version.compareTo(MTM2_PATCHED) == 0) {
            return Optional.of(new MonsterVersionInfo(productVersion, "MTM2 patched", 99));
        }
        return Optional.of(new MonsterVersionInfo(productVersion, "Community patch", 199));
    }

    public String displayLabel() {
        return label + " version " + productVersion;
    }

    private record VersionNumber(List<Integer> parts) implements Comparable<VersionNumber> {
        static Optional<VersionNumber> parse(String text) {
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            String[] tokens = text.trim().split("\\.");
            List<Integer> parts = new ArrayList<>();
            for (String token : tokens) {
                try {
                    parts.add(Integer.parseInt(token));
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }
            return parts.isEmpty() ? Optional.empty() : Optional.of(new VersionNumber(List.copyOf(parts)));
        }

        int major() {
            return parts.isEmpty() ? 0 : parts.get(0);
        }

        @Override
        public int compareTo(VersionNumber other) {
            int max = Math.max(parts.size(), other.parts.size());
            for (int i = 0; i < max; i++) {
                int left = i < parts.size() ? parts.get(i) : 0;
                int right = i < other.parts.size() ? other.parts.get(i) : 0;
                int comparison = Integer.compare(left, right);
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }
    }
}
