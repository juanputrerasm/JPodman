package com.mtm2.jpodman;

import java.nio.file.Path;
import java.util.Optional;

/** Current game folder and optional executable information. */
public record GameInstall(Path rootFolder, Optional<Path> executable, String versionLabel) {
    public Path podIniPath() {
        return rootFolder.resolve("pod.ini");
    }

    public boolean canLaunch() {
        return executable.isPresent();
    }
}
