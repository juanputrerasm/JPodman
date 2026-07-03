package com.mtm2.jpodman.io;

import com.mtm2.jpodman.PodMountList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes MTM/Cowpod pod.ini files. */
public final class PodIniWriter {
    private PodIniWriter() {}

    public static void write(Path path, PodMountList list) throws IOException {
        if (path == null) {
            throw new IOException("No pod.ini path selected.");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toPodIniText(list), StandardCharsets.UTF_8);
    }

    public static String toPodIniText(PodMountList list) {
        StringBuilder sb = new StringBuilder();
        sb.append(list.size()).append(System.lineSeparator());
        for (String entry : list.entries()) {
            sb.append(entry).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
