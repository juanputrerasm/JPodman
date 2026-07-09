package com.mtm2.jpodman.io;

import com.mtm2.jpodman.PodMountList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** Discovers .pod files in the game folder and configured external folders. */
public final class PodDiscoveryService {
    private PodDiscoveryService() {}

    public static List<String> discover(Path gameRoot, List<Path> extraFolders, int folderDepth, PodMountList mountedList) {
        Set<String> discovered = new LinkedHashSet<>();
        Path root = normalize(gameRoot);
        scanFolder(root, root, false, folderDepth, mountedList, discovered);
        if (extraFolders != null) {
            for (Path folder : extraFolders) {
                scanFolder(normalize(folder), normalize(folder), true, folderDepth, mountedList, discovered);
            }
        }
        return List.copyOf(discovered);
    }

    public static boolean isAcceptablePodPath(String path) {
        return path != null
                && !path.isBlank()
                && path.toLowerCase(Locale.ROOT).endsWith(".pod");
    }

    public static Path resolveMountedPath(Path gameRoot, String entry) {
        Path path = Path.of(entry);
        return path.isAbsolute() ? path.normalize() : normalize(gameRoot).resolve(entry).normalize();
    }

    private static void scanFolder(
            Path baseFolder,
            Path currentFolder,
            boolean external,
            int folderDepth,
            PodMountList mountedList,
            Set<String> discovered) {
        if (currentFolder == null || !Files.isDirectory(currentFolder)) {
            return;
        }
        int maxDepth = folderDepth < 0 ? Integer.MAX_VALUE : folderDepth + 1;
        try (Stream<Path> stream = Files.walk(currentFolder, maxDepth)) {
            List<Path> pods = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pod"))
                    .forEach(pods::add);
            pods.sort((left, right) -> left.toString().compareToIgnoreCase(right.toString()));
            for (Path pod : pods) {
                String entry = external ? pod.toAbsolutePath().normalize().toString() : toRootRelative(baseFolder, pod);
                entry = entry.replace('\\', '/');
                if (isAcceptablePodPath(entry) && !mountedList.contains(entry)) {
                    discovered.add(entry);
                }
            }
        } catch (IOException ignored) {
            // Unreadable folders are skipped; the UI can still operate on known entries.
        }
    }

    private static String toRootRelative(Path root, Path file) {
        try {
            return root.relativize(file.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException ex) {
            return file.toAbsolutePath().normalize().toString();
        }
    }

    private static Path normalize(Path path) {
        return path == null ? Path.of(".").toAbsolutePath().normalize() : path.toAbsolutePath().normalize();
    }
}
