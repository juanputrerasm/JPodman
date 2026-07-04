package com.mtm2.jpodman;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A named POD mount list persisted in JPodman's preferences JSON. */
public record SavedPodList(
        String id,
        String name,
        List<String> entries,
        List<String> alwaysMount,
        String createdAt,
        String updatedAt) {
    public SavedPodList {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        name = name == null || name.isBlank() ? "Untitled List" : name.trim();
        entries = entries == null ? List.of() : List.copyOf(entries);
        alwaysMount = alwaysMount == null ? List.of() : List.copyOf(alwaysMount);
        String now = Instant.now().toString();
        createdAt = createdAt == null || createdAt.isBlank() ? now : createdAt;
        updatedAt = updatedAt == null || updatedAt.isBlank() ? now : updatedAt;
    }

    public static SavedPodList create(String name, List<String> entries) {
        String now = Instant.now().toString();
        return new SavedPodList(UUID.randomUUID().toString(), name, entries, List.of(), now, now);
    }

    public SavedPodList withName(String newName) {
        return new SavedPodList(id, newName, entries, alwaysMount, createdAt, Instant.now().toString());
    }

    public SavedPodList withEntries(List<String> newEntries) {
        return new SavedPodList(id, name, newEntries, alwaysMount, createdAt, Instant.now().toString());
    }

    public SavedPodList withAlwaysMount(List<String> newAlwaysMount) {
        return new SavedPodList(id, name, entries, newAlwaysMount, createdAt, Instant.now().toString());
    }

    @Override
    public String toString() {
        return name;
    }
}
