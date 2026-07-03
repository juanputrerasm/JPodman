package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodDiscoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PodDiscoveryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void discoversRootRelativeAndExternalAbsolutePods() throws IOException {
        Path gameRoot = tempDir.resolve("game");
        Path extra = tempDir.resolve("extra");
        Files.createDirectories(gameRoot.resolve("Stock"));
        Files.createDirectories(extra);
        Files.writeString(gameRoot.resolve("Stock").resolve("startup.pod"), "");
        Files.writeString(extra.resolve("track.pod"), "");

        List<String> discovered = PodDiscoveryService.discover(gameRoot, List.of(extra), -1, PodMountList.empty());

        assertTrue(discovered.contains("Stock/startup.pod"));
        assertTrue(discovered.contains(extra.resolve("track.pod").toAbsolutePath().normalize().toString().replace('\\', '/')));
    }

    @Test
    void filtersSpacesAndAlreadyMountedPods() throws IOException {
        Path gameRoot = tempDir.resolve("game");
        Files.createDirectories(gameRoot);
        Files.writeString(gameRoot.resolve("startup.pod"), "");
        Files.writeString(gameRoot.resolve("bad name.pod"), "");
        PodMountList mounted = PodMountList.of(List.of("startup.pod"), 99);

        List<String> discovered = PodDiscoveryService.discover(gameRoot, List.of(), -1, mounted);

        assertEquals(List.of(), discovered);
        assertFalse(PodDiscoveryService.isAcceptablePodPath("bad name.pod"));
    }

    @Test
    void respectsFolderDepth() throws IOException {
        Path gameRoot = tempDir.resolve("game");
        Files.createDirectories(gameRoot.resolve("one").resolve("two"));
        Files.writeString(gameRoot.resolve("root.pod"), "");
        Files.writeString(gameRoot.resolve("one").resolve("one.pod"), "");
        Files.writeString(gameRoot.resolve("one").resolve("two").resolve("two.pod"), "");

        List<String> depthZero = PodDiscoveryService.discover(gameRoot, List.of(), 0, PodMountList.empty());

        assertTrue(depthZero.contains("root.pod"));
        assertFalse(depthZero.contains("one/one.pod"));
        assertFalse(depthZero.contains("one/two/two.pod"));
    }
}
