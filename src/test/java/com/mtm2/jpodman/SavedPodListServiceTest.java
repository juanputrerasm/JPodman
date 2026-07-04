package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodIniReader;
import com.mtm2.jpodman.io.PodIniWriter;
import com.mtm2.jpodman.io.SavedPodListService;
import com.mtm2.jpodman.io.SavedPodListService.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavedPodListServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void importsExternalPodIniIntoSavedList() throws IOException {
        Path source = tempDir.resolve("downloaded.ini");
        Files.writeString(source, "3\ntracks/a.pod\ntrucks/b.pod\nTRACKS/A.POD\n");

        SavedPodList list = SavedPodListService.importPodIni(source, "Internet List", 99);

        assertEquals("Internet List", list.name());
        assertEquals(List.of("tracks/a.pod", "trucks/b.pod"), list.entries());
    }

    @Test
    void combinesEntriesOnlyAndDedupesInOrder() {
        SavedPodList list = new SavedPodList("id", "Race Night", List.of("a.pod", "b.pod"), List.of("B.POD", "c.pod"), "c", "u");

        assertEquals(List.of("a.pod", "b.pod"), SavedPodListService.combineForMount(list));
    }

    @Test
    void validatesMissingFilesAgainstKnownItems() throws IOException {
        Path gameRoot = tempDir.resolve("MTM2");
        Files.createDirectories(gameRoot);
        Files.writeString(gameRoot.resolve("existing.pod"), "");
        List<PodListItem> known = List.of(new PodListItem("known.pod", "known.pod [Track:Known]"));

        ValidationResult result = SavedPodListService.validate(gameRoot, List.of("known.pod", "existing.pod", "missing.pod"), known);

        assertEquals(List.of("known.pod"), result.existing());
        assertEquals(List.of("existing.pod", "missing.pod"), result.missing());
    }

    @Test
    void writesOnlyExistingEntriesToGamePodIni() throws IOException {
        Path gameRoot = tempDir.resolve("MTM2");
        Files.createDirectories(gameRoot);

        SavedPodListService.writeExistingToPodIni(gameRoot, List.of("a.pod", "b.pod"), 99);

        assertEquals(List.of("a.pod", "b.pod"), PodIniReader.read(gameRoot.resolve("pod.ini"), 99).entries());
        String newline = System.lineSeparator();
        assertEquals("2" + newline + "a.pod" + newline + "b.pod" + newline, PodIniWriter.toPodIniText(PodIniReader.read(gameRoot.resolve("pod.ini"), 99)));
    }
}
