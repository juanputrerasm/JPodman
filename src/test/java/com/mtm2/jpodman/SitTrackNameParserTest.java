package com.mtm2.jpodman;

import com.mtm2.jpodman.io.SitTrackNameParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SitTrackNameParserTest {
    @Test
    void extractsRaceTrackName() {
        byte[] bytes = "foo.lvl\n!Race Track Name\nLaguna Seca\n".getBytes(StandardCharsets.ISO_8859_1);

        assertEquals("Laguna Seca", SitTrackNameParser.displayName(bytes, "WORLD/LAGUNA.SIT"));
    }

    @Test
    void stripsTraxxBinaryTailBeforeParsing() {
        byte[] bytes = "foo.lvl\n!Race Track Name\nMiami 1\nTRAXXV2\0\0".getBytes(StandardCharsets.ISO_8859_1);

        assertEquals("Miami 1", SitTrackNameParser.displayName(bytes, "MIAMI.SIT"));
    }

    @Test
    void fallsBackToEntryTitle() {
        assertEquals("HOMESTEAD", SitTrackNameParser.displayName("no label".getBytes(StandardCharsets.ISO_8859_1), "TRK/HOMESTEAD.SIT"));
    }
}
