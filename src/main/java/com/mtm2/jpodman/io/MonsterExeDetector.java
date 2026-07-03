package com.mtm2.jpodman.io;

import com.mtm2.jpodman.GameInstall;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** Finds monster.exe or monsterx.exe for launch/status purposes only. */
public final class MonsterExeDetector {
    private static final Map<Long, String> VERSION_BY_SIZE = Map.of(
            2_920_448L, "MTM2 Unpatched",
            2_925_568L, "MTM2 Patched",
            2_903_552L, "MTM2 Patched (Spanish)",
            2_918_400L, "MTM2 Trial",
            3_109_888L, "MTM1 Unpatched",
            2_455_552L, "MTM1 Patched");

    private MonsterExeDetector() {}

    public static GameInstall detect(Path rootFolder) {
        Path root = rootFolder == null ? Path.of(".").toAbsolutePath().normalize() : rootFolder.toAbsolutePath().normalize();
        Optional<Path> executable = firstRegularFile(root.resolve("monster.exe")).or(() -> firstRegularFile(root.resolve("monsterx.exe")));
        String version = executable.map(MonsterExeDetector::versionLabel).orElse("No monster.exe found");
        return new GameInstall(root, executable, version);
    }

    private static Optional<Path> firstRegularFile(Path path) {
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    private static String versionLabel(Path executable) {
        try {
            long size = Files.size(executable);
            return VERSION_BY_SIZE.getOrDefault(size, "Unknown monster.exe");
        } catch (IOException ex) {
            return "Unknown monster.exe";
        }
    }
}
