package com.mtm2.jpodman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterVersionInfoTest {
    @Test
    void classifiesMtm1ProductVersions() {
        MonsterVersionInfo info = MonsterVersionInfo.fromProductVersion("1.0.0.0").orElseThrow();

        assertEquals("MTM1", info.label());
        assertEquals(15, info.suggestedPodLimit());
    }

    @Test
    void classifiesMtm2BetaBelowRetailVersion() {
        MonsterVersionInfo info = MonsterVersionInfo.fromProductVersion("2.0.40.0").orElseThrow();

        assertEquals("MTM2 beta", info.label());
        assertEquals(30, info.suggestedPodLimit());
    }

    @Test
    void classifiesMtm2RetailOrTrialVersion() {
        MonsterVersionInfo info = MonsterVersionInfo.fromProductVersion("2.0.41.0").orElseThrow();

        assertEquals("MTM2 retail/trial", info.label());
        assertEquals(30, info.suggestedPodLimit());
    }

    @Test
    void classifiesMtm2PatchedVersion() {
        MonsterVersionInfo info = MonsterVersionInfo.fromProductVersion("2.0.42.0").orElseThrow();

        assertEquals("MTM2 patched", info.label());
        assertEquals(99, info.suggestedPodLimit());
    }

    @Test
    void classifiesCommunityPatchAbovePatchedVersion() {
        MonsterVersionInfo info = MonsterVersionInfo.fromProductVersion("2.0.43.0").orElseThrow();

        assertEquals("Community patch", info.label());
        assertEquals(199, info.suggestedPodLimit());
    }

    @Test
    void ignoresUnparseableProductVersions() {
        assertTrue(MonsterVersionInfo.fromProductVersion("not-a-version").isEmpty());
    }
}
