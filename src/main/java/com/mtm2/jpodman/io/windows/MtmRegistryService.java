package com.mtm2.jpodman.io.windows;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Windows-only MTM registry reader/reset service. */
public final class MtmRegistryService {
    private static final int REGISTRY_VIEW_32 = WinNT.KEY_WOW64_32KEY;

    public boolean isWindows() {
        return Platform.isWindows();
    }

    public RegistrySnapshot readSnapshot(MtmRegistryProfile profile) {
        List<RegistrySnapshot.KeyValues> groups = new ArrayList<>();
        for (RegistryWrite write : registryWrites(profile, Path.of(""), Path.of("monster.exe"))) {
            List<RegistrySnapshot.Value> values = new ArrayList<>();
            for (String name : write.values().keySet()) {
                values.add(new RegistrySnapshot.Value(name, readString(write.keyPath(), name)));
            }
            groups.add(new RegistrySnapshot.KeyValues(write.title(), write.keyPath(), values));
        }
        return new RegistrySnapshot(groups);
    }

    public void applyProfile(MtmRegistryProfile profile, Path gameRoot, Path executable) {
        requireWindows();
        for (RegistryWrite write : registryWrites(profile, gameRoot, executable)) {
            createKeyPath(write.keyPath());
            for (Map.Entry<String, String> value : write.values().entrySet()) {
                Advapi32Util.registrySetStringValue(
                        WinReg.HKEY_LOCAL_MACHINE,
                        write.keyPath(),
                        value.getKey(),
                        value.getValue(),
                        REGISTRY_VIEW_32);
            }
        }
    }

    public static List<RegistryWrite> registryWrites(MtmRegistryProfile profile, Path gameRoot, Path executable) {
        Path root = gameRoot == null ? Path.of("") : gameRoot;
        String rootText = windowsPath(root);
        Path exe = executable == null ? root.resolve("monster.exe") : executable;
        String exePath = windowsPath(exe);
        String exeName = exe.getFileName() == null ? "monster.exe" : exe.getFileName().toString();

        return switch (profile) {
            case MTM1 -> List.of(
                    new RegistryWrite("Program", "SOFTWARE\\Microsoft\\Monster Truck Madness\\1.0", orderedMap(
                            "Path", exePath,
                            "InstalledTo", rootText,
                            "Version", "1.0",
                            "ProductID", "52919-450-0000000-00000",
                            "Name", "DriverX",
                            "Organization", " ")),
                    new RegistryWrite("Uninstall", "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Monster Truck Madness", orderedMap(
                            "DisplayName", "Microsoft Monster Truck Madness",
                            "UninstallString", rootText + "\\setup\\setup.exe /Z customui.dll")));
            case MTM2 -> List.of(
                    new RegistryWrite("Program", "SOFTWARE\\Microsoft\\Microsoft Games\\Monster Truck Madness\\2.0", orderedMap(
                            "Version", "2.0",
                            "Launched", "1",
                            "InstalledPath", rootText,
                            "VersionType", "RetailVersion",
                            "InstalledGroup", "1",
                            "ProductID", "83622-442-0000000-00000",
                            "PID", "83622-442-0000000-00000")),
                    new RegistryWrite("Uninstall", "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Monster Truck Madness 2.0", orderedMap(
                            "DisplayName", "Microsoft Monster Truck Madness 2",
                            "UninstallString", rootText + "\\UNINSTAL.EXE")),
                    new RegistryWrite("DirectPlay", "SOFTWARE\\Microsoft\\DirectPlay\\Applications\\Monster Truck Madness 2", orderedMap(
                            "Guid", "{6cd1c6e0-96fb-11d1-a268-00a02f29c995}",
                            "File", exeName,
                            "CommandLine", " ",
                            "CurrentDirectory", rootText,
                            "Path", rootText)));
            case TRIAL -> List.of(
                    new RegistryWrite("Program", "SOFTWARE\\Microsoft\\Microsoft Games\\Monster Truck Madness\\2.00Trial", orderedMap(
                            "InstallType", "1",
                            "InstallationDirectory", rootText,
                            "Launched", "1",
                            "PID", " ",
                            "Path", rootText,
                            "VersionType", "TrialVersion",
                            "InstalledGroup", "1")),
                    new RegistryWrite("Uninstall", "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Monster Truck Madness 2.00Trial", orderedMap(
                            "DisplayName", "Microsoft Monster Truck Madness 2 Trial",
                            "UninstallString", rootText + "\\UNINSTAL.EXE")),
                    new RegistryWrite("DirectPlay", "SOFTWARE\\Microsoft\\DirectPlay\\Applications\\Monster Truck Madness 2 Trial", orderedMap(
                            "Guid", "{6cd1c6e0-96fb-11d1-a268-00a02f29c996}",
                            "File", exeName,
                            "CommandLine", " ",
                            "CurrentDirectory", rootText,
                            "Path", rootText)));
        };
    }

    public static String windowsPath(Path path) {
        return path == null ? "" : path.toString().replace('/', '\\');
    }

    private String readString(String keyPath, String valueName) {
        if (!isWindows()) {
            return "(Windows only)";
        }
        try {
            return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, keyPath, valueName, REGISTRY_VIEW_32);
        } catch (Win32Exception ex) {
            return "(not set)";
        }
    }

    private void createKeyPath(String keyPath) {
        String[] parts = keyPath.split("\\\\");
        String current = "";
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (current.isEmpty()) {
                Advapi32Util.registryCreateKey(WinReg.HKEY_LOCAL_MACHINE, part, REGISTRY_VIEW_32);
                current = part;
            } else {
                Advapi32Util.registryCreateKey(WinReg.HKEY_LOCAL_MACHINE, current, part, REGISTRY_VIEW_32);
                current += "\\" + part;
            }
        }
    }

    private void requireWindows() {
        if (!isWindows()) {
            throw new IllegalStateException("Registry reset is only available on Windows.");
        }
    }

    private static Map<String, String> orderedMap(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key/value arguments must be paired.");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    public record RegistryWrite(String title, String keyPath, Map<String, String> values) {
        public RegistryWrite {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }
}
