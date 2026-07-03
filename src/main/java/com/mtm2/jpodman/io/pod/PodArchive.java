package com.mtm2.jpodman.io.pod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Immutable in-memory representation of a Terminal Reality POD archive. */
public final class PodArchive {
    public enum Format {
        POD1("POD1"),
        POD2("POD2"),
        EPD("EPD");

        private final String displayName;

        Format(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private final Format format;
    private final String comment;
    private final byte[] bytes;
    private final List<Entry> entries;

    PodArchive(Format format, String comment, byte[] bytes, List<Entry> entries) {
        this.format = Objects.requireNonNull(format);
        this.comment = Objects.requireNonNull(comment);
        this.bytes = Objects.requireNonNull(bytes);
        this.entries = List.copyOf(entries);
    }

    public Format getFormat() {
        return format;
    }

    public String getComment() {
        return comment;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<Entry> getEntriesByExtension(String extension) {
        String normalized = extension.toUpperCase(Locale.ROOT);
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.name().toUpperCase(Locale.ROOT).endsWith(normalized)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    public Optional<Entry> findEntry(String name) {
        String normalized = name.toUpperCase(Locale.ROOT);
        for (Entry entry : entries) {
            if (entry.name().toUpperCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<Entry> findEntryByTitle(String name) {
        String normalized = name.toUpperCase(Locale.ROOT);
        for (Entry entry : entries) {
            if (entry.title().equals(normalized)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public byte[] getEntryBytes(Entry entry) {
        int start = Math.toIntExact(entry.offset());
        int end = Math.toIntExact(entry.offset() + entry.length());
        return java.util.Arrays.copyOfRange(bytes, start, end);
    }

    public record Entry(String name, long length, long offset) {
        public String title() {
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            return (slash >= 0 ? name.substring(slash + 1) : name).toUpperCase(Locale.ROOT);
        }
    }
}
