package com.mtm2.jpodman.io;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Extracts display names from MTM1 and MTM2 TRK truck files. */
public final class TrkTruckNameParser {
    private TrkTruckNameParser() {}

    public static String displayName(byte[] trkBytes, String entryName) {
        List<String> lines = normalizedLines(trkBytes);
        if (lines.isEmpty()) {
            return SitTrackNameParser.titleWithoutExtension(entryName, ".TRK");
        }

        for (int i = 0; i < lines.size() - 1; i++) {
            String line = lines.get(i);
            if ("truckName".equalsIgnoreCase(line) || "MTM2 truckName".equalsIgnoreCase(line)) {
                String name = lines.get(i + 1).trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }

        int index = 0;
        if (lines.get(index).toUpperCase().startsWith("MTM2")) {
            index++;
        }
        if (index < lines.size()) {
            String name = lines.get(index).trim();
            if (!name.isEmpty()
                    && !"truckName".equalsIgnoreCase(name)
                    && !"MTM2 truckName".equalsIgnoreCase(name)) {
                return name;
            }
        }
        return SitTrackNameParser.titleWithoutExtension(entryName, ".TRK");
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
