package com.quransafeguard.hifz.preview;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Before;
import org.junit.Test;

public class PreviewGeometryInstrumentedTest {
    private Context context;
    private GeometryRepository geometry;

    @Before public void setup() {
        context = ApplicationProvider.getApplicationContext();
        geometry = GeometryRepository.get(context);
    }

    @Test public void canonicalMushafHasExpectedSabqiBoundariesFrom275() {
        int start = geometry.firstLineIndex(new VerseRef(2,75));
        GeometryRepository.FiveLineBlock first = geometry.fiveLineBlock(start);
        assertEquals(new VerseRef(2,75), first.startVerse);
        assertEquals(new VerseRef(2,76), first.endVerse);
        assertFalse(first.endsInsideVerse);
        assertEquals(5, first.lineIds.size());

        GeometryRepository.FiveLineBlock second = geometry.fiveLineBlock(first.endLineIndex + 1);
        assertEquals(new VerseRef(2,77), second.startVerse);
        assertEquals(new VerseRef(2,79), second.endVerse);
        assertFalse(second.endsInsideVerse);

        GeometryRepository.FiveLineBlock third = geometry.fiveLineBlock(second.endLineIndex + 1);
        assertEquals(new VerseRef(2,80), third.startVerse);
        assertEquals(new VerseRef(2,82), third.endVerse);
        assertTrue(third.endsInsideVerse);
    }

    @Test public void itqanStartsExactlyAt491AndNeverIncludesEarlierVerseOnPage() {
        HifzPrefs prefs = new HifzPrefs(context);
        prefs.resetPreviewState();
        GeometryRepository.VerseUnit unit = geometry.eligiblePageUnit(new VerseRef(49,1), prefs.corpus());
        assertEquals(new VerseRef(49,1), unit.start);
        assertEquals(515, unit.page);
        for (VerseRef ref : unit.verses) {
            assertTrue(GeometryRepository.ordinal(ref) >= GeometryRepository.ordinal(new VerseRef(49,1)));
            assertTrue(prefs.corpus().contains(ref));
        }
    }

    @Test public void murajaahCursorIsIndependentFromItqanCursor() {
        HifzPrefs prefs = new HifzPrefs(context);
        prefs.resetPreviewState();
        VerseRef itqanBefore = prefs.itqanCursor();
        VerseRef nextMurajaah = prefs.murajaahCorpus().next(prefs.murajaahCursor());
        assertTrue(prefs.completeMurajaah(nextMurajaah, "2026-09-13", "test rotation naturelle"));
        assertEquals(itqanBefore, prefs.itqanCursor());
        assertEquals(nextMurajaah, prefs.murajaahCursor());
    }

    @Test public void tafsirCorpusLoadsAnAuditedVerse() throws Exception {
        TafsirRepository.Entry entry = new TafsirRepository(context).load(new VerseRef(2,1));
        assertNotNull(entry);
        assertFalse(entry.commentaryRuns.isEmpty());
        assertFalse(entry.commentaryRuns.get(0).text.trim().isEmpty());
    }

    @Test public void hifzPreviewDeclaresNoAccessibilityServiceOrBroadPackagePermission() throws Exception {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SERVICES | PackageManager.GET_PERMISSIONS);
        if (info.services != null) {
            for (android.content.pm.ServiceInfo service : info.services) {
                assertNotEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", service.permission);
            }
        }
        if (info.requestedPermissions != null) {
            for (String permission : info.requestedPermissions) {
                assertNotEquals("android.permission.QUERY_ALL_PACKAGES", permission);
            }
        }
    }
}
