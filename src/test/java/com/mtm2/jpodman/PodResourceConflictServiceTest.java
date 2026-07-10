package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodMetadataService;
import com.mtm2.jpodman.io.PodResourceConflictService;
import com.mtm2.jpodman.io.PodResourceConflictService.ConflictResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PodResourceConflictServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsSameSitTitleInDifferentPodsIgnoringFolders() throws IOException {
        writePod(tempDir.resolve("one.pod"), List.of(entry("WORLD/DEMO.SIT")));
        writePod(tempDir.resolve("two.pod"), List.of(entry("TRACKS/DEMO.SIT")));

        ConflictResult result = detect("one.pod", "two.pod");

        assertEquals(List.of("DEMO.SIT"), result.conflictsFor("one.pod"));
        assertEquals(List.of("DEMO.SIT"), result.conflictsFor("two.pod"));
        assertEquals(1, result.conflictCount());
    }

    @Test
    void matchesResourceTitlesCaseInsensitively() throws IOException {
        writePod(tempDir.resolve("one.pod"), List.of(entry("WORLD/demo.sit")));
        writePod(tempDir.resolve("two.pod"), List.of(entry("TRACKS/DEMO.SIT")));

        ConflictResult result = detect("one.pod", "two.pod");

        assertEquals(List.of("DEMO.SIT"), result.conflictsFor("one.pod"));
        assertEquals(List.of("DEMO.SIT"), result.conflictsFor("two.pod"));
    }

    @Test
    void keepsSitAndTrkTitlesDistinct() throws IOException {
        writePod(tempDir.resolve("one.pod"), List.of(entry("DEMO.SIT")));
        writePod(tempDir.resolve("two.pod"), List.of(entry("DEMO.TRK")));

        ConflictResult result = detect("one.pod", "two.pod");

        assertFalse(result.hasConflicts());
    }

    @Test
    void ignoresDuplicateTitleInsideSinglePodOnly() throws IOException {
        writePod(tempDir.resolve("one.pod"), List.of(entry("A/DEMO.SIT"), entry("B/DEMO.SIT")));
        writePod(tempDir.resolve("two.pod"), List.of(entry("OTHER.SIT")));

        ConflictResult result = detect("one.pod", "two.pod");

        assertFalse(result.hasConflicts());
    }

    @Test
    void listsEveryConflictingTitleForOffendingPod() throws IOException {
        writePod(tempDir.resolve("one.pod"), List.of(entry("A.SIT"), entry("B.TRK"), entry("LOCAL.SIT")));
        writePod(tempDir.resolve("two.pod"), List.of(entry("A.SIT"), entry("B.TRK")));

        ConflictResult result = detect("one.pod", "two.pod");

        assertEquals(List.of("A.SIT", "B.TRK"), result.conflictsFor("one.pod"));
        assertEquals(List.of("A.SIT", "B.TRK"), result.conflictsFor("two.pod"));
        assertEquals(2, result.conflictCount());
    }

    private ConflictResult detect(String... entries) {
        return new PodResourceConflictService().detect(tempDir, List.of(entries), new PodMetadataService());
    }

    private static PodEntry entry(String name) {
        String extension = name.toUpperCase().endsWith(".TRK") ? ".TRK" : ".SIT";
        String text = extension.equals(".TRK") ? "truckName\n" + name + "\n" : "x\n!Race Track Name\n" + name + "\n";
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
