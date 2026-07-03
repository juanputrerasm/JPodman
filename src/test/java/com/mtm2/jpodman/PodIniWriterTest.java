package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodIniReader;
import com.mtm2.jpodman.io.PodIniWriter;
import com.mtm2.jpodman.io.PodListExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PodIniWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndRoundTripsPodIni() throws IOException {
        Path podIni = tempDir.resolve("pod.ini");
        PodMountList list = PodMountList.of(List.of("startup.pod", "music.pod"), 99);

        PodIniWriter.write(podIni, list);
        PodMountList loaded = PodIniReader.read(podIni, 99);

        assertEquals(list.entries(), loaded.entries());
    }

    @Test
    void saveCreatesMissingPodIni() throws IOException {
        Path podIni = tempDir.resolve("missing").resolve("pod.ini");
        PodMountList list = PodMountList.of(List.of("startup.pod"), 99);

        PodIniWriter.write(podIni, list);

        assertTrue(Files.isRegularFile(podIni));
        assertEquals("1", Files.readAllLines(podIni).get(0));
    }

    @Test
    void exportDoesNotModifyPodIni() throws IOException {
        Path podIni = tempDir.resolve("pod.ini");
        Path export = tempDir.resolve("podlist.txt");
        Files.writeString(podIni, "0\n");
        PodMountList list = PodMountList.of(List.of("startup.pod", "music.pod"), 99);

        PodListExporter.export(export, list);

        assertEquals("0\n", Files.readString(podIni));
        assertTrue(Files.readString(export).contains("startup.pod"));
    }

    @Test
    void podLimitBlocksAdditionalEntries() {
        PodMountList list = PodMountList.empty();

        assertTrue(list.add("one.pod", 1));
        assertFalse(list.add("two.pod", 1));
        assertEquals(1, list.size());
    }
}
