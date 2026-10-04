package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Remaining device-level cases from Claude's second 0.7.5 re-audit patch. */
public final class ClaudeRound2ExtendedInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void reconcileNeverQueuesOwnerlessPagePortion() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit()
            .putString("promotedRanges", "[{\"start\":\"53:2\",\"end\":\"53:2\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"53:2\",\"end\":\"53:2\"}]")
            .putString("anchoringQueue", "[]")
            .putBoolean("anchoringQueueInitialized", false)
            .commit());
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        assertTrue(prefs.anchoringQueue().isEmpty());
    }

    @Test public void manualStabilizationRangeWithoutOwnedLineIsRejected() {
        HifzPrefs prefs = new HifzPrefs(context);
        String before = main.getString("unconsolidatedPromotedRanges", "");
        try {
            prefs.setV6StabilizationRanges(Collections.singletonList(
                new VerseRange(new VerseRef(53, 2), new VerseRef(53, 2))), geometry);
            fail("ownerless Stabilisation range must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("526"));
        }
        assertEquals(before, main.getString("unconsolidatedPromotedRanges", ""));
        assertTrue(prefs.setV6StabilizationRanges(Collections.singletonList(
            new VerseRange(new VerseRef(53, 1), new VerseRef(53, 62))), geometry));
    }

    @Test public void openUnitAcrossTwoSurahRangesDoesNotBlockSettingsEdit() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit()
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"80:41\",\"end\":\"80:42\"},{\"start\":\"81:1\",\"end\":\"81:29\"}]")
            .putInt("itqanRep", 1)
            .putString("itqanUnitStart", "80:41")
            .putString("itqanUnitEnd", "81:29")
            .commit());
        assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(
            new VerseRange(new VerseRef(2, 1), new VerseRef(2, 74))), geometry));
        assertEquals(2, prefs.unconsolidatedPromotedRanges().size());
    }
}
