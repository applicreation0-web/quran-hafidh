package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Provenance-preserving selection, not artificial intelligence or editorial
 * approval. The full source-context audit remains mandatory before debug.
 */
public final class IbnKathirHeadingSelectionTest {
    @Test public void chooseOriginalThematicHeadingWithinTheSameGroup() {
        assertEquals("Allah swears by His Creation that the Person Who Purifies Himself will be Successful",
            IbnKathirTitlePack.preferredSourceHeading(new String[]{
                "Recitation of Surah Ash-Shams in the 'Isha' Prayer",
                "Allah swears by His Creation that the Person Who Purifies Himself will be Successful"
            }));
        assertEquals("The Meaning of Al-Fatihah and its Various Names",
            IbnKathirTitlePack.preferredSourceHeading(new String[]{
                "Introduction to Fatihah", "The Meaning of Al-Fatihah and its Various Names"
            }));
        assertEquals("The Discussion of the Individual Letters",
            IbnKathirTitlePack.preferredSourceHeading(new String[]{
                "The Virtues of Surat Al-Baqarah",
                "Surat Al-Baqarah was revealed in Al-Madinah",
                "The Discussion of the Individual Letters"
            }));
    }

    @Test public void neverFabricateHeadingsWhenOnlyPreambleOrNothingExists() {
        assertEquals("The Virtues of Surat Al-Baqarah",
            IbnKathirTitlePack.preferredSourceHeading(new String[]{
                "The Virtues of Surat Al-Baqarah",
                "Virtues of Surat Al-Baqarah and Surat Aali 'Imran",
                "Surat Al-Baqarah was revealed in Al-Madinah"
            }));
        assertEquals("",IbnKathirTitlePack.preferredSourceHeading(new String[0]));
        assertEquals("",IbnKathirTitlePack.preferredSourceHeading(null));
    }

    @Test public void unrelatedNarrationIsNotTurnedIntoAHeadingByThisSelector() {
        // This test covers selection only. Work MUST still review the dataset
        // for lines that are not editorial headings in the original source.
        assertFalse(IbnKathirTitlePack.isGenericPreamble("The Meaning of Iman"));
        assertTrue(IbnKathirTitlePack.isGenericPreamble("The Recitation of Surat Al-Layl in the Prayer"));
        assertTrue(IbnKathirTitlePack.isGenericPreamble("Surat Al-Baqarah was revealed in Al-Madinah"));
    }
}
