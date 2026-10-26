package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanMaintenanceVocabularyTest {
    @Test public void maintenanceItqanNeverRebrandedAsPassiveRevision() {
        assertEquals("Itqān · Maintien de la maîtrise",HifzDisplayVocabulary.canonicalize("Itqān · Entretien"));
        assertEquals("Itqān · Maintien de la maîtrise",HifzDisplayVocabulary.canonicalize("Entretien · Itqān"));
    }
    @Test public void oldEntretienStillMeansLegacyPassiveRevision() {
        assertEquals("Révision passive",HifzDisplayVocabulary.canonicalize("Entretien"));
        assertEquals("Révision passive installée",HifzDisplayVocabulary.canonicalize("Entretien installé"));
    }
    @Test public void approvedItqanTitlesRemainExactWhenDisplayingHistory() {
        for(String title:new String[]{"Itqān · Maintien de la maîtrise","Itqān · Consolidation"})
            assertEquals(title,HifzDisplayVocabulary.canonicalize(title));
    }
    @Test public void oldFinaleNamesAndSnowballNeverLeakIntoUserLabels() {
        assertEquals("Finale Sabqi",HifzDisplayVocabulary.canonicalize("Finale Sabqi"));
        assertEquals("Finale Itqān",HifzDisplayVocabulary.canonicalize("Finale Itqān"));
        assertEquals("Finale Sabqi",HifzDisplayVocabulary.canonicalize("Finale Renforcement Sabqi"));
        assertEquals("Finale Itqān",HifzDisplayVocabulary.canonicalize("Finale Consolidation Itqān"));
        assertEquals("Renforcement · révision cumulative",
            HifzDisplayVocabulary.canonicalize("Renforcement · boule de neige"));
        assertEquals("Consolidation · Révision cumulative",
            HifzDisplayVocabulary.canonicalize("Consolidation · Boule de neige"));
    }
    @Test public void settingsChoicesAndDatedSummariesUseApprovedTitles() throws Exception {
        java.nio.file.Path path=java.nio.file.Paths.get("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        if(!java.nio.file.Files.exists(path))path=java.nio.file.Paths.get("hifz-app").resolve(path);
        String source=java.nio.file.Files.readString(path);
        String programme=source.substring(source.indexOf("private String itqanProgrammeSummary()"),source.indexOf("private void chooseItqanProgramme()"));
        assertTrue("Physical completion must govern the current programme summary",
            programme.contains("if(prefs.itqanPostNasMaintenance())return \"Itqān · Maintien de la maîtrise · 1 hizb ×3\";"));
        assertFalse("Visible Itqan summaries must not revive Entretien",programme.contains("\"Entretien"));
        assertTrue(source.contains("\"Itqān · Consolidation · protocole approfondi\""));
        assertTrue(source.contains("\"Itqān · Maintien de la maîtrise · 1 hizb ×3\""));
    }
}
