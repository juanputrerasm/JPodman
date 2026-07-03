package com.mtm2.jpodman;

/** A POD list item with a stable mount path and a richer UI label. */
public record PodListItem(String mountPath, String displayLabel) {
    public PodListItem {
        mountPath = mountPath == null ? "" : mountPath.trim();
        displayLabel = displayLabel == null || displayLabel.isBlank() ? mountPath : displayLabel.trim();
    }

    public static PodListItem plain(String mountPath) {
        return new PodListItem(mountPath, mountPath);
    }

    @Override
    public String toString() {
        return displayLabel;
    }
}
