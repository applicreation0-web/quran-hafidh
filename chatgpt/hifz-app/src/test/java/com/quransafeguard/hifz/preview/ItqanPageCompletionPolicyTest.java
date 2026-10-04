package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ItqanPageCompletionPolicyTest {
    @Test public void noTrailingLinesMeansNoOffer() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 7);
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, Collections.emptyList());
        assertEquals(ItqanPageCompletionPolicy.Choice.NONE, offer.choice);
        assertTrue(offer.bonusLineIds.isEmpty());
        assertEquals(idsOf(base), offer.actualLineIds);
    }

    @Test public void oneTrailingLineOffersPlusOne() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 7);
        List<GeometryRepository.LineMeta> trailing = Collections.singletonList(
            line(7, 100, "bonus-0", new VerseRef(2, 3)));
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, trailing);
        assertEquals(ItqanPageCompletionPolicy.Choice.PLUS_ONE, offer.choice);
        assertEquals(Collections.singletonList("bonus-0"), offer.bonusLineIds);
        List<String> expectedActual = new ArrayList<>(idsOf(base));
        expectedActual.add("bonus-0");
        assertEquals(expectedActual, offer.actualLineIds);
    }

    @Test public void twoTrailingLinesOfferPlusTwoWhenCandidateStaysUnderTheCap() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 9);
        List<GeometryRepository.LineMeta> trailing = twoTrailing(9, 100, new VerseRef(2, 3));
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, trailing);
        assertEquals(ItqanPageCompletionPolicy.Choice.PLUS_TWO, offer.choice);
        assertEquals(11, offer.actualLineIds.size());
    }

    @Test public void neverProposesMoreThanTwoBonusLines() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 7);
        ArrayList<GeometryRepository.LineMeta> trailing = new ArrayList<>();
        for (int i = 0; i < 3; i++) trailing.add(line(7 + i, 100, "extra-" + i, new VerseRef(2, 3)));
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, trailing);
        assertEquals(ItqanPageCompletionPolicy.Choice.NONE, offer.choice);
        assertEquals(idsOf(base), offer.actualLineIds);
    }

    /**
     * The critical safety case: an already-legal 11-line base (kept whole by the existing policy)
     * plus any bonus becomes 12 lines, which StabilizationHalfPagePolicy.planPage splits into two
     * physical units instead of reproducing the candidate as one — exactly what would later make
     * completeConsolidationSessionV6's re-verification throw. The offer must refuse rather than
     * ever hand the caller a merged block that can't round-trip through planPage as a single unit.
     */
    @Test public void refusesABonusThatWouldPushAnElevenLineBaseOverTheConsolidationCap() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 11);
        List<GeometryRepository.LineMeta> trailing = Collections.singletonList(
            line(11, 100, "bonus-0", new VerseRef(2, 3)));
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, trailing);
        assertEquals(ItqanPageCompletionPolicy.Choice.NONE, offer.choice);
        assertEquals(idsOf(base), offer.actualLineIds);
    }

    @Test public void refusesWhenCandidateCrossesASurahBoundary() {
        List<GeometryRepository.LineMeta> base = uniqueLines(100, 2, 7);
        List<GeometryRepository.LineMeta> trailing = Collections.singletonList(
            line(7, 100, "bonus-0", new VerseRef(3, 1)));
        ItqanPageCompletionPolicy.Offer offer = ItqanPageCompletionPolicy.evaluate(
            idsOf(base), base, trailing);
        assertEquals(ItqanPageCompletionPolicy.Choice.NONE, offer.choice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresANonEmptyBase() {
        ItqanPageCompletionPolicy.evaluate(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList());
    }

    private static List<String> idsOf(List<GeometryRepository.LineMeta> lines) {
        List<String> ids = new ArrayList<>();
        for (GeometryRepository.LineMeta l : lines) ids.add(l.id);
        return ids;
    }

    private static List<GeometryRepository.LineMeta> twoTrailing(int startIndex, int page, VerseRef verse) {
        ArrayList<GeometryRepository.LineMeta> trailing = new ArrayList<>();
        trailing.add(line(startIndex, page, "bonus-0", verse));
        trailing.add(line(startIndex + 1, page, "bonus-1", verse));
        return trailing;
    }

    private static List<GeometryRepository.LineMeta> uniqueLines(int page, int surah, int count) {
        List<GeometryRepository.LineMeta> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) lines.add(line(i, page, "l-" + i, new VerseRef(surah, i + 1)));
        return lines;
    }

    private static GeometryRepository.LineMeta line(int index, int page, String id, VerseRef verse) {
        return new GeometryRepository.LineMeta(index, id, page, Collections.singletonList(verse));
    }
}
