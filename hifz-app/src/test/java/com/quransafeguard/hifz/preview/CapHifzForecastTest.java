package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Forecasts are advisory and strictly based on canonical physical QCF lines. */
public final class CapHifzForecastTest {
    private static GeometryRepository geometry() throws Exception {
        Path source = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(source)) source = Path.of("..").resolve(source);
        return GeometryRepository.fromJson(Files.readString(source));
    }

    private static List<CapHifzForecast.Quarter> quarters(GeometryRepository geometry,
            LocalDate date, int sabqi, int page, boolean complete, int days, double attendance) {
        int itqan = geometry.linesForExactIds(geometry.lineIdsOnPage(page)).get(0).globalIndex;
        return CapHifzForecast.quarters(geometry, date, sabqi, itqan, complete,
            day -> WeeklyProgramPolicy.actionFor(day.getDayOfWeek(), HifzPrefs.WeeklyProgram.MANUAL_SEVEN, days),
            days > 0, !complete, List.of(), java.util.Set.of(), null, attendance);
    }

    /** Cap Hifdh intentionally excludes Al-Fātiḥah: 113 surahs (2–114),
     * with 112 inter-surah transitions across its two canonical route legs.
     * The 2 and 49 start positions are physical cursors, not extra credits. */
    @Test public void capHifdhCovers113SurahsAnd112TransitionsWithoutAlFatiha() throws Exception {
        GeometryRepository geo = geometry();
        int surahs = 0;
        int transitions = 0;
        for (int surah = 2; surah <= 114; surah++) {
            VerseRef start = new VerseRef(surah, 1);
            assertTrue("Cap Hifdh source has no opening line for surah " + surah,
                geo.firstLineIndex(start) >= 0);
            assertTrue("Surah " + surah + " cannot be opened in the Mushaf",
                geo.pageForVerse(start) >= 1);
            assertFalse("Surah " + surah + " is missing its canonical Arabic title",
                QuranSurahNames.rawName(surah).isEmpty());
            surahs++;
            if (surah < 114) {
                assertTrue("A Quran surah transition is reversed at " + surah,
                    GeometryRepository.ordinal(new VerseRef(surah + 1, 1))
                        > GeometryRepository.ordinal(start));
                transitions++;
            }
        }
        assertEquals("Cap Hifdh covers 113 canonical surahs, excluding Al-Fātiḥah",
            113, surahs);
        assertEquals("113 Cap Hifdh surahs imply 112 inter-surah transitions",
            112, transitions);
        assertEquals("Al-Baqarah must flow directly into Āl ʿImrān",
            new VerseRef(3, 1), QuranCanon.INSTANCE.next(new VerseRef(2, 286)));
        assertEquals("Acquisition to Maîtrise route junction must stay canonical",
            new VerseRef(49, 1), QuranCanon.INSTANCE.next(new VerseRef(48, 29)));
    }

    @Test public void canonicalJuzAndHizbBoundariesMatchMushaf() {
        assertEquals(1, CapHifzForecast.juz(new VerseRef(2,125)));
        assertEquals(2, CapHifzForecast.hizb(new VerseRef(2,125)));
        assertEquals(26, CapHifzForecast.juz(new VerseRef(48,29)));
        assertEquals(52, CapHifzForecast.hizb(new VerseRef(48,29)));
        assertEquals(30, CapHifzForecast.juz(new VerseRef(114,6)));
        assertEquals(60, CapHifzForecast.hizb(new VerseRef(114,6)));
    }

    @Test public void quartersAreRealQuarterEndsAndNoFutureCreditIsWritten() throws Exception {
        GeometryRepository geo = geometry();
        int sabqi = geo.firstLineIndex(new VerseRef(2,125));
        List<CapHifzForecast.Quarter> quarters =
            quarters(geo, LocalDate.of(2026,11,14), sabqi,
                geo.pageForVerse(new VerseRef(53,35)), false, 3, .9);
        assertEquals(LocalDate.of(2026,12,31), quarters.get(0).date);
        assertEquals(LocalDate.of(2027,3,31), quarters.get(1).date);
        assertEquals(LocalDate.of(2027,6,30), quarters.get(2).date);
        for (int i=1; i<quarters.size(); i++) {
            assertTrue(quarters.get(i).date.isAfter(quarters.get(i-1).date));
            assertTrue(GeometryRepository.ordinal(quarters.get(i).sabqi)
                >= GeometryRepository.ordinal(quarters.get(i-1).sabqi));
        }
        CapHifzForecast.Quarter last=quarters.get(quarters.size()-1);
        assertTrue(last.sabqiComplete);
        assertEquals(new VerseRef(48,29),last.sabqi);
        assertTrue(quarters.size()>=40 && quarters.size()<=50);
    }

    @Test public void afterNasItqanNeverClaimsNewCompletion() throws Exception {
        GeometryRepository geo=geometry();
        List<CapHifzForecast.Quarter> rows=
            quarters(geo,LocalDate.of(2026,11,14),
                geo.firstLineIndex(new VerseRef(2,125)), 527, false, 3, .9);
        assertNotNull(rows.get(0).itqan);
        boolean seenNas=false;
        for(CapHifzForecast.Quarter row: rows) {
            if(row.firstTailComplete) seenNas=true;
            if(seenNas) assertNull(row.itqan);
        }
        assertTrue(seenNas);
        List<CapHifzForecast.Quarter> postNas=
            quarters(geo,LocalDate.of(2026,11,14),
                geo.firstLineIndex(new VerseRef(2,125)),604,true,3,.9);
        for(CapHifzForecast.Quarter row:postNas) {
            assertNull(row.itqan);
            assertTrue(row.firstTailComplete);
        }
    }

    @Test public void quarterEndingTodayDoesNotCreditAnUnstartedItqanPage() throws Exception {
        GeometryRepository geo = geometry();
        List<CapHifzForecast.Quarter> rows = quarters(
            geo, LocalDate.of(2026,12,31), geo.firstLineIndex(new VerseRef(2,125)), 529, false, 3, 1);
        VerseRef position = rows.get(0).itqan;
        assertTrue(position == null || geo.lastLineIndex(position)
            < geo.linesForExactIds(geo.lineIdsOnPage(529)).get(0).globalIndex);
    }

    @Test public void finishedSabqiDoesNotStopAnUnfinishedItqanProjection() throws Exception {
        GeometryRepository geo = geometry();
        List<CapHifzForecast.Quarter> rows = quarters(
            geo, LocalDate.of(2026,11,14), geo.lastLineIndex(new VerseRef(48,29)) + 1, 527, false, 3, .9);
        assertTrue(rows.get(0).sabqiComplete);
        assertFalse(rows.get(0).firstTailComplete);
        assertTrue(rows.get(rows.size() - 1).firstTailComplete);
    }

    @Test public void aStoppedCadenceDoesNotForecastAlFathAsFinished() throws Exception {
        GeometryRepository geo = geometry();
        List<CapHifzForecast.Quarter> rows = quarters(
            geo, LocalDate.of(2026,12,31), geo.firstLineIndex(new VerseRef(2,125)), 604, true, 0, 1);
        assertEquals(1, rows.size());
        assertFalse(rows.get(0).sabqiComplete);
    }

    @Test public void milestonesAreDatedOnTheDayTheBoundaryIsCrossed() throws Exception {
        GeometryRepository geo = geometry();
        LocalDate today = LocalDate.of(2026, 11, 14);
        List<CapHifzForecast.Quarter> rows = quarters(geo, today,
            geo.firstLineIndex(new VerseRef(2,125)), 527, false, 3, 1);
        LocalDate previous = today;
        int previousHizb = 0;
        int previousOrdinal = -1;
        boolean foundImran = false;
        boolean foundBeforeQuarterEnd = false;
        for (CapHifzForecast.Quarter row : rows) {
            for (CapHifzForecast.Milestone point : row.sabqiMilestones) {
                assertTrue(point.hizb >= previousHizb);
                int ordinal = GeometryRepository.ordinal(point.verse);
                assertTrue("Projected Quran milestones must never reverse or duplicate", ordinal > previousOrdinal);
                foundImran |= point.verse.equals(new VerseRef(3, 1));
                assertFalse(point.date.isBefore(previous));
                assertTrue(point.date.isAfter(today));
                assertFalse(point.date.isAfter(row.date));
                assertEquals(point.hizb, CapHifzForecast.hizb(point.verse));
                foundBeforeQuarterEnd |= point.date.isBefore(row.date);
                previous = point.date;
                previousHizb = point.hizb;
                previousOrdinal = ordinal;
            }
        }
        assertEquals(52, previousHizb);
        assertTrue("Āl ʿImrān 3:1 must appear as a dated canonical transition", foundImran);
        assertTrue(foundBeforeQuarterEnd);
    }

    @Test public void everyCanonicalSurahTransitionIsProjectedWithoutSkipsOnBothRoutes() throws Exception {
        GeometryRepository geo = geometry();
        LocalDate today = LocalDate.of(2026, 11, 15);
        List<CapHifzForecast.Quarter> rows = CapHifzForecast.quarters(
            geo, today, geo.firstLineIndex(new VerseRef(2, 125)),
            geo.firstLineIndex(new VerseRef(49, 1)), false,
            day -> WeeklyProgramPolicy.actionFor(
                day.getDayOfWeek(), HifzPrefs.WeeklyProgram.MANUAL_SEVEN, 3),
            true, true, List.of(), Set.of(), null, .9);
        assertTrue("Acquisition must reach Al-Fatḥ", rows.get(rows.size() - 1).sabqiComplete);
        assertTrue("Maîtrise must reach An-Nās", rows.get(rows.size() - 1).firstTailComplete);

        Set<Integer> acquisitionSurahs = new LinkedHashSet<>();
        Set<Integer> masterySurahs = new LinkedHashSet<>();
        int lastAcquisition = -1, lastMastery = -1;
        for (CapHifzForecast.Quarter row : rows) {
            for (CapHifzForecast.Milestone milestone : row.sabqiMilestones) {
                int order = GeometryRepository.ordinal(milestone.verse);
                assertTrue("Acquisition must be strictly canonical", order > lastAcquisition);
                lastAcquisition = order;
                if (milestone.verse.getAyah() == 1)
                    acquisitionSurahs.add(milestone.verse.getSurah());
            }
            for (CapHifzForecast.Milestone milestone : row.tailMilestones) {
                int order = GeometryRepository.ordinal(milestone.verse);
                assertTrue("Maîtrise must be strictly canonical", order > lastMastery);
                lastMastery = order;
                if (milestone.verse.getAyah() == 1)
                    masterySurahs.add(milestone.verse.getSurah());
            }
        }

        List<Integer> expectedAcquisition = new ArrayList<>();
        for (int surah = 3; surah <= 48; surah++) expectedAcquisition.add(surah);
        List<Integer> expectedMastery = new ArrayList<>();
        for (int surah = 50; surah <= 114; surah++) expectedMastery.add(surah);
        assertEquals("No surah start may be skipped between Al-Baqarah and Al-Fatḥ",
            new LinkedHashSet<>(expectedAcquisition), acquisitionSurahs);
        assertEquals("No surah start may be skipped between Al-Ḥujurāt and An-Nās",
            new LinkedHashSet<>(expectedMastery), masterySurahs);
    }

    @Test public void timelineKeepsOneNearbyStepAndNextSurahTransition() {
        LocalDate d = LocalDate.of(2027, 2, 19);
        CapHifzForecast.Milestone baqarah203 =
            new CapHifzForecast.Milestone(d, new VerseRef(2, 203), 4);
        CapHifzForecast.Milestone baqarah253 =
            new CapHifzForecast.Milestone(d.plusMonths(4), new VerseRef(2, 253), 5);
        CapHifzForecast.Milestone imran =
            new CapHifzForecast.Milestone(d.plusMonths(9), new VerseRef(3, 1), 5);
        assertEquals(List.of(baqarah203, imran), CapHifzForecast.visibleMilestones(
            List.of(baqarah203, baqarah253, imran)));
        assertEquals(List.of(baqarah203, baqarah253), CapHifzForecast.visibleMilestones(
            List.of(baqarah203, baqarah253)));
    }

    @Test public void higherAttendanceCannotFinishLater() throws Exception {
        GeometryRepository geo=geometry();
        int cursor=geo.firstLineIndex(new VerseRef(2,125));
        List<CapHifzForecast.Quarter> slow =
            quarters(geo,LocalDate.of(2026,11,14),cursor,527,false,3,.8);
        List<CapHifzForecast.Quarter> fast =
            quarters(geo,LocalDate.of(2026,11,14),cursor,527,false,3,.9);
        assertFalse(fast.get(fast.size()-1).date.isAfter(slow.get(slow.size()-1).date));
    }
}
