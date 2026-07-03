package com.mtm2.jpodman;

import com.mtm2.jpodman.io.windows.MtmRegistryProfile;
import com.mtm2.jpodman.io.windows.MtmRegistryService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtmRegistryServiceTest {
    @Test
    void mtm2ProfileContainsProgramUninstallAndDirectPlayKeys() {
        List<MtmRegistryService.RegistryWrite> writes = MtmRegistryService.registryWrites(
                MtmRegistryProfile.MTM2,
                Path.of("C:/Games/MTM2"),
                Path.of("C:/Games/MTM2/monster.exe"));

        assertEquals(3, writes.size());
        assertTrue(writes.stream().anyMatch(write -> write.keyPath().equals("SOFTWARE\\Microsoft\\Microsoft Games\\Monster Truck Madness\\2.0")));
        assertTrue(writes.stream().anyMatch(write -> write.keyPath().equals("SOFTWARE\\Microsoft\\DirectPlay\\Applications\\Monster Truck Madness 2")));
        assertTrue(writes.stream().anyMatch(write -> "Microsoft Monster Truck Madness 2".equals(write.values().get("DisplayName"))));
    }

    @Test
    void mtm1ProfileHasNoDirectPlayKey() {
        List<MtmRegistryService.RegistryWrite> writes = MtmRegistryService.registryWrites(
                MtmRegistryProfile.MTM1,
                Path.of("C:/Games/MTM1"),
                Path.of("C:/Games/MTM1/monster.exe"));

        assertEquals(2, writes.size());
        assertTrue(writes.stream().noneMatch(write -> write.title().equals("DirectPlay")));
        assertTrue(writes.stream().anyMatch(write -> "C:\\Games\\MTM1\\monster.exe".equals(write.values().get("Path"))));
    }

    @Test
    void trialProfileUsesTrialGuid() {
        List<MtmRegistryService.RegistryWrite> writes = MtmRegistryService.registryWrites(
                MtmRegistryProfile.TRIAL,
                Path.of("C:/Games/Trial"),
                Path.of("C:/Games/Trial/monsterx.exe"));

        assertTrue(writes.stream().anyMatch(write -> write.values().containsValue("{6cd1c6e0-96fb-11d1-a268-00a02f29c996}")));
        assertTrue(writes.stream().anyMatch(write -> write.values().containsValue("monsterx.exe")));
    }
}
