package com.mtm2.jpodman.io;

import com.mtm2.jpodman.GameInstall;
import com.mtm2.jpodman.MonsterVersionInfo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds known POD-game executables for launch/status purposes only. */
public final class MonsterExeDetector {
    private static final Pattern POD_LIMIT_PATTERN = Pattern.compile("^\\s*podLimit\\s*=\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Map<Long, String> VERSION_BY_SIZE = Map.of(
            2_920_448L, "MTM2 Unpatched",
            2_925_568L, "MTM2 Patched",
            2_903_552L, "MTM2 Patched (Spanish)",
            2_918_400L, "MTM2 Trial",
            3_109_888L, "MTM1 Unpatched",
            2_455_552L, "MTM1 Patched");
    private static final Map<String, ExecutableDescriptor> EXECUTABLES = executableDescriptors();

    private MonsterExeDetector() {}

    public static GameInstall detect(Path rootFolder) {
        Path root = rootFolder == null ? Path.of(".").toAbsolutePath().normalize() : rootFolder.toAbsolutePath().normalize();
        Optional<DetectedExecutable> detected = findKnownExecutable(root);
        Optional<Path> executable = detected.map(DetectedExecutable::path);
        Optional<MonsterVersionInfo> versionInfo = detected
                .filter(value -> value.descriptor().mtm())
                .map(DetectedExecutable::path)
                .flatMap(MonsterExeDetector::readProductVersionInfo);
        String version = detected
                .map(value -> value.descriptor().mtm()
                        ? versionInfo.map(MonsterVersionInfo::displayLabel).orElseGet(() -> versionLabel(value.path()))
                        : value.descriptor().gameName())
                .orElse("Generic POD game");
        String gameName = detected.map(value -> value.descriptor().gameName()).orElse("Generic POD game");
        Optional<Integer> executablePodLimit = detected.flatMap(value -> value.descriptor().podLimit());
        boolean mtmExecutable = detected.map(value -> value.descriptor().mtm()).orElse(false);
        return new GameInstall(
                root,
                executable,
                version,
                gameName,
                executablePodLimit,
                mtmExecutable,
                versionInfo,
                mtmExecutable ? readMonsterIniPodLimit(root) : Optional.empty());
    }

    private static Optional<DetectedExecutable> findKnownExecutable(Path root) {
        if (!Files.isDirectory(root)) {
            return Optional.empty();
        }
        try (var stream = Files.list(root)) {
            Map<String, Path> files = new LinkedHashMap<>();
            stream.filter(Files::isRegularFile)
                    .forEach(path -> files.putIfAbsent(path.getFileName().toString().toLowerCase(Locale.ROOT), path));
            for (Map.Entry<String, ExecutableDescriptor> entry : EXECUTABLES.entrySet()) {
                Path path = files.get(entry.getKey());
                if (path != null) {
                    return Optional.of(new DetectedExecutable(path, entry.getValue()));
                }
            }
        } catch (IOException ignored) {
            for (Map.Entry<String, ExecutableDescriptor> entry : EXECUTABLES.entrySet()) {
                Path candidate = root.resolve(entry.getValue().fileName());
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(new DetectedExecutable(candidate, entry.getValue()));
                }
            }
        }
        return Optional.empty();
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

    private static Map<String, ExecutableDescriptor> executableDescriptors() {
        Map<String, ExecutableDescriptor> descriptors = new LinkedHashMap<>();
        add(descriptors, new ExecutableDescriptor("monster.exe", "Monster Truck Madness", Optional.empty(), true));
        add(descriptors, new ExecutableDescriptor("monsterx.exe", "Monster Truck Madness", Optional.empty(), true));
        add(descriptors, new ExecutableDescriptor("TV.EXE", "Terminal Velocity", Optional.of(15), false));
        add(descriptors, new ExecutableDescriptor("FURY3.EXE", "Fury3", Optional.of(15), false));
        add(descriptors, new ExecutableDescriptor("HELLBEND.EXE", "Hellbender", Optional.of(15), false));
        add(descriptors, new ExecutableDescriptor("cart.exe", "CPR", Optional.of(30), false));
        add(descriptors, new ExecutableDescriptor("nocturne.exe", "Nocturne", Optional.empty(), false));
        add(descriptors, new ExecutableDescriptor("4x4.exe", "4x4 Evo", Optional.empty(), false));
        add(descriptors, new ExecutableDescriptor("4x42.exe", "4x4 Evo 2", Optional.empty(), false));
        return Collections.unmodifiableMap(descriptors);
    }

    private static void add(Map<String, ExecutableDescriptor> descriptors, ExecutableDescriptor descriptor) {
        descriptors.put(descriptor.fileName().toLowerCase(Locale.ROOT), descriptor);
    }

    private record ExecutableDescriptor(String fileName, String gameName, Optional<Integer> podLimit, boolean mtm) {}

    private record DetectedExecutable(Path path, ExecutableDescriptor descriptor) {}
}
