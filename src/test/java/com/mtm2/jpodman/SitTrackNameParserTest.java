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
    void readsEvoSixAndSevenTrackNames() {
        assertEquals("Silverton Pass", SitTrackNameParser.displayName(
                evoSit("6", "Race Track Name", "Silverton Pass"), "TRACK.SIT"));
        assertEquals("Pikes Peak", SitTrackNameParser.displayName(
                evoSit("7", "#Race Track Name", "Pikes Peak"), "TRACK.SIT"));
        assertEquals("Truck Stop 101", SitTrackNameParser.displayName(
                evoSit("7", "&Race Track Name", "Truck Stop 101"), "TRACK.SIT"));
    }

    @Test
    void readsCommunityPatch3Si2TrackName() {
        byte[] bytes = "foo.lvl\r\n!Race Track Name\r\nHigh Resolution\r\n".getBytes(StandardCharsets.ISO_8859_1);

        assertEquals("High Resolution", SitTrackNameParser.displayName(bytes, "WORLD/HIGHRES.SI2"));
    }

    @Test
    void fallsBackToTheSi2EntryTitleWithoutExtension() {
        assertEquals("HIGHRES", SitTrackNameParser.displayName("no label".getBytes(StandardCharsets.ISO_8859_1), "WORLD/HIGHRES.si2"));
    }

    private static byte[] evoSit(String version, String label, String trackName) {
        return ("version\r\n" + version + "\r\nTRACK.LVL\r\n" + label + "\r\n" + trackName + "\r\n*** Boxes ***\r\n")
                .getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void fallsBackToEntryTitle() {
        assertEquals("HOMESTEAD", SitTrackNameParser.displayName("no label".getBytes(StandardCharsets.ISO_8859_1), "TRK/HOMESTEAD.SIT"));
    }
}
