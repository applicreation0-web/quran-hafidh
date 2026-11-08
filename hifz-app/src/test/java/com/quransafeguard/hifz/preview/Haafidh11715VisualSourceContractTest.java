package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-only display contracts. All real receipts and Hifz protocols remain unchanged. */
public final class Haafidh11715VisualSourceContractTest {
    private static String source(String path) throws Exception {
        Path file=Paths.get(path);
        if(!Files.exists(file)) file=Paths.get("..",path);
        assertTrue("Missing production source: "+path,Files.exists(file));
        return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
    }
    private static String java(String name)throws Exception {
        return source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/"+name+".java");
    }
    @Test public void snowballCumulRequiresAuthenticPhysicalContinuity()throws Exception{
        String session=java("HifzSessionActivity");
        assertTrue(session.contains("genuineCumul = protocol == ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS"));
        assertTrue(session.contains("protocol == ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS_FIVE"));
        assertTrue(session.contains("ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(sources)"));
        assertTrue(session.contains("ConsolidationPhysicalUnitPolicy.physicallyContinuous(sources,geometry)"));
        assertTrue(session.contains("genuineCumul ? displayName + \" · Cumul\" : displayName"));
        assertTrue("Counter must come from frozen session, never from presentation",
            session.contains("int[] vector = consolidationSession.stageVectorAt(position);"));
        assertTrue(session.contains("consolidationSession.donePerStage() + \"/\" + target"));
        assertFalse("Old duplicated header",session.contains("\"passage final ×5\""));
        assertFalse("Old duplicated header",session.contains("\" lignes · unité \""));
    }
    @Test public void inactiveSessionsCannotPretendToHaveClosingFatiha()throws Exception{
        String session=java("HifzSessionActivity");
        assertTrue(session.contains("if (hasAuthenticClosingReceipt()) showClosingFatiha();"));
        assertTrue(session.contains("else if(!hasShown&&hasAuthenticClosingReceipt())showClosingFatiha();"));
        assertTrue(session.contains("if (sessionRenderFailed || !sessionCompleted || awaitingValidation) return false;"));
        assertTrue(session.contains("date.equals(prefs.lastLearningSnowballEveningDate())"));
        assertTrue(session.contains("date.equals(prefs.lastRecentSabqiReviewDate())"));
        assertTrue("Closing page must still be an authentic full Fatiha",session.contains("geometry.pageForVerse(new VerseRef(1, 7)) != page"));
        assertTrue(session.contains("mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0, false);"));
    }
    @Test public void itineraryRetainsActionsWhileHidingOnlyEmptyRows()throws Exception{
        String main=java("MainActivity");
        assertTrue(main.contains("if (bilan.validated > 0 || bilan.dueToday > 0 || bilan.unverifiedDays > 0)"));
        assertTrue(main.contains("if (hasPostFinalRecapContent())"));
        assertTrue(main.contains("postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.SABQI)"));
        assertTrue(main.contains("hasUsefulSchedulePreview(morningPreview.getText().toString())"));
        assertTrue(main.contains("hasUsefulSchedulePreview(eveningPreview.getText().toString())"));
        assertTrue(main.contains("weekDayPanel.addView(morning);"));
        assertTrue(main.contains("weekDayPanel.addView(evening);"));
        assertTrue(main.contains("showTodayWindow(unfinished);"));
        assertTrue("No missed-session message on home",main.contains("if(label==backlogHomeRow) {"));
        assertTrue(main.contains("label.setVisibility(View.GONE);"));
    }
    @Test public void qcfAndEinkUpdatePathRemainPassive()throws Exception{
        String html=source("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue(html.contains("body.eink #sidemarks{opacity:.88}"));
        assertTrue(html.contains("body.eink #centermark{opacity:.90}"));
        assertTrue(html.contains("#mushaf{width:min(calc(100vw - 72px),calc((100vh - 4px) * 345 / 550))"));
        assertFalse("No visual animation/refresh",html.contains("animation:fade")||html.contains("setInterval"));
    }
    @Test public void refrainsAndJuzPreserveContentNavigationAndLevels()throws Exception{
        String timeline=java("RefrainTimeline"),bar=java("JuzProgressBar"),map=java("ProgressMapActivity");
        assertTrue(timeline.contains("screenDp<540?11.5f:15f"));
        assertTrue(timeline.contains("final int slotDp=Math.max(42,(screenDp-100)/visible)"));
        assertTrue(timeline.contains("RefrainTimelineData.NODES"));
        assertTrue(timeline.contains("if(onSelect!=null)onSelect.selected"));
        assertTrue(bar.contains("filledLines()==0 ? stroke*1.3f : stroke"));
        assertTrue(bar.contains("float acquiredTop=y-acquired*scale;"));
        assertTrue(bar.contains("float stabilizedTop=y-stabilized*scale;"));
        assertTrue(bar.contains("float learnedTop=y-learned*scale;"));
        assertTrue(map.contains("JuzProgressBar"));
    }
    @Test public void buildIdentityIsOnlyIncrement()throws Exception{
        String gradle=source("hifz-app/build.gradle.kts");
        assertTrue(gradle.contains("versionCode = 53"));
        assertTrue(gradle.contains("versionName = \"1.17.15\""));
        assertTrue(gradle.contains("applicationId = \"com.quransafeguard.hifz\""));
    }
}
