package com.mtm2.jpodman.io.pod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads Terminal Reality POD archives into memory. */
public final class PodArchiveReader {
    private static final Charset LEGACY_CHARSET = StandardCharsets.ISO_8859_1;
    private static final byte[] EPD_MAGIC = {'d', 't', 'x', 'e'};
    private static final byte[] POD2_MAGIC = {'P', 'O', 'D', '2'};
    private static final int POD_COMMENT_SIZE = 80;
    private static final int POD_ENTRY_NAME_SIZE = 32;
    private static final int POD1_ENTRY_SIZE = 40;
    private static final int POD2_ENTRY_SIZE = 20;
    private static final int EPD_ENTRY_SIZE = 80;
    private static final int POD1_HEADER_SIZE = Integer.BYTES + POD_COMMENT_SIZE;
    private static final int POD2_HEADER_SIZE = 8 + POD_COMMENT_SIZE + Integer.BYTES + Integer.BYTES;
    private static final int EPD_COUNT_OFFSET = 0x90;
    private static final int EPD_TABLE_OFFSET = 0x110;
    private static final int EPD_TITLE_OFFSET = 4;
    private static final int EPD_TITLE_SIZE = 4;
    private static final int MAX_REASONABLE_ITEMS = 8192;

    public PodArchive read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < POD1_HEADER_SIZE) {
            throw new IOException("File too small to be a POD archive: " + path);
        }
        if (hasMagic(bytes, EPD_MAGIC)) {
            return readEpd(bytes, path);
        }
        if (hasMagic(bytes, POD2_MAGIC)) {
            return readPod2(bytes, path);
        }
        return readPod1(bytes);
    }

    private PodArchive readPod1(byte[] bytes) throws IOException {
        int itemCount = readInt32LE(bytes, 0);
        if (itemCount < 1 || itemCount > MAX_REASONABLE_ITEMS) {
            throw new IOException("Suspicious POD item count: " + itemCount);
        }
        int tableOffset = POD1_HEADER_SIZE;
        int tableSize = Math.multiplyExact(itemCount, POD1_ENTRY_SIZE);
        if (tableOffset + tableSize > bytes.length) {
            throw new IOException("POD item table exceeds file size");
        }
        String comment = decodeNullTerminated(bytes, Integer.BYTES, POD_COMMENT_SIZE);
        List<PodArchive.Entry> entries = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int entryOffset = tableOffset + i * POD1_ENTRY_SIZE;
            String name = decodeNullTerminated(bytes, entryOffset, POD_ENTRY_NAME_SIZE);
            long length = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + POD_ENTRY_NAME_SIZE));
            long offset = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + POD_ENTRY_NAME_SIZE + Integer.BYTES));
            validateEntryBounds(name, offset, length, bytes.length);
            entries.add(new PodArchive.Entry(name, length, offset));
        }
        return new PodArchive(PodArchive.Format.POD1, comment, bytes, entries);
    }

    private PodArchive readEpd(byte[] bytes, Path path) throws IOException {
        if (bytes.length < EPD_TABLE_OFFSET) {
            throw new IOException("File too small to be an EPD archive: " + path);
        }
        String comment = decodeNullTerminated(bytes, EPD_TITLE_OFFSET, EPD_TITLE_SIZE);
        int itemCount = readInt32LE(bytes, EPD_COUNT_OFFSET);
        if (itemCount < 1 || itemCount > MAX_REASONABLE_ITEMS) {
            throw new IOException("Suspicious EPD item count: " + itemCount);
        }
        int tableSize = Math.multiplyExact(itemCount, EPD_ENTRY_SIZE);
        if (EPD_TABLE_OFFSET + tableSize > bytes.length) {
            throw new IOException("EPD item table exceeds file size");
        }
        List<PodArchive.Entry> entries = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int entryOffset = EPD_TABLE_OFFSET + i * EPD_ENTRY_SIZE;
            String name = decodeEpdEntryName(bytes, entryOffset);
            long length = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + 64));
            long offset = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + 68));
            validateEntryBounds(name, offset, length, bytes.length);
            entries.add(new PodArchive.Entry(name, length, offset));
        }
        return new PodArchive(PodArchive.Format.EPD, comment, bytes, entries);
    }

    private PodArchive readPod2(byte[] bytes, Path path) throws IOException {
        if (bytes.length < POD2_HEADER_SIZE) {
            throw new IOException("File too small to be a POD2 archive: " + path);
        }
        String comment = decodeNullTerminated(bytes, 8, POD_COMMENT_SIZE);
        int itemCount = readInt32LE(bytes, 88);
        if (itemCount < 1 || itemCount > MAX_REASONABLE_ITEMS) {
            throw new IOException("Suspicious POD2 item count: " + itemCount);
        }
        int tableOffset = POD2_HEADER_SIZE;
        int tableSize = Math.multiplyExact(itemCount, POD2_ENTRY_SIZE);
        if (tableOffset + tableSize > bytes.length) {
            throw new IOException("POD2 item table exceeds file size");
        }
        int nameTableOffset = tableOffset + tableSize;
        List<PodArchive.Entry> entries = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int entryOffset = tableOffset + i * POD2_ENTRY_SIZE;
            int pathOffset = readInt32LE(bytes, entryOffset);
            long length = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + 4));
            long offset = Integer.toUnsignedLong(readInt32LE(bytes, entryOffset + 8));
            String name = decodeNullTerminated(bytes, nameTableOffset + pathOffset, bytes.length - (nameTableOffset + pathOffset));
            validateEntryBounds(name, offset, length, bytes.length);
            entries.add(new PodArchive.Entry(name, length, offset));
        }
        return new PodArchive(PodArchive.Format.POD2, comment, bytes, entries);
    }

    private static boolean hasMagic(byte[] bytes, byte[] magic) {
        if (bytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static void validateEntryBounds(String name, long offset, long length, int fileSize) throws IOException {
        if (offset < 0 || length < 0 || offset + length > fileSize) {
            throw new IOException("POD entry exceeds file size: " + name);
        }
    }

    private static String decodeEpdEntryName(byte[] bytes, int entryOffset) {
        String suffix = decodeNullTerminated(bytes, entryOffset + 4, 60);
        String prefix = decodeNullTerminated(bytes, entryOffset, 4);
        if (!suffix.isEmpty() && suffix.charAt(0) == '\\' && isLikelyPathPrefix(prefix)) {
            return prefix + suffix;
        }
        if (!suffix.isEmpty()) {
            return suffix;
        }
        return decodeNullTerminated(bytes, entryOffset, 64);
    }

    private static boolean isLikelyPathPrefix(String prefix) {
        if (prefix.isEmpty()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_') {
                return false;
            }
        }
        return true;
    }

    private static int readInt32LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static String decodeNullTerminated(byte[] bytes, int offset, int length) {
        if (offset < 0 || offset >= bytes.length || length <= 0) {
            return "";
        }
        int end = offset;
        int limit = Math.min(offset + length, bytes.length);
        while (end < limit && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, offset, end - offset, LEGACY_CHARSET).trim();
    }
}
