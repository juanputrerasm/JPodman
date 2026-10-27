package com.mtm2.jpodman.io;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Extracts display names from MTM1, MTM2 and Community Patch "MTM2.1" TRK truck files. */
public final class TrkTruckNameParser {
    private static final String MTM1_HEADER = "truckName";
    private static final String MTM2_HEADER = "MTM2 truckName";
    /** The Community Patch header that turns on the patched engine's extended truck loading. */
    private static final String MTM21_HEADER = "MTM2.1 truckName";

    private TrkTruckNameParser() {}

    public static String displayName(byte[] trkBytes, String entryName) {
        List<String> lines = normalizedLines(trkBytes);
        if (lines.isEmpty()) {
            return SitTrackNameParser.titleWithoutExtension(entryName, ".TRK");
        }

        for (int i = 0; i < lines.size() - 1; i++) {
            String line = lines.get(i);
            if (isHeader(line)) {
                String name = lines.get(i + 1).trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }

        // Positional form: MTM2 and MTM2.1 files put the name on the line after the version header.
        int index = 0;
        if (lines.get(index).toUpperCase(java.util.Locale.ROOT).startsWith("MTM2")) {
            index++;
        }
        if (index < lines.size()) {
            String name = lines.get(index).trim();
            if (!name.isEmpty() && !isHeader(name)) {
                return name;
            }
        }
        return SitTrackNameParser.titleWithoutExtension(entryName, ".TRK");
    }

    private static boolean isHeader(String line) {
        return MTM1_HEADER.equalsIgnoreCase(line)
                || MTM2_HEADER.equalsIgnoreCase(line)
                || MTM21_HEADER.equalsIgnoreCase(line);
    }

    private static List<String> normalizedLines(byte[] trkBytes) {
        String text = new String(trkBytes == null ? new byte[0] : trkBytes, StandardCharsets.ISO_8859_1)
                .replace("\0", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String[] rawLines = text.split("\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
