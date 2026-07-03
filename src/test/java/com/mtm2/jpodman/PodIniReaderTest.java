package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodIniReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PodIniReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void missingPodIniReadsAsEmptyList() throws IOException {
        PodMountList list = PodIniReader.read(tempDir.resolve("pod.ini"), 99);

        assertEquals(0, list.size());
    }

    @Test
    void readsDeclaredEntriesUpToConfiguredLimit() throws IOException {
        Path podIni = tempDir.resolve("pod.ini");
        Files.writeString(podIni, "3\nstartup.pod\nmusic.pod\nsound.pod\n");

        PodMountList list = PodIniReader.read(podIni, 2);

        assertEquals(2, list.size());
        assertEquals("startup.pod", list.entries().get(0));
        assertEquals("music.pod", list.entries().get(1));
    }

    @Test
    void badCountReadsAsEmptyList() throws IOException {
        Path podIni = tempDir.resolve("pod.ini");
        Files.writeString(podIni, "bad\nstartup.pod\n");

        PodMountList list = PodIniReader.read(podIni, 99);

        assertEquals(0, list.size());
    }
}
