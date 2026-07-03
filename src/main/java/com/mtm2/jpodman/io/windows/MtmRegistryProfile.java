package com.mtm2.jpodman.io.windows;

/** Cowpod-compatible MTM registry reset profiles. */
public enum MtmRegistryProfile {
    MTM1("MTM1"),
    MTM2("MTM2"),
    TRIAL("MTM2 Demo");

    private final String displayName;

    MtmRegistryProfile(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
