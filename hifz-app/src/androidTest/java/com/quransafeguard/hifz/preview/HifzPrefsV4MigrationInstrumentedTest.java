package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class HifzPrefsV4MigrationInstrumentedTest {
    private static final String NAME = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences raw;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        raw = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
    }

    @After public void tearDown() {
        raw.edit().clear().commit();
    }

    @Test public void freshSchemaFourBootstrapsTailAsPendingReconstruction() {
        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals(1, prefs.itqanRanges().size());
        assertTrue(prefs.itqanWorkCorpus().contains(new VerseRef(49, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
        assertTrue(prefs.murajaahCorpus().contains(new VerseRef(2, 74)));
        assertEquals(new VerseRef(49, 1), prefs.itqanCursor());
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
        assertTrue(prefs.consolidationAttendanceDates().isEmpty());
        assertTrue(prefs.forcedPromotedRanges().isEmpty());
        AnchoringQueue.Entry first = prefs.currentAnchoringEntry(GeometryRepository.get(context));
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, first.origin);
        assertEquals(AnchoringQueue.ItqanProtocol.LIGHT, first.protocol);
    }

    @Test public void alreadyV4InstallPurgesObsoleteStableRecentLinesAndRepairsNewOptionalKeys() {
        raw.edit()
            .putInt("schema", 4)
            .putString("stableRecentLines", "[{\"start\":10,\"end\":14}]")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertFalse(raw.contains("stableRecentLines"));
        assertTrue(raw.contains("consolidationAttendanceDates"));
        assertTrue(raw.contains("forcedPromotedRanges"));
        assertTrue(prefs.consolidationAttendanceDates().isEmpty());
        assertTrue(prefs.forcedPromotedRanges().isEmpty());
    }

    @Test public void validatedAnchoringPageJoinsMaintenanceWithoutTeleportingItsCursor() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry first = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(first.start);
        VerseRef end = GeometryRepository.parseVerse(first.end);
        VerseRef naturalMaintenanceCursor = prefs.murajaahCursor();

        assertFalse(prefs.murajaahCorpus().contains(start));
        assertTrue(prefs.completeItqanUnitAndConsolidate(start, end,
            prefs.itqanWorkCorpus().next(end), "2026-09-13", "validation test"));

        assertTrue(prefs.murajaahCorpus().contains(start));
        assertEquals(naturalMaintenanceCursor, prefs.murajaahCursor());
    }

    @Test public void consolidationAttendanceIsPersistedAtCompletionWithoutDashboardNavigation() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.completeRecentSabqiReview("2026-01-04", 0, "Consolidation"));
        assertTrue(prefs.completeRecentSabqiReview("2026-01-04", 0, "Consolidation reprise"));
        assertTrue(prefs.completeRecentSabqiReview("2026-01-11", 0, "Consolidation"));

        assertEquals(2, prefs.consolidationAttendanceDates().size());
        assertEquals(LocalDate.of(2026, 1, 4), prefs.consolidationAttendanceDates().get(0));
        assertEquals(LocalDate.of(2026, 1, 11), prefs.consolidationAttendanceDates().get(1));
    }

    @Test public void forcedPromotionSurvivesQueueReconciliationAsDistinctOrigin() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        VerseRef verse = new VerseRef(2, 75);

        assertTrue(prefs.addPromotedVerses(Collections.singletonList(verse), true));
        assertTrue(prefs.reconcileAnchoringQueue(geometry));

        boolean found = false;
        for (AnchoringQueue.Entry entry : prefs.anchoringQueue()) {
            VerseRef start = GeometryRepository.parseVerse(entry.start);
            VerseRef end = GeometryRepository.parseVerse(entry.end);
            if (GeometryRepository.ordinal(verse) >= GeometryRepository.ordinal(start)
                    && GeometryRepository.ordinal(verse) <= GeometryRepository.ordinal(end)) {
                assertEquals(AnchoringQueue.Origin.FORCED_PROMOTION, entry.origin);
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test public void promotionInvalidatesAnchoringCacheAndNextReadReconcilesItOnce() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        assertFalse(raw.getBoolean("anchoringQueueInitialized", false));
        assertTrue(prefs.currentAnchoringEntry(geometry) != null);
        assertTrue(raw.getBoolean("anchoringQueueInitialized", false));

        assertTrue(prefs.addPromotedVerses(Collections.singletonList(new VerseRef(2, 75)), false));
        assertFalse(raw.getBoolean("anchoringQueueInitialized", true));
        assertTrue(prefs.currentAnchoringEntry(geometry) != null);
        assertTrue(raw.getBoolean("anchoringQueueInitialized", false));
    }

    @Test public void failedAnchoringAtomicallyClearsElapsedAndSinglePageDoesNotImmediateLoop() {
        HifzPrefs prefs = new HifzPrefs(context);
        String one = "[{\"start\":\"49:1\",\"end\":\"49:5\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]";
        raw.edit()
            .putString("anchoringQueue", one)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putLong("itqanElapsedMs", 55_000L)
            .commit();

        assertTrue(prefs.failAndDeferAnchoring(new VerseRef(49, 1), new VerseRef(49, 5)));
        assertEquals(0L, prefs.elapsedFor("ITQAN"));
        assertNull(prefs.currentAnchoringEntry(GeometryRepository.get(context)));
    }

    @Test public void completedAnchoringEntryMissingFromQueueFailsClosed() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        prefs.currentAnchoringEntry(geometry);
        int before = prefs.anchoringQueue().size();

        assertFalse(prefs.completeItqanUnitAndConsolidate(
            new VerseRef(2, 1), new VerseRef(2, 1), new VerseRef(2, 2),
            "2026-09-13", "must fail"));
        assertEquals(before, prefs.anchoringQueue().size());
    }

    @Test public void schemaThreeMigrationIsAtomicAndPreservesUnrelatedProgress() {
        raw.edit()
            .putInt("schema", 3)
            .putString("programStartDate", "2026-09-01")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"},{\"start\":\"49:1\",\"end\":\"114:6\"}]")
            .putString("promotedRanges", "[]")
            .putString("unconsolidatedPromotedRanges", "[]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "49:1")
            .putString("lastSabqiLabel", "progression à conserver")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals("progression à conserver", prefs.lastSabqiLabel());
        assertEquals(1, prefs.itqanRanges().size());
        assertTrue(prefs.isUnconsolidatedPromoted(new VerseRef(49, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
        assertTrue(prefs.consolidationAttendanceDates().isEmpty());
    }

    @Test public void schemaOneMigratesThroughV2V3ToV4WithoutLosingProgress() {
        raw.edit()
            .putInt("schema", 1)
            .putString("programStartDate", "2026-09-01")
            .putString("promotedFrontier", "2:150")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRotationStart", "49:1")
            .putString("lastSabqiLabel", "progression à conserver")
            .putString("murajaahPhase", "A")
            .putInt("murajaahRecentLinesDone", 10)
            .putLong("murajaahBlockAElapsedMs", 120_000L)
            .putLong("murajaahBlockBElapsedMs", 240_000L)
            .putString("recentSabqi", "[{\"start\":10,\"end\":14}]")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertTrue(prefs.isUnconsolidatedPromoted(new VerseRef(2, 100)));
        assertTrue(prefs.murajaahCorpus().contains(new VerseRef(2, 100)));
        assertTrue(prefs.itqanWorkCorpus().contains(new VerseRef(49, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
        assertEquals(1, prefs.itqanRanges().size());
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
        assertEquals("progression à conserver", prefs.lastSabqiLabel());
        assertFalse(raw.contains("murajaahPhase"));
        assertFalse(raw.contains("murajaahRecentLinesDone"));
        assertFalse(raw.contains("murajaahBlockAElapsedMs"));
        assertFalse(raw.contains("murajaahBlockBElapsedMs"));
        assertEquals(120_000L, raw.getLong("recent_sabqi_reviewElapsedMs", -1L));
        assertEquals(240_000L, raw.getLong("murajaahElapsedMs", -1L));
        assertEquals(2, raw.getInt("recentSabqiReviewIndex", -1));
        assertTrue(raw.contains("consolidationAttendanceDates"));
        assertTrue(raw.contains("forcedPromotedRanges"));
        assertTrue(raw.contains("hardAnchoringSurahs"));
        assertTrue(raw.contains("anchoringRetryAfterDate"));
        assertEquals(0, prefs.itqanBlockIndex());
        assertFalse(raw.contains("stableRecentLines"));
        assertEquals(LocalDate.now(), prefs.recentSabqi().get(0).addedOn);
    }

    @Test public void migratedV1StateReopensAfterProcessDeathWithoutSecondMigration() {
        raw.edit()
            .putInt("schema", 1)
            .putString("programStartDate", "2026-09-01")
            .putString("promotedFrontier", "2:150")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRotationStart", "49:1")
            .putString("lastSabqiLabel", "progression à conserver")
            .putString("recentSabqi", "[{\"start\":10,\"end\":14}]")
            .commit();

        HifzPrefs first = new HifzPrefs(context);
        int promotedCount = first.promotedRanges().size();
        int pendingCount = first.unconsolidatedPromotedRanges().size();

        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(5, reopened.schema());
        assertEquals(promotedCount, reopened.promotedRanges().size());
        assertEquals(pendingCount, reopened.unconsolidatedPromotedRanges().size());
        assertEquals("progression à conserver", reopened.lastSabqiLabel());
        assertTrue(reopened.isUnconsolidatedPromoted(new VerseRef(2, 100)));
        assertTrue(reopened.itqanWorkCorpus().contains(new VerseRef(49, 1)));
    }

    @Test public void crossSurahStartedUnitResetsEvenIfHardSurahWasRemovedBeforeV5Migration() {
        raw.edit()
            .putInt("schema", 4)
            .putString("programStartDate", "2026-09-01")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", "[{\"start\":\"55:70\",\"end\":\"56:16\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"55:70\",\"end\":\"56:16\"}]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("itqanRotationStart", "2:1")
            .putString("itqanCursor", "2:1")
            .putString("murajaahCursor", "2:1")
            .putString("hardAnchoringSurahs", "[]")
            .putString("itqanUnitStart", "55:70")
            .putString("itqanUnitEnd", "56:16")
            .putInt("itqanBlockIndex", 2)
            .putInt("itqanRep", 9)
            .putInt("itqanAssisted", 1)
            .putInt("itqanFinalReveals", 1)
            .putLong("itqanElapsedMs", 12345L)
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(0, prefs.itqanAssisted());
        assertEquals(0, prefs.itqanFinalReveals());
        assertEquals(0L, prefs.elapsedFor(HifzSessionActivity.ITQAN));
        assertEquals(new VerseRef(55,70), prefs.itqanUnitStart());
        assertEquals(new VerseRef(56,16), prefs.itqanUnitEnd());
    }

    @Test public void legacyRecentEntryWithoutAddedOnUsesTheMigrationFallback() {
        raw.edit()
            .putInt("schema", 3)
            .putString("programStartDate", "2025-01-01")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"},{\"start\":\"49:1\",\"end\":\"114:6\"}]")
            .putString("promotedRanges", "[]")
            .putString("unconsolidatedPromotedRanges", "[]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "49:1")
            .putString("recentSabqi", "[{\"start\":10,\"end\":14}]")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(1, prefs.recentSabqi().size());
        assertEquals(LocalDate.now(), prefs.recentSabqi().get(0).addedOn);
    }
    @Test public void corruptAnchoringEntryIsIgnoredPersistedAndRebuiltFromPendingCorpus() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry initial = prefs.currentAnchoringEntry(geometry);
        assertTrue(initial != null);

        String mixed = "[{\"start\":\"49:1\",\"end\":\"49:5\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0},"
            + "{\"start\":\"bad\",\"end\":\"bad\",\"origin\":\"FUTURE_ENUM\",\"protocol\":\"LIGHT\"}]";
        raw.edit().putString("anchoringQueue", mixed).putBoolean("anchoringQueueInitialized", true).commit();

        java.util.List<AnchoringQueue.Entry> recovered = prefs.anchoringQueue();
        assertEquals(1, recovered.size());
        assertFalse(raw.getBoolean("anchoringQueueInitialized", true));

        AnchoringQueue.Entry rebuilt = prefs.currentAnchoringEntry(geometry);
        assertTrue(rebuilt != null);
        assertTrue(raw.getBoolean("anchoringQueueInitialized", false));
    }

    @Test public void malformedAnchoringQueueFailsOpenAndReconcilesInsteadOfCrashing() {
        HifzPrefs prefs = new HifzPrefs(context);
        raw.edit().putString("anchoringQueue", "not-json").putBoolean("anchoringQueueInitialized", true).commit();

        assertTrue(prefs.anchoringQueue().isEmpty());
        assertFalse(raw.getBoolean("anchoringQueueInitialized", true));
        assertTrue(prefs.currentAnchoringEntry(GeometryRepository.get(context)) != null);
    }

    @Test public void schemaFourStartedCrossSurahFractionatedUnitResetsOnlySubBlockProgress() {
        String queue = "[{\"start\":\"55:70\",\"end\":\"56:16\",\"origin\":\"PROMOTED\",\"protocol\":\"FULL\",\"failures\":2}]";
        raw.edit()
            .putInt("schema", 4)
            .putString("programStartDate", "2026-09-01")
            .putString("hardAnchoringSurahs", "[55]")
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("itqanUnitStart", "55:70")
            .putString("itqanUnitEnd", "56:16")
            .putInt("itqanBlockIndex", 2)
            .putInt("itqanRep", 17)
            .putInt("itqanAssisted", 4)
            .putInt("itqanFinalReveals", 1)
            .putLong("itqanElapsedMs", 123456L)
            .putString("itqanCursor", "55:70")
            .putString("murajaahCursor", "2:1")
            .putInt("sabqiLineCursor", 321)
            .putString("promotedRanges", "[{\"start\":\"55:70\",\"end\":\"56:16\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"55:70\",\"end\":\"56:16\"}]")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(0, prefs.itqanAssisted());
        assertEquals(0, prefs.itqanFinalReveals());
        assertEquals(0L, prefs.elapsedFor(HifzSessionActivity.ITQAN));
        assertEquals(new VerseRef(55, 70), prefs.itqanUnitStart());
        assertEquals(new VerseRef(56, 16), prefs.itqanUnitEnd());
        assertEquals(queue, raw.getString("anchoringQueue", ""));
        assertEquals(0, raw.getInt("anchoringQueueIndex", -1));
        assertEquals("55:70", raw.getString("itqanCursor", ""));
        assertEquals("2:1", raw.getString("murajaahCursor", ""));
        assertEquals(321, raw.getInt("sabqiLineCursor", -1));
        assertTrue(raw.getString("promotedRanges", "").contains("55:70"));
        assertTrue(raw.getString("unconsolidatedPromotedRanges", "").contains("55:70"));
    }

    @Test public void schemaFourStartedSingleSurahUnitKeepsItsSubBlockProgress() {
        raw.edit()
            .putInt("schema", 4)
            .putString("hardAnchoringSurahs", "[53]")
            .putString("itqanUnitStart", "53:1")
            .putString("itqanUnitEnd", "53:26")
            .putInt("itqanBlockIndex", 1)
            .putInt("itqanRep", 7)
            .putInt("itqanAssisted", 2)
            .putInt("itqanFinalReveals", 1)
            .putLong("itqanElapsedMs", 4444L)
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals(1, prefs.itqanBlockIndex());
        assertEquals(7, prefs.itqanRep());
        assertEquals(2, prefs.itqanAssisted());
        assertEquals(1, prefs.itqanFinalReveals());
        assertEquals(4444L, prefs.elapsedFor(HifzSessionActivity.ITQAN));
    }

}
