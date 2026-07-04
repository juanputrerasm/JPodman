package com.mtm2.jpodman;

import com.mtm2.jpodman.io.MonsterIniService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterIniServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void readsMonsterIniSettings() throws IOException {
        Path ini = tempDir.resolve("monster.ini");
        Files.writeString(ini, """
                [Fonts]
                gOSFontUseGlobal=1
                gOSFontUse_SmallLCD=0
                gOSFontName_SmallLCD=Modern,Modern,Modern
                gOSFontSize_SmallLCD=-9,-9,-11
                gLocalizationFile=UI\\CUSTOM.loc

                [Game]
                kookyHornFlag=0
                latencyDisplay=1
                showHiddenTrack=666
                podLimit=199
                """);

        MonsterIniSettings settings = MonsterIniService.read(ini);

        assertTrue(settings.useGlobalFonts());
        assertEquals("UI\\CUSTOM.loc", settings.localizationFile());
        assertEquals("Modern,Modern,Modern", settings.fonts().get(0).fontName());
        assertEquals("-9,-9,-11", settings.fonts().get(0).fontSize());
        assertEquals(false, settings.extraHorn());
        assertTrue(settings.numericLatency());
        assertTrue(settings.hiddenTrack());
    }

    @Test
    void writesSupportedKeysAndPreservesUnknownKeys() throws IOException {
        Path ini = tempDir.resolve("monster.ini");
        Files.writeString(ini, """
                ; keep this comment
                [Fonts]
                gOSFontUseGlobal=0
                gLocalizationFile=UI\\OLD.loc

                [Game]
                podLimit=199
                latencyDisplay=2
                """);
        MonsterIniSettings settings = new MonsterIniSettings(
                true,
                MonsterIniSettings.defaultFonts(),
                "UI\\MTM2-PIG.loc",
                true,
                true,
                true);

        MonsterIniService.write(ini, settings);
        String written = Files.readString(ini);

        assertTrue(written.contains("; keep this comment"));
        assertTrue(written.contains("podLimit=199"));
        assertTrue(written.contains("gLocalizationFile=UI\\MTM2-PIG.loc"));
        assertTrue(written.contains("latencyDisplay=1"));
        assertTrue(written.contains("showHiddenTrack=666"));
    }
}
