package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.List;

/** Pure guard for cyclic Entretien traversals whose endpoint occurrence cannot be disambiguated. */
final class MurajaahTraversalPolicy {
    private MurajaahTraversalPolicy() {}

    static boolean endpointAmbiguous(List<VerseRef> traversal, VerseRef endpoint) {
        if (traversal == null || endpoint == null) return false;
        int occurrences = 0;
        for (VerseRef verse : traversal) {
            if (endpoint.equals(verse) && ++occurrences > 1) return true;
        }
        return false;
    }
}
