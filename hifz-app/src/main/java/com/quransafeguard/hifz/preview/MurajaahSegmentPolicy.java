package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

/**
 * Révision passages (objective label, corpus jumps, dashboard): a verse or two still finishing
 * its own chain between acquired runs of the same surah — typically a verse straddling two
 * Apprentissage blocks — no longer splits the passage into tiny pieces (device report: "petites
 * segmentations sans raison"). Display/navigation only: the eligible corpus itself is unchanged.
 */
final class MurajaahSegmentPolicy {
    /** Largest run of not-yet-eligible verses bridged inside one surah. */
    static final int MAX_BRIDGED_VERSES = 3;

    private MurajaahSegmentPolicy() {}

    static boolean continues(VerseRef previous, VerseRef current) {
        if (previous == null || current == null) return false;
        int gap = GeometryRepository.ordinal(current) - GeometryRepository.ordinal(previous) - 1;
        if (gap == 0) return true;
        return gap > 0 && gap <= MAX_BRIDGED_VERSES && previous.getSurah() == current.getSurah();
    }
}
