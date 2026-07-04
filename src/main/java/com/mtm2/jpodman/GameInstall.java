package com.mtm2.jpodman;

import java.nio.file.Path;
import java.util.Optional;

/** Current game folder and optional executable information. */
public record GameInstall(
        Path rootFolder,
        Optional<Path> executable,
        String versionLabel,
        Optional<MonsterVersionInfo> versionInfo,
        Optional<Integer> monsterIniPodLimit) {
    public Path podIniPath() {
        return rootFolder.resolve("pod.ini");
    }

    public Path monsterIniPath() {
        return rootFolder.resolve("system").resolve("monster.ini");
    }

    public boolean canLaunch() {
        return executable.isPresent();
    }

    public Optional<Integer> warningPodLimit() {
        return monsterIniPodLimit.or(() -> versionInfo.map(MonsterVersionInfo::suggestedPodLimit));
    }
}
