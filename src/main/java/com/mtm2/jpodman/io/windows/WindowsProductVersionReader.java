package com.mtm2.jpodman.io.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.file.Path;
import java.util.Optional;

/** Reads the ProductVersion string from Windows executable version resources. */
public final class WindowsProductVersionReader {
    private WindowsProductVersionReader() {}

    public static Optional<String> readProductVersion(Path executable) {
        String filePath = executable.toAbsolutePath().toString();
        IntByReference dummy = new IntByReference();
        int versionLength = Version.INSTANCE.GetFileVersionInfoSize(filePath, dummy);
        if (versionLength == 0) {
            return Optional.empty();
        }

        Pointer data = new Memory(versionLength);
        if (!Version.INSTANCE.GetFileVersionInfo(filePath, 0, versionLength, data)) {
            return Optional.empty();
        }

        Optional<String> byTranslation = readViaTranslation(data);
        if (byTranslation.isPresent()) {
            return byTranslation;
        }
        return readStringValue(data, "\\StringFileInfo\\040904b0\\ProductVersion");
    }

    private static Optional<String> readViaTranslation(Pointer data) {
        PointerByReference translationPtr = new PointerByReference();
        IntByReference translationLength = new IntByReference();
        if (!Version.INSTANCE.VerQueryValue(data, "\\VarFileInfo\\Translation", translationPtr, translationLength)
                || translationLength.getValue() < 4) {
            return Optional.empty();
        }
        Pointer pointer = translationPtr.getValue();
        int language = pointer.getShort(0) & 0xFFFF;
        int codePage = pointer.getShort(2) & 0xFFFF;
        String block = String.format("\\StringFileInfo\\%04x%04x\\ProductVersion", language, codePage);
        return readStringValue(data, block);
    }

    private static Optional<String> readStringValue(Pointer data, String block) {
        PointerByReference valuePtr = new PointerByReference();
        IntByReference valueLength = new IntByReference();
        if (!Version.INSTANCE.VerQueryValue(data, block, valuePtr, valueLength) || valueLength.getValue() <= 0) {
            return Optional.empty();
        }
        String value = valuePtr.getValue().getWideString(0).trim();
        if (value.isEmpty()) {
            value = valuePtr.getValue().getString(0, Native.getDefaultStringEncoding()).trim();
        }
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
}
