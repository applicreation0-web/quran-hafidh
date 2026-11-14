package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CapHifzEvidence.Track;

public final class CapHifzEvidenceTest {
    @Test public void coverageAndActualReceiptDateControlEachIndependentTrack() {
        LocalDate monday = LocalDate.of(2026, 11, 9);
        LocalDate today = monday.plusDays(5);
        Map<Track, CapHifzEvidence.Count> counts = CapHifzEvidence.count(monday, today, today,
            monday.plusDays(1), monday.plusDays(2),
            day -> day.getDayOfWeek().getValue() % 2 == 1 ? CadenceAction.LEARNING : CadenceAction.STABILIZATION,
            day -> true, day -> !day.equals(monday.plusDays(3)),
            (track, day) -> track == Track.SABQI && day.equals(monday.plusDays(4))
                || track == Track.ITQAN && day.equals(monday.plusDays(1))
                || track == Track.EVENING && day.equals(today));
        assertEquals("1/2", counts.get(Track.SABQI).fraction());
        assertEquals("1/1", counts.get(Track.ITQAN).fraction());
        assertEquals("0/3", counts.get(Track.MORNING).fraction());
        assertEquals("1/4", counts.get(Track.EVENING).fraction());
    }

    @Test public void firstDateRequiresARealPhysicalReceiptAndIgnoresFutureEntries() {
        String json = "[{\"family\":\"LEARNING\",\"date\":\"2026-11-15\",\"lines\":[\"2:0\"]},"
            + "{\"family\":\"LEARNING\",\"date\":\"2026-11-09\",\"lines\":[\"2:1\"]},"
            + "{\"family\":\"LEARNING\",\"date\":\"2026-11-07\",\"lines\":[\"2:1\"]}]";
        List<CapHifzEvidence.Lesson> lessons = CapHifzEvidence.lessons(json, Set.of("2:0", "2:1"));
        assertEquals(Map.of("2:1", LocalDate.of(2026, 11, 7)),
            CapHifzEvidence.firstDates(lessons, Track.SABQI, LocalDate.of(2026, 11, 14)));
        assertThrows(IllegalStateException.class, () -> CapHifzEvidence.lessons(json, Set.of("2:1")));
    }
}
