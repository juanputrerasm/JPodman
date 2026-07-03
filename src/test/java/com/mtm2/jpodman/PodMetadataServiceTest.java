package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PodMetadataServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void readsTrackAndTruckMetadataFromPod() throws IOException {
        Path pod = tempDir.resolve("Combo.pod");
        writePod(pod, List.of(
                entry("WORLD/LAGUNA.SIT", "x\n!Race Track Name\nLaguna Seca\n"),
                entry("TRUCK/BIGFOOT.TRK", "truckName\nBigfoot 15\n")));

        String label = new PodMetadataService().displayLabel(tempDir, "Combo.pod");

        assertEquals("Combo.pod [Track:Laguna Seca; Truck:Bigfoot 15]", label);
    }

    @Test
    void invalidPodFallsBackToPlainLabel() throws IOException {
        Path pod = tempDir.resolve("bad.pod");
        Files.writeString(pod, "not a pod");

        assertEquals("bad.pod", new PodMetadataService().displayLabel(tempDir, "bad.pod"));
    }

    @Test
    void cacheInvalidatesWhenFileMetadataChanges() throws IOException {
        Path pod = tempDir.resolve("Track.pod");
        PodMetadataService service = new PodMetadataService();
        writePod(pod, List.of(entry("A.SIT", "x\n!Race Track Name\nFirst\n")));
        assertEquals("Track.pod [Track:First]", service.displayLabel(tempDir, "Track.pod"));

        writePod(pod, List.of(entry("A.SIT", "x\n!Race Track Name\nSecond\nextra\n")));
        Files.setLastModifiedTime(pod, FileTime.fromMillis(System.currentTimeMillis() + 5_000));

        assertEquals("Track.pod [Track:Second]", service.displayLabel(tempDir, "Track.pod"));
    }

    private static PodEntry entry(String name, String text) {
        return new PodEntry(name, text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void writePod(Path path, List<PodEntry> entries) throws IOException {
        int headerSize = 84;
        int tableSize = entries.size() * 40;
        int dataOffset = headerSize + tableSize;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeIntLE(out, entries.size());
        out.write(new byte[80]);
        int offset = dataOffset;
        for (PodEntry entry : entries) {
            byte[] name = entry.name().getBytes(StandardCharsets.ISO_8859_1);
            byte[] nameField = new byte[32];
            System.arraycopy(name, 0, nameField, 0, Math.min(name.length, nameField.length));
            out.write(nameField);
            writeIntLE(out, entry.bytes().length);
            writeIntLE(out, offset);
            offset += entry.bytes().length;
        }
        for (PodEntry entry : entries) {
            out.write(entry.bytes());
        }
        Files.write(path, out.toByteArray());
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private record PodEntry(String name, byte[] bytes) {}
}
