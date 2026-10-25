package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanMaintenanceVocabularyTest {
    @Test public void maintenanceItqanNeverRebrandedAsPassiveRevision() {
        assertEquals("Itqān · Entretien",HifzDisplayVocabulary.canonicalize("Itqān · Entretien"));
        assertEquals("Entretien · Itqān",HifzDisplayVocabulary.canonicalize("Entretien · Itqān"));
    }
    @Test public void oldEntretienStillMeansLegacyPassiveRevision() {
        assertEquals("Révision passive",HifzDisplayVocabulary.canonicalize("Entretien"));
        assertEquals("Révision passive installée",HifzDisplayVocabulary.canonicalize("Entretien installé"));
    }
}
