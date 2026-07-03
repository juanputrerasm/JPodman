package com.mtm2.jpodman;

import com.mtm2.jpodman.io.TrkTruckNameParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrkTruckNameParserTest {
    @Test
    void extractsMtm1TruckNameLabel() {
        assertEquals("Bigfoot 15", TrkTruckNameParser.displayName(bytes("truckName\nBigfoot 15\n"), "TRUCK/BIGFOOT.TRK"));
    }

    @Test
    void extractsMtm2TruckNameLabel() {
        assertEquals("Carolina Crusher HD", TrkTruckNameParser.displayName(bytes("MTM2 truckName\nCarolina Crusher HD\n"), "TRUCK/CC.TRK"));
    }

    @Test
    void extractsMtm2HeaderPositionalName() {
        assertEquals("Snake Bite", TrkTruckNameParser.displayName(bytes("MTM2.1\nSnake Bite\ntruckModelBaseName\n"), "TRUCK/SNAKE.TRK"));
    }

    @Test
    void fallsBackToEntryTitle() {
        assertEquals("UNKNOWN", TrkTruckNameParser.displayName(bytes("truckName\n"), "TRUCK/UNKNOWN.TRK"));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
