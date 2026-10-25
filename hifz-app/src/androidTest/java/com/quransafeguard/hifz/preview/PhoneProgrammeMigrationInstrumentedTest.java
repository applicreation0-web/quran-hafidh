package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDate;

import static org.junit.Assert.*;

/** Real SharedPreferences proof for user-selected 1.17.5 phone programmes. */
public final class PhoneProgrammeMigrationInstrumentedTest {
    private Context context;
    private SharedPreferences raw;
    @Before public void before(){
        context=ApplicationProvider.getApplicationContext();
        raw=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        raw.edit().clear().commit();
    }
    @After public void after(){raw.edit().clear().commit();}

    @Test public void freshPhoneProfileDoesNotManufactureAcquisFromAlBaqara() {
        HifzPrefs prefs=new HifzPrefs(context);
        assertTrue(prefs.itqanRanges().isEmpty());
        assertEquals(0,prefs.acquiredLineCountV6());
        assertTrue(prefs.reconcileV6AcquiredBootstrap(GeometryRepository.get(context)));
        assertEquals(0,new HifzPrefs(context).acquiredLineCountV6());
    }

    @Test public void existingAcquisAreUnchangedByForwardDatedProgrammeChoice() {
        HifzPrefs prefs=new HifzPrefs(context);
        String line=GeometryRepository.get(context).line(1).id;
        prefs.transitionV6Lines(java.util.Arrays.asList(line),HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        prefs.transitionV6Lines(java.util.Arrays.asList(line),HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
        prefs.transitionV6Lines(java.util.Arrays.asList(line),HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
        String before=raw.getString("v6AcquiredCreditLineIds","");
        assertTrue(prefs.scheduleItqanProgramme("ENTRETIEN",HifzClock.today().plusDays(3)));
        assertEquals(before,raw.getString("v6AcquiredCreditLineIds",""));
        assertEquals("ENTRETIEN",prefs.itqanProgrammeChoice());
        assertEquals(HifzClock.today().plusDays(3),prefs.itqanProgrammeStartDate());
        assertEquals(before,raw.getString("v6AcquiredCreditLineIds",""));
    }

    @Test public void deepAndMaintenanceCanBeSelectedOnlyForNewUnstampedUnits() {
        HifzPrefs prefs=new HifzPrefs(context);
        assertFalse(prefs.itqanEntretienActive());
        // A date cannot pretend that the physical An-Nās latch was reached.
        assertTrue(prefs.scheduleItqanProgramme("ENTRETIEN",HifzClock.today()));
        assertFalse(prefs.itqanEntretienActive());
        // Simulate an actual, persisted tail-finished milestone for the test.
        assertTrue(raw.edit().putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,true).commit());
        assertTrue(prefs.itqanEntretienActive());
        assertTrue(prefs.scheduleItqanProgramme("CONSOLIDATION",HifzClock.today()));
        assertFalse(prefs.itqanEntretienActive());
        assertTrue(prefs.scheduleItqanProgramme("ENTRETIEN",HifzClock.today()));
        assertTrue(prefs.itqanEntretienActive());
        // No change to the physical completion latch.
        assertTrue(raw.getBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,false));
        assertFalse(raw.contains("itqanUnitPlanV1"));
    }

    @Test public void phoneTodayPreviewExcludesTechnicalCoordinatesAndNeverOpensMushaf() {
        String raw="Stabilisation · séance 1/4 · 7 lignes · 1 bloc · p. 515 l. 9–15 · 4 ← 1 الحجرات · ×40 · ✓ Finale Renforcement Sabqi";
        assertEquals("Itqān · Consolidation",MainActivity.readableScheduleTitle(raw,false));
        assertEquals("Itqān · Entretien",MainActivity.readableScheduleTitle(raw,true));
        String compact=MainActivity.readableScheduleEssentials(raw);
        assertTrue(compact.contains("7 lignes"));
        assertTrue(compact.contains("séance 1/4"));
        assertFalse(compact.contains("p. 515"));
        assertFalse(compact.contains("الحجرات"));
    }
}
