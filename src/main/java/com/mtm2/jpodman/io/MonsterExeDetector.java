package com.mtm2.jpodman.io;

import com.mtm2.jpodman.GameInstall;
import com.mtm2.jpodman.MonsterVersionInfo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds monster.exe or monsterx.exe for launch/status purposes only. */
public final class MonsterExeDetector {
    private static final Pattern POD_LIMIT_PATTERN = Pattern.compile("^\\s*podLimit\\s*=\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
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
        Optional<MonsterVersionInfo> versionInfo = executable.flatMap(MonsterExeDetector::readProductVersionInfo);
        String version = executable
                .map(path -> versionInfo.map(MonsterVersionInfo::displayLabel).orElseGet(() -> versionLabel(path)))
                .orElse("No monster.exe found");
        return new GameInstall(root, executable, version, versionInfo, readMonsterIniPodLimit(root));
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

    private static Optional<MonsterVersionInfo> readProductVersionInfo(Path executable) {
        if (!isWindows()) {
            return Optional.empty();
        }
        try {
            Class<?> readerClass = Class.forName("com.mtm2.jpodman.io.windows.WindowsProductVersionReader");
            Method method = readerClass.getMethod("readProductVersion", Path.class);
            @SuppressWarnings("unchecked")
            Optional<String> productVersion = (Optional<String>) method.invoke(null, executable);
            return productVersion.flatMap(MonsterVersionInfo::fromProductVersion);
        } catch (ReflectiveOperationException | LinkageError ex) {
            return Optional.empty();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static Optional<Integer> readMonsterIniPodLimit(Path rootFolder) {
        Path monsterIni = rootFolder.resolve("system").resolve("monster.ini");
        if (!Files.isRegularFile(monsterIni)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(monsterIni);
            for (String line : lines) {
                Matcher matcher = POD_LIMIT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    return Optional.of(Integer.parseInt(matcher.group(1)));
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
