package com.mtm2.jpodman.io.windows;

import java.util.List;

/** Read-only view of MTM registry values for display. */
public record RegistrySnapshot(List<KeyValues> groups) {
    public RegistrySnapshot {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public record KeyValues(String title, String keyPath, List<Value> values) {
        public KeyValues {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    public record Value(String name, String data) {}
}
