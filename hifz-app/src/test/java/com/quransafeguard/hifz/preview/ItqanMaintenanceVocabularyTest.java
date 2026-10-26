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
    @Test public void settingsFollowPhysicalAnNasWithoutAFalseDatedSwitch() throws Exception {
        java.nio.file.Path path=java.nio.file.Paths.get("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        if(!java.nio.file.Files.exists(path))path=java.nio.file.Paths.get("hifz-app").resolve(path);
        String source=java.nio.file.Files.readString(path);
        assertTrue("BOOX must identify the automatic physical regime change",
            source.contains("Bascule automatique ; séances déjà ouvertes préservées."));
        assertFalse("No ineffective user-scheduled Itqan change",source.contains("chooseItqanProgramme()"));
        assertFalse("Do not display a programme date picker",source.contains("Choisir la date"));
        String regime=java.nio.file.Files.readString(path.resolveSibling("ItqanRegimeStore.java"));
        assertTrue(regime.contains("return ItqanMaintenancePolicy.regimeFor(maintenanceForNewSession(p));"));
        assertTrue(regime.contains("if (stamped != null) return stamped;"));
    }
    @Test public void oldSnowballTermsAreOnlyCompatibilityInputNotVisibleSubtitles() throws Exception {
        assertEquals("Renforcement · révision cumulative",
            HifzDisplayVocabulary.canonicalize("Renforcement · boule de neige"));
        assertEquals("Consolidation · Révision cumulative",
            HifzDisplayVocabulary.canonicalize("Consolidation · Boule de neige"));
        String dir="src/main/java/com/quransafeguard/hifz/preview/";
        for(String f:new String[]{"MainActivity.java","HifzSessionActivity.java",
                "WeeklyDashboardPlanner.java","SettingsActivity.java","DashboardLedger.java"}){
            java.nio.file.Path file=java.nio.file.Paths.get(dir+f);
            if(!java.nio.file.Files.exists(file))
                file=java.nio.file.Paths.get("hifz-app").resolve(file);
            String src=java.nio.file.Files.readString(file);
            assertFalse("Old visible capitalized wording in "+f,
                src.contains("\\\"Boule de neige"));
            assertFalse("Old visible lower-case wording in "+f,
                src.contains("\\\"boule de neige"));
        }
    }

}
