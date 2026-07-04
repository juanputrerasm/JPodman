package com.mtm2.jpodman;

import com.mtm2.jpodman.io.PodDisplayNameResolver;
import com.mtm2.jpodman.io.PodDisplayNameResolver.PodMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PodDisplayNameResolverTest {
    @Test
    void formatsSingleTrack() {
        assertEquals("Laguna.pod [Track:Laguna Seca]",
                PodDisplayNameResolver.displayLabel("Laguna.pod", new PodMetadata(List.of("Laguna Seca"), List.of())));
    }

    @Test
    void formatsMultipleTrucksByNameUpToThree() {
        assertEquals("Arena.pod [Trucks:Bigfoot 15, Carolina Crusher HD]",
                PodDisplayNameResolver.displayLabel("Arena.pod", new PodMetadata(List.of(), List.of("Bigfoot 15", "Carolina Crusher HD"))));
    }

    @Test
    void formatsCountsAfterThree() {
        assertEquals("Tracks9.pod [Tracks:4]",
                PodDisplayNameResolver.displayLabel("Tracks9.pod", new PodMetadata(List.of("A", "B", "C", "D"), List.of())));
    }

    @Test
    void formatsMixedPodsCompactly() {
        assertEquals("Mixed.pod [Track:Miami; Truck:Bigfoot]",
                PodDisplayNameResolver.displayLabel("Mixed.pod", new PodMetadata(List.of("Miami"), List.of("Bigfoot"))));
    }

    @Test
    void formatsSystemPodsBeforeDetectedMetadata() {
        assertEquals("truck2.pod [System; Trucks:4]",
                PodDisplayNameResolver.displayLabel("truck2.pod", new PodMetadata(List.of(), List.of("A", "B", "C", "D")), true));
    }

    @Test
    void formatsMissingPods() {
        assertEquals("Missing1.pod [missing]", PodDisplayNameResolver.missingLabel("Missing1.pod"));
    }
}
