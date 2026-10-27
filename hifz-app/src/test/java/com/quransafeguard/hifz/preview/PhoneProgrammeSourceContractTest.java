package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public final class PhoneProgrammeSourceContractTest {
    private static String source(String name) throws Exception {
        return Files.readString(Paths.get("src/main/java/com/quransafeguard/hifz/preview/"+name),StandardCharsets.UTF_8);
    }
    @Test public void continueAlwaysPreviewsAndWeekHasClearTitles() throws Exception {
        String main=source("MainActivity.java");
        assertTrue(main.contains("VOIR LES SÉANCES  ›"));
        assertTrue(main.contains("showTodayWindow(sessions);"));
        assertTrue(main.contains("if(sessions.isEmpty()){"));
        assertFalse(main.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }"));
        assertTrue(main.contains("readableScheduleTitle(chosen.morning"));
        assertTrue(main.contains("readableScheduleEssentials(chosen.evening)"));
    }
    @Test public void noFakeAcquiredRangeInFreshProfiles() throws Exception {
        String prefs=source("HifzPrefs.java");
        assertTrue(prefs.contains(".putString(\"itqanRanges\", \"[]\")"));
        assertTrue(prefs.contains("return parseRangesAllowEmpty(\"itqanRanges\");"));
        assertTrue(prefs.contains("p.edit().putBoolean(\"v6AcquiredBootstrapSeeded\", true).commit()"));
        String settings=source("SettingsActivity.java");
        assertTrue(settings.contains("Acquis déterminés par les validations réelles"));
        assertFalse(settings.contains("corpus.addView(acquiredRangesBox)"));
    }
    @Test public void programmeChoiceMustBeForwardDatedWithoutAlteredOpenPlan() throws Exception {
        String prefs=source("HifzPrefs.java"),regime=source("ItqanRegimeStore.java");
        assertTrue(prefs.contains("scheduleItqanProgramme(String programme,LocalDate from)"));
        assertTrue(prefs.contains("from.isBefore(HifzClock.today())"));
        assertTrue(regime.contains("if(\"ENTRETIEN\".equals(programme))return reached;"));
        assertTrue(regime.contains("if(\"CONSOLIDATION\".equals(programme))return reached;"));
        assertTrue(regime.contains("if (stamped != null) return stamped;"));
        assertTrue(source("SettingsActivity.java").contains("Bascule automatique ; séances déjà ouvertes préservées."));
        assertFalse("No misleading dated Itqan switch remains",
            source("SettingsActivity.java").contains("new android.app.DatePickerDialog(this,"));
    }
}
