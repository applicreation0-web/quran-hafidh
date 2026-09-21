package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class AnchoringProtocolContinuityInstrumentedTest {
    private static final String PREFS_NAME = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences raw;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void fullFractionatedUnitKeepsFullAcrossQueueReorder() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry before = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(before.start);
        VerseRef end = GeometryRepository.parseVerse(before.end);
        assertTrue(prefs.advanceItqanBlock(1, start, end, "2026-09-13", "bloc 1"));

        putOtherPromotionAtQueueHead(AnchoringQueue.ItqanProtocol.FULL);
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        AnchoringQueue.Entry after = prefs.currentAnchoringEntry(geometry);

        assertSameIdentity(before, after);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, after.protocol);
        assertEquals(40, PreviewConfig.itqanTotalReps(after.protocol));
    }

    @Test public void lightFractionatedUnitKeepsLightAcrossQueueReorder() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.LIGHT);
        AnchoringQueue.Entry before = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(before.start);
        VerseRef end = GeometryRepository.parseVerse(before.end);
        assertTrue(prefs.advanceItqanBlock(1, start, end, "2026-09-13", "bloc 1"));

        putOtherPromotionAtQueueHead(AnchoringQueue.ItqanProtocol.LIGHT);
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        AnchoringQueue.Entry after = prefs.currentAnchoringEntry(geometry);

        assertSameIdentity(before, after);
        assertEquals(AnchoringQueue.ItqanProtocol.LIGHT, after.protocol);
        assertEquals(35, PreviewConfig.itqanTotalReps(after.protocol));
    }

    @Test public void killRestartWithItqanRepAboveZeroResumesSameUnitAndProtocol() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry expected = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(expected.start);
        VerseRef end = GeometryRepository.parseVerse(expected.end);
        assertTrue(prefs.setItqanProgress(3, 0, start, end));

        HifzPrefs reopened = new HifzPrefs(context);
        assertSameIdentity(expected, reopened.currentAnchoringEntry(geometry));
        assertEquals(3, reopened.itqanRep());
        assertSessionShowsProtocol(40);

        HifzPrefs afterActivityRecreate = new HifzPrefs(context);
        assertSameIdentity(expected, afterActivityRecreate.currentAnchoringEntry(geometry));
        assertEquals(3, afterActivityRecreate.itqanRep());
    }

    @Test public void killRestartWithItqanBlockIndexAboveZeroResumesSameUnitAndProtocol() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry expected = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(expected.start);
        VerseRef end = GeometryRepository.parseVerse(expected.end);
        assertTrue(prefs.advanceItqanBlock(1, start, end,
            LocalDate.now().minusDays(1).toString(), "bloc 1"));

        HifzPrefs reopened = new HifzPrefs(context);
        assertSameIdentity(expected, reopened.currentAnchoringEntry(geometry));
        assertEquals(1, reopened.itqanBlockIndex());
        assertSessionShowsProtocol(40);

        HifzPrefs afterActivityRecreate = new HifzPrefs(context);
        assertSameIdentity(expected, afterActivityRecreate.currentAnchoringEntry(geometry));
        assertEquals(1, afterActivityRecreate.itqanBlockIndex());
    }

    @Test public void todayCardAndSessionScreenAgreeOnUnitAndProtocol() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry expected = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(expected.start);
        VerseRef end = GeometryRepository.parseVerse(expected.end);
        assertTrue(prefs.advanceItqanBlock(1, start, end,
            LocalDate.now().minusDays(1).toString(), "bloc 1"));

        String todayDetail = MainActivity.anchoringTodayDetail(prefs, geometry);
        assertTrue(todayDetail.contains(shortRange(start, end)));
        assertTrue(todayDetail.contains("×40"));
        assertSessionShowsProtocol(40);
    }

    @Test public void startEndAndProtocolArePreservedExactly() {
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry before = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(before.start);
        VerseRef end = GeometryRepository.parseVerse(before.end);
        assertTrue(prefs.advanceItqanBlock(1, start, end, "2026-09-13", "bloc 1"));
        putOtherPromotionAtQueueHead(AnchoringQueue.ItqanProtocol.FULL);
        assertTrue(prefs.reconcileAnchoringQueue(geometry));

        HifzPrefs reopened = new HifzPrefs(context);
        AnchoringQueue.Entry after = reopened.currentAnchoringEntry(geometry);
        assertSameIdentity(before, after);
        assertEquals(start, reopened.itqanUnitStart());
        assertEquals(end, reopened.itqanUnitEnd());
    }

    @Test public void noPrematureValidationAndNoDoubleCredit() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        HifzPrefs prefs = fractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        AnchoringQueue.Entry entry = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        VerseRef end = GeometryRepository.parseVerse(entry.end);
        VerseRef murajaahBefore = prefs.murajaahCursor();
        String label = "Ancrage fractionné · bloc 1/3 validé · révélations 0";
        assertTrue(prefs.advanceItqanBlock(1, start, end, today.toString(), label));

        assertEquals(1, prefs.itqanBlockIndex());
        assertTrue(prefs.isUnconsolidatedPromoted(start));
        assertEquals(murajaahBefore, prefs.murajaahCursor());
        assertNotNull(prefs.inProgressAnchoringEntry());
    }


    @Test public void crossSurahBlockSelectionSurvivesActivityRecreation() {
        HifzPrefs prefs = crossSurahFractionatedFixture(AnchoringQueue.ItqanProtocol.FULL);
        VerseRef start = new VerseRef(55, 70);
        VerseRef end = new VerseRef(56, 16);
        assertTrue(prefs.advanceItqanBlock(2, start, end,
            HifzClock.today().minusDays(1).toString(), "C23 bloc 2 validé"));
        assertSessionContains("56:1 → 56:9", "3/4");

        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(2, reopened.itqanBlockIndex());
        assertEquals(start, reopened.itqanUnitStart());
        assertEquals(end, reopened.itqanUnitEnd());
    }

    private HifzPrefs fractionatedFixture(AnchoringQueue.ItqanProtocol protocol) {
        new HifzPrefs(context);
        String range = "[{\"start\":\"53:1\",\"end\":\"53:26\"}]";
        String queue = "[{\"start\":\"53:1\",\"end\":\"53:26\","
            + "\"origin\":\"RECONSTRUCTION\",\"protocol\":\"" + protocol.name()
            + "\",\"failures\":0}]";
        raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("hardAnchoringSurahs", "[53]")
            .putString("lastItqanDate", "")
            .commit();
        return new HifzPrefs(context);
    }


    private HifzPrefs crossSurahFractionatedFixture(AnchoringQueue.ItqanProtocol protocol) {
        new HifzPrefs(context);
        String range = "[{\"start\":\"55:70\",\"end\":\"56:16\"}]";
        String queue = "[{\"start\":\"55:70\",\"end\":\"56:16\","
            + "\"origin\":\"RECONSTRUCTION\",\"protocol\":\"" + protocol.name()
            + "\",\"failures\":0}]";
        raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("hardAnchoringSurahs", "[55]")
            .putString("lastItqanDate", "")
            .commit();
        return new HifzPrefs(context);
    }

    private void putOtherPromotionAtQueueHead(AnchoringQueue.ItqanProtocol currentProtocol) {
        String ranges = "[{\"start\":\"2:75\",\"end\":\"2:75\"},"
            + "{\"start\":\"53:1\",\"end\":\"53:26\"}]";
        String queue = "[{\"start\":\"2:75\",\"end\":\"2:75\","
            + "\"origin\":\"FORCED_PROMOTION\",\"protocol\":\"FULL\",\"failures\":0},"
            + "{\"start\":\"53:1\",\"end\":\"53:26\","
            + "\"origin\":\"RECONSTRUCTION\",\"protocol\":\"" + currentProtocol.name()
            + "\",\"failures\":0}]";
        raw.edit()
            .putString("promotedRanges", ranges)
            .putString("unconsolidatedPromotedRanges", ranges)
            .putString("forcedPromotedRanges", "[{\"start\":\"2:75\",\"end\":\"2:75\"}]")
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .commit();
    }

    private void assertSessionShowsProtocol(int repetitions) {
        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.ITQAN);
        try (ActivityScenario<HifzSessionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity ->
                assertTrue("Session must show ×" + repetitions,
                    containsText(activity.findViewById(android.R.id.content), "×" + repetitions)));
            scenario.recreate();
            scenario.onActivity(activity ->
                assertTrue("Recreated session must show ×" + repetitions,
                    containsText(activity.findViewById(android.R.id.content), "×" + repetitions)));
        }
    }


    private void assertSessionContains(String first, String second) {
        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.ITQAN);
        try (ActivityScenario<HifzSessionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                assertTrue("Session must show " + first, containsText(root, first));
                assertTrue("Session must show " + second, containsText(root, second));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                assertTrue("Recreated session must show " + first, containsText(root, first));
                assertTrue("Recreated session must show " + second, containsText(root, second));
            });
        }
    }

    private static boolean containsText(View view, String expected) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(expected)) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (containsText(group.getChildAt(i), expected)) return true;
        }
        return false;
    }

    private static void assertSameIdentity(AnchoringQueue.Entry expected, AnchoringQueue.Entry actual) {
        assertNotNull(actual);
        assertEquals(expected.start, actual.start);
        assertEquals(expected.end, actual.end);
        assertEquals(expected.protocol, actual.protocol);
    }

    private static String shortRange(VerseRef start, VerseRef end) {
        return start.getSurah() == end.getSurah()
            ? start.getSurah() + ":" + start.getAyah() + "–" + end.getAyah()
            : start + "–" + end;
    }
}
