package com.mtm2.jpodman.io;

import com.mtm2.jpodman.io.PodDisplayNameResolver.PodMetadata;
import com.mtm2.jpodman.io.pod.PodArchive;
import com.mtm2.jpodman.io.pod.PodArchiveReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads and caches track/truck metadata from POD files. */
public final class PodMetadataService {
    private final PodArchiveReader reader = new PodArchiveReader();
    private final Map<Path, CacheEntry> cache = new HashMap<>();

    public String displayLabel(Path gameRoot, String mountPath) {
        Path podPath = PodDiscoveryService.resolveMountedPath(gameRoot, mountPath);
        PodMetadata metadata = metadataFor(podPath);
        return PodDisplayNameResolver.displayLabel(mountPath, metadata);
    }

    public PodMetadata metadataFor(Path podPath) {
        try {
            Path normalized = podPath.toAbsolutePath().normalize();
            if (!Files.isRegularFile(normalized)) {
                return PodMetadata.empty();
            }
            long size = Files.size(normalized);
            FileTime modified = Files.getLastModifiedTime(normalized);
            CacheEntry cached = cache.get(normalized);
            if (cached != null && cached.size == size && cached.modified.equals(modified)) {
                return cached.metadata;
            }
            PodMetadata metadata = readMetadata(normalized);
            cache.put(normalized, new CacheEntry(size, modified, metadata));
            return metadata;
        } catch (IOException | RuntimeException ex) {
            return PodMetadata.empty();
        }
    }

    public void clear() {
        cache.clear();
    }

    private PodMetadata readMetadata(Path podPath) throws IOException {
        PodArchive archive = reader.read(podPath);
        List<String> tracks = new ArrayList<>();
        for (PodArchive.Entry entry : archive.getEntriesByExtension(".sit")) {
            tracks.add(SitTrackNameParser.displayName(archive.getEntryBytes(entry), entry.name()));
        }
        List<String> trucks = new ArrayList<>();
        for (PodArchive.Entry entry : archive.getEntriesByExtension(".trk")) {
            trucks.add(TrkTruckNameParser.displayName(archive.getEntryBytes(entry), entry.name()));
        }
        return new PodMetadata(tracks, trucks);
    }

    private record CacheEntry(long size, FileTime modified, PodMetadata metadata) {}
}
