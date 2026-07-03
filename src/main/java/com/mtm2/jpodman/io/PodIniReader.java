package com.mtm2.jpodman.io;

import com.mtm2.jpodman.PodMountList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads MTM/Cowpod pod.ini files. */
public final class PodIniReader {
    private PodIniReader() {}

    public static PodMountList read(Path path, int podLimit) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return PodMountList.empty();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return PodMountList.empty();
        }

        int declaredCount = parseCount(lines.get(0));
        int count = Math.min(Math.min(declaredCount, podLimit), Math.max(0, lines.size() - 1));
        PodMountList list = PodMountList.empty();
        for (int i = 0; i < count; i++) {
            String entry = lines.get(i + 1).trim();
            if (!entry.isBlank()) {
                list.add(entry, podLimit);
            }
        }
        return list;
    }

    private static int parseCount(String line) {
        try {
            return Math.max(0, Integer.parseInt(line.trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
