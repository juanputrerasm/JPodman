package com.mtm2.jpodman.io;

import com.mtm2.jpodman.PodMountList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exports the visible mounted POD list without changing the game pod.ini. */
public final class PodListExporter {
    private PodListExporter() {}

    public static void export(Path path, PodMountList list) throws IOException {
        if (path == null) {
            throw new IOException("No export path selected.");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toText(list), StandardCharsets.UTF_8);
    }

    public static String toText(PodMountList list) {
        StringBuilder sb = new StringBuilder();
        sb.append("# JPodman mounted POD list").append(System.lineSeparator());
        sb.append("# Count: ").append(list.size()).append(System.lineSeparator());
        for (String entry : list.entries()) {
            sb.append(entry).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
