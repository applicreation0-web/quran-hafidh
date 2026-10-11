package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verbatim source title order is preserved; only initial display offset varies. */
public final class IbnKathirQuranComPrivateTitleOrderTest {
    @Test public void qafBeginsWithActualThemeAndKeepsAllFour() {
        String[] headings = {
            "Which was revealed in Makkah",
            "The Beginning of the Mufassal Section of the Qur'an",
            "The Virtues of Surah Qaf",
            "The Disbelievers wonder at the Message and Resurrection"
        };
        int initial=IbnKathirQuranComPrivateTitles.firstThematicIndex(headings);
        assertEquals(3,initial);
        assertEquals("The Disbelievers wonder at the Message and Resurrection",headings[initial]);
        assertEquals("Which was revealed in Makkah",headings[(initial+1)%headings.length]);
    }

    @Test public void baqarahPrefaceDoesNotReplaceOriginalTheme() {
        String[] headings = {
            "Which was revealed in Al-Madina",
            "Virtues of Surat Al-Baqarah",
            "Virtues of Surat Al-Baqarah and Surat Al `Imran",
            "Surat Al-Baqarah was revealed in Al-Madinah",
            "The Discussion of the Individual Letters",
            "The Letters at the Beginning of Surahs"
        };
        assertEquals(4,IbnKathirQuranComPrivateTitles.firstThematicIndex(headings));
        assertEquals("The Discussion of the Individual Letters",headings[4]);
    }

    @Test public void oneOriginalIntroductionRemainsAvailable() {
        String[] headings = {"Which was revealed in Makkah"};
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(headings));
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(new String[0]));
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(null));
    }
}
