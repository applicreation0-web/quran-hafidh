package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.VerseRange;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import static org.junit.Assert.*;

/** Real 604-page KFQC regression for the BOOX small-acquired-corpus P0. */
public final class GeometryEligiblePlannerBooxRegressionTest {
    private static GeometryRepository geometry() throws Exception {
        Path source=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(source))source=Paths.get("../app/src/main/assets/reader109/geometry.json");
        assertTrue("Original public 604-page geometry must be present",Files.isRegularFile(source));
        return GeometryRepository.fromJson(new String(Files.readAllBytes(source),StandardCharsets.UTF_8));
    }
    @Test public void smallAcquiredCorpusCanCompleteFullReviewLinePlanWithoutSilentCredit()throws Exception {
        GeometryRepository original=geometry();
        VerseRef first=new VerseRef(2,47),last=new VerseRef(2,51);
        EligibleCorpus acquired=EligibleCorpus.Companion.of(
            Collections.singletonList(new VerseRange(first,last)));
        // Before the P0 repair, a hard 20-wrap limit aborted an otherwise valid
        // 200-line active-review plan, preventing the real validation button.
        GeometryRepository.EligibleLinePlan result=original.planEligibleLines(first,200,acquired);
        assertEquals(first,result.start);
        assertEquals(200,result.requestedLines);
        assertTrue(acquired.contains(result.actualPlannedEnd));
        assertTrue(result.traversalVerses.contains(first));
        assertTrue(result.traversalVerses.contains(last));
        assertTrue("Real repeated eligible lines must be counted, not fake Hifz receipts",
            result.traversalVerses.size()>=200);
        GeometryRepository.EligibleLinePlan again=original.planEligibleLines(first,200,acquired);
        assertEquals("Planning must be deterministic after BOOX pause/recreation",
            result.actualPlannedEnd,again.actualPlannedEnd);
        assertEquals(result.traversalVerses,again.traversalVerses);
    }
}