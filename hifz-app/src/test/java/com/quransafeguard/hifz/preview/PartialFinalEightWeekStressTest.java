package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

/** Real QCF lines and production persistence/backup codec; no Android UI emulation. */
@RunWith(Parameterized.class)
public final class PartialFinalEightWeekStressTest {
    @Parameterized.Parameters(name="absentWeeks={0},family={1},attendanceMask={2}")
    public static Collection<Object[]> cases() {
        List<Object[]> out=new ArrayList<>();
        for(int w=1;w<=8;w++) for(ConsolidationCycleEngine.Family f:ConsolidationCycleEngine.Family.values())
            for(int mask=0;mask<8;mask++)out.add(new Object[]{w,f,mask});
        return out;
    }
    private final int weeks,mask;
    private final ConsolidationCycleEngine.Family family;
    private static GeometryRepository geometry;
    public PartialFinalEightWeekStressTest(int weeks,ConsolidationCycleEngine.Family family,int mask) {
        this.weeks=weeks;this.family=family;this.mask=mask;
    }
    @BeforeClass public static void geometry() throws Exception {
        Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(p))p=Paths.get("..",p.toString());
        geometry=GeometryRepository.fromJson(Files.readString(p));
    }
    private boolean learning(){return family==ConsolidationCycleEngine.Family.LEARNING;}
    private String prefix(){return learning()?"learning":"stabilization";}
    static InMemoryPrefs empty() {
        InMemoryPrefs disk=new InMemoryPrefs();disk.disk.put("schema",6);
        for(String key:new String[]{"itqanRanges","legacyMurajaahPromotedRanges","promotedRanges",
                "unconsolidatedPromotedRanges","forcedPromotedRanges","anchoringQueue",
                "v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds",
                "v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"})disk.disk.put(key,"[]");
        disk.disk.put("anchoringQueueInitialized",true);
        disk.disk.put("itqanCursor","50:1");disk.disk.put("sabqiLineCursor",100);
        return disk;
    }
    /** Use the actual typed export/check/replace codec, including persisted OPEN cycles. */
    static InMemoryPrefs backupRoundTrip(InMemoryPrefs disk) throws Exception {
        Method encode=HifzBackup.class.getDeclaredMethod("encoded",Map.class);
        Method check=HifzBackup.class.getDeclaredMethod("checkValues",JSONObject.class);
        Method replace=HifzBackup.class.getDeclaredMethod("replace",android.content.SharedPreferences.class,JSONObject.class);
        encode.setAccessible(true);check.setAccessible(true);replace.setAccessible(true);
        JSONObject values=new JSONObject(((JSONObject)encode.invoke(null,disk.getAll())).toString());
        check.invoke(null,values);
        InMemoryPrefs restored=new InMemoryPrefs();replace.invoke(null,restored,values);
        assertEquals("Typed backup must preserve all progress and receipts",disk.disk,restored.disk);
        return restored;
    }
    private List<ConsolidationCycleEngine.Unit> evening(HifzPrefs p,LocalDate d) {
        return learning()?p.learningConsolidationUnits(d):p.stabilizedConsolidationUnits(d);
    }
    private List<ConsolidationCycleEngine.Unit> finals(HifzPrefs p,LocalDate d) {
        return learning()?p.learningSnowballFinalUnits(d,geometry):p.stabilizationSnowballFinalUnits(d,geometry);
    }
    private ConsolidationCycleEngine.Session open(List<ConsolidationCycleEngine.Unit> units,String id) {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(id,family,units.get(0));
        for(int i=1;i<units.size();i++)cycle=engine.addUnit(cycle,units.get(i));
        return engine.openSession(cycle,"stress-"+id);
    }
    private static Set<String> lines(List<ConsolidationCycleEngine.Unit> units) {
        Set<String> ids=new LinkedHashSet<>();
        for(ConsolidationCycleEngine.Unit u:units)ids.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(u.id()));
        return ids;
    }
    @Test public void missedSlotsPartialFinalRestartAndTypedBackupPreserveOnlyEarnedLines() throws Exception {
        InMemoryPrefs disk=empty();HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate original=LocalDate.of(2026,12,21),monday=original.plusWeeks(weeks),sunday=monday.plusDays(6);
        Map<String,Object> initial=new LinkedHashMap<>(disk.disk);
        for(int day=0;day<weeks*7;day++) {
            LocalDate date=original.plusDays(day);
            assertTrue(finals(prefs,date).isEmpty());
            assertTrue(evening(prefs,date).isEmpty());
            prefs=new HifzPrefs(disk);
        }
        assertEquals("Absence alone changes no cursor or credit",initial,disk.disk);
        Set<String> completed=new LinkedHashSet<>();
        int cursor=geometry.firstLineIndex(new VerseRef(2,75));
        for(int slot=0;slot<3;slot++) {
            if((mask&(1<<slot))==0)continue;
            LocalDate date=monday.plusDays(2*slot+(learning()?0:1));
            int count=learning()?5:(slot==0?8:7);
            List<String> block=new ArrayList<>();
            for(int i=0;i<count;i++)block.add(geometry.line(cursor+i).id);
            String oppositeCursor=learning()?disk.getString("itqanCursor",""):
                String.valueOf(disk.getInt("sabqiLineCursor",-1));
            if(learning())assertTrue(prefs.completeSabqiBlockV6(cursor,cursor+count-1,cursor+count,
                block,date.toString(),"stress"));
            else {
                List<GeometryRepository.LineMeta> meta=geometry.linesForExactIds(block);
                VerseRef start=meta.get(0).verses.get(0);
                List<VerseRef> last=meta.get(meta.size()-1).verses;
                assertTrue(prefs.completeStabilizationBlockV6(block,slot+1,false,start,
                    last.get(last.size()-1),start,date.toString(),"stress",Collections.emptyList()));
            }
            assertEquals(oppositeCursor,learning()?disk.getString("itqanCursor",""):
                String.valueOf(disk.getInt("sabqiLineCursor",-1)));
            assertEquals(date.toString(),learning()?prefs.lastSabqiDate():prefs.lastItqanDate());
            assertFalse("Morning work alone cannot enter Sunday's final",lines(finals(prefs,sunday)).containsAll(block));
            List<ConsolidationCycleEngine.Unit> due=evening(prefs,date);
            assertFalse(due.isEmpty());
            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
            ConsolidationCycleEngine.Session session=open(due,prefix()+"-"+date);
            assertTrue(prefs.persistConsolidationSession(session));
            Map<String,Object> before=new LinkedHashMap<>(disk.disk);
            try {
                prefs.completeSnowballEvening(family,date.toString(),session);
                fail("Incomplete repetitions cannot earn an evening receipt");
            } catch(IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("Complete frozen evening proof required"));
            }
            assertEquals(before,disk.disk);
            session=engine.recordRepetition(session);assertTrue(prefs.persistConsolidationSession(session));
            disk=backupRoundTrip(disk);prefs=new HifzPrefs(disk);
            ConsolidationCycleEngine.Session restored=prefs.restoreConsolidationSession(engine,family);
            assertEquals(session.unitIds(),restored.unitIds());
            assertEquals(session.stage(),restored.stage());
            assertEquals(session.donePerStage(),restored.donePerStage());
            assertEquals(date.toString(),prefs.unfinishedConsolidationSessionDate(family,prefix()+"-"));
            if(learning()&&!completed.isEmpty()) {
                ConsolidationCycleEngine.Session uncheckpointed=restored;
                while(!uncheckpointed.readyToClose())uncheckpointed=engine.recordRepetition(uncheckpointed);
                Map<String,Object> frozenDisk=new LinkedHashMap<>(disk.disk);
                assertFalse("A cumulative overlap requires the persisted completed session",
                    prefs.completeSnowballEvening(family,date.toString(),uncheckpointed));
                assertEquals(frozenDisk,disk.disk);
            }
            while(!restored.readyToClose()) {
                restored=engine.recordRepetition(restored);assertTrue(prefs.persistConsolidationSession(restored));
            }
            assertTrue(prefs.completeSnowballEvening(family,date.toString(),restored));
            Map<String,Object> once=new LinkedHashMap<>(disk.disk);
            assertFalse(prefs.completeSnowballEvening(family,date.toString(),restored));assertEquals(once,disk.disk);
            completed.addAll(block);cursor+=count;
            assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
        }
        disk=backupRoundTrip(disk);prefs=new HifzPrefs(disk);
        List<ConsolidationCycleEngine.Unit> eligible=finals(prefs,sunday);
        assertEquals("Sunday consists exactly of the genuinely attended and receipted lines",completed,lines(eligible));
        if(completed.isEmpty()) {assertTrue(eligible.isEmpty());assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());return;}
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Session session=open(eligible,prefix()+"-final-"+sunday);
        session=engine.recordRepetition(session);assertTrue(prefs.persistConsolidationSession(session));
        disk=backupRoundTrip(disk);prefs=new HifzPrefs(disk);
        session=prefs.restoreConsolidationSession(engine,family,prefix()+"-final-"+sunday);
        assertNotNull(session);
        while(!session.readyToClose()) {session=engine.recordRepetition(session);assertTrue(prefs.persistConsolidationSession(session));}
        assertTrue(learning()?prefs.completeLearningConsolidationSessionV6(session,geometry,sunday.toString(),"stress"):
            prefs.completeConsolidationSessionV6(session,geometry,sunday.toString(),"stress"));
        Map<String,Object> once=new LinkedHashMap<>(disk.disk);
        assertFalse(learning()?prefs.completeLearningConsolidationSessionV6(session,geometry,sunday.toString(),"duplicate"):
            prefs.completeConsolidationSessionV6(session,geometry,sunday.toString(),"duplicate"));
        assertEquals(once,disk.disk);
        disk=backupRoundTrip(disk);prefs=new HifzPrefs(disk);
        assertEquals(completed,prefs.progressionSnapshotV6().acquired);
        assertTrue(prefs.progressionSnapshotV6().learned.isEmpty());assertTrue(prefs.progressionSnapshotV6().stabilized.isEmpty());
        assertTrue(finals(prefs,sunday).isEmpty());assertTrue(finals(prefs,sunday.plusWeeks(1)).isEmpty());
    }
}
