package com.mtm2.jpodman.io;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Extracts display names from MTM SIT track files. */
public final class SitTrackNameParser {
    private static final byte[] TRAXXV1 = "TRAXXV1".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] TRAXXV2 = "TRAXXV2".getBytes(StandardCharsets.ISO_8859_1);

    private SitTrackNameParser() {}

    public static String displayName(byte[] sitBytes, String entryName) {
        byte[] textBytes = stripTraxxTail(sitBytes == null ? new byte[0] : sitBytes);
        String text = new String(textBytes, StandardCharsets.ISO_8859_1)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length - 1; i++) {
            if ("!Race Track Name".equals(lines[i].trim())) {
                String name = lines[i + 1].trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        return titleWithoutExtension(entryName, ".SIT");
    }

    static byte[] stripTraxxTail(byte[] bytes) {
        int v1 = indexOf(bytes, TRAXXV1);
        int v2 = indexOf(bytes, TRAXXV2);
        int index = v1 < 0 ? v2 : (v2 < 0 ? v1 : Math.min(v1, v2));
        return index < 0 ? bytes : Arrays.copyOf(bytes, index);
    }

    static String titleWithoutExtension(String entryName, String extension) {
        String name = entryName == null ? "" : entryName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String title = slash >= 0 ? name.substring(slash + 1) : name;
        if (title.toUpperCase(Locale.ROOT).endsWith(extension)) {
            return title.substring(0, title.length() - extension.length());
        }
        return title;
    }

    private static int indexOf(byte[] bytes, byte[] needle) {
        if (needle.length == 0 || bytes.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= bytes.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
