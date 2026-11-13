package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.After;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public final class JuzMaintenanceStoreTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final GeometryRepository QCF = MorningProtocolFixture.realGeometry();
    private static final EligibleCorpus LAST_JUZ = EligibleCorpus.Companion.of(List.of(
        new VerseRange(new VerseRef(78, 1), new VerseRef(114, 6))));

    @After public void resetClock() { HifzClock.resetClockForTests(); }

    @Test public void visibleAndMaskedPassesHaveIdenticalPagesAndCorrectionsAreEarned() throws Exception {
        HifzClock.setClockForTests(Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        for (int revealCount : List.of(0, 1, 3)) {
            InMemoryPrefs disk = new InMemoryPrefs();
            var store = new JuzMaintenanceStore(disk);
            var prefs = new HifzPrefs(disk, QCF);
            assertNull(store.open(TODAY, QCF, LAST_JUZ));
            assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
                ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(49, 1), true)));
            disk.edit().putString("activeMurajaahCursor", "2:10").putString("murajaahCursor", "2:20").commit();
            var draft = store.open(TODAY, QCF, LAST_JUZ);
            assertEquals(30, draft.juz);
            var plan = JuzMaintenanceStore.plan(draft.juz, QCF, LAST_JUZ);
            assertEquals(Integer.valueOf(582), plan.pages.get(0));
            assertEquals(Integer.valueOf(604), plan.pages.get(plan.pages.size() - 1));
            var read = new ArrayList<Integer>();
            var recalled = new ArrayList<Integer>();
            while ((draft = store.read()).phase == JuzMaintenanceStore.Phase.READ) {
                read.add(draft.page(plan));
                assertTrue(store.advance(draft, plan, TODAY));
                assertFalse(store.advance(draft, plan, TODAY));
            }
            for (int index = 0; index < plan.pages.size(); index++) {
                draft = store.read();
                assertEquals(JuzMaintenanceStore.Phase.RECALL, draft.phase);
                recalled.add(draft.page(plan));
                if (index < revealCount) {
                    assertTrue(store.reveal(draft, plan));
                    draft = store.read();
                    assertTrue(store.reveal(draft, plan));
                    assertEquals(index + 1, store.read().revealed.size());
                }
                assertTrue(store.advance(draft, plan, TODAY));
            }
            assertEquals(read, recalled);
            if (revealCount > 0) {
                assertEquals("", prefs.lastItqanDate());
                store = new JuzMaintenanceStore(disk);
                assertEquals(read.subList(0, revealCount), store.read().revealed);
                assertTrue(store.advance(store.read(), plan, TODAY));
                assertTrue(store.reveal(store.read(), plan));
                assertTrue(store.advance(store.read(), plan, TODAY));
                assertEquals(JuzMaintenanceStore.Phase.CORRECT_READ, store.read().phase);
                for (int i = 0; i < revealCount; i++) {
                    assertTrue(store.advance(store.read(), plan, TODAY));
                    assertTrue(store.advance(store.read(), plan, TODAY));
                }
            }
            assertNull(store.read());
            assertNull(store.open(TODAY, QCF, LAST_JUZ));
            assertEquals(TODAY.toString(), prefs.lastItqanDate());
            var history = new org.json.JSONArray(disk.getString("postNasMaintainedHizbsV1", "[]"));
            assertEquals(1, history.length());
            assertEquals(plan.linesByPage.values().stream().flatMap(List::stream).toList(),
                ConsolidationPhysicalUnitPolicy.decodeLineUnit(history.getString(0)));
            assertEquals("2:10", disk.getString("activeMurajaahCursor", ""));
            assertEquals("2:20", disk.getString("murajaahCursor", ""));
        }
    }

    @Test public void legacyOpenHizbHasPriorityOverWholeJuz() {
        InMemoryPrefs disk = new InMemoryPrefs();
        var prefs = new HifzPrefs(disk, QCF);
        assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(49, 1), true)));
        assertTrue(prefs.usesJuzMaintenance());
        disk.edit().putString("itqanUnitStart", "49:1").putString("itqanUnitEnd", "51:30")
            .putInt("itqanRep", 1).commit();
        assertFalse(prefs.usesJuzMaintenance());
        assertEquals(1, prefs.itqanRep());
    }
}
