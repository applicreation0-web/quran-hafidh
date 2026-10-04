package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class HifzDisplayVocabularyTest {
    @Test public void schemaFiveSabqiLabelDisplaysAsApprentissage() {
        assertEquals("Apprentissage installé à conserver",
  HifzDisplayVocabulary.canonicalize("Sabqi installé à conserver"));
    }

    @Test public void schemaFiveItqanLabelDisplaysAsStabilisationWithFrenchAgreement() {
        assertEquals("Stabilisation installée à conserver",
  HifzDisplayVocabulary.canonicalize("Itqān installé à conserver"));
    }

    @Test public void schemaFiveMaintenanceLabelDisplaysAsRevisionWithFrenchAgreement() {
        assertEquals("Révision installée à conserver",
  HifzDisplayVocabulary.canonicalize("Entretien installé à conserver"));
    }

    @Test public void olderAnchoringNamesDisplayAsStabilisation() {
        assertEquals("Stabilisation · 49:1–18",
  HifzDisplayVocabulary.canonicalize("Ancrage fractionné · 49:1–18"));
    }

    @Test public void canonicalLabelsRemainUnchanged() {
        assertEquals("Consolidation historique",
  HifzDisplayVocabulary.canonicalize("Consolidation historique"));
        assertEquals("Révision · séance validée",
  HifzDisplayVocabulary.canonicalize("Révision · séance validée"));
    }

    @Test public void nullAndEmptyRemainStable() {
        assertNull(HifzDisplayVocabulary.canonicalize(null));
        assertEquals("", HifzDisplayVocabulary.canonicalize(""));
    }
}
