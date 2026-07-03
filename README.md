# JPodman

JPodman is a Java 17 Swing tool for managing Monster Truck Madness 1 & 2 `pod.ini` POD file mount lists.

## Features

- Dual-list POD manager for moving POD files between mounted and available lists.
- Reads and writes TRI `pod.ini` files.
- Creates a new `pod.ini` when one does not already exist.
- Discovers `.pod` files in the game folder and configured extra POD folders.
- Configurable active POD limit, defaulting to 99, with preset buttons for MTM1, MTM2 trial/retail, MTM2 patched, and community patch setups.
- POD metadata labels using POD parsing: tracks are detected from `.SIT` files and trucks from `.TRK` files.
- Displays track metadata in light blue and truck metadata in light red.
- Exports the current mounted POD list without modifying the game `pod.ini`.
- Optional save-and-launch workflow for `monster.exe` (retail) or `monsterx.exe` (trial).
- Preferences dialog with JSON storage in the user's OS-specific config folder.
- Windows-only registry info and reset tools using JNA-based registry access.
- JNA registry support targets the 32-bit Windows registry view used by MTM1 & 2.

## Build And Run

```sh
mvn test
mvn package
java -jar target/jpodman.jar
```

## Behavior

- The app starts even when the selected folder has no `monster.exe`, `monsterx.exe`, or `pod.ini`.
- Missing `pod.ini` loads as an empty mount list. Saving creates `pod.ini` if the folder is writable.
- If saving fails, JPodman warns that no data was persisted and keeps the in-memory list intact.
- The standard active POD limit is 99. Change it from `Tools > Preferences`.
- The limit is not derived from `monster.exe` file size.
- `File > Export POD List` writes a readable text export without changing the game `pod.ini`.

## Preferences

Preferences are stored as JSON in the user's config folder:

- macOS: `~/Library/Application Support/JPodman/preferences.json`
- Windows: `%APPDATA%/JPodman/preferences.json`
- Linux/Unix: `$XDG_CONFIG_HOME/JPodman/preferences.json` or `~/.config/JPodman/preferences.json`

The preferences file stores the POD limit, extra POD folders, folder scan depth, sort mode, always-on-top mode, and view mode.

---

## License

This project is licensed under the Apache License 2.0. See [LICENSE](./LICENSE).