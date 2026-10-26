package com.quransafeguard.hifz.preview;
import org.junit.Test;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public final class LateSabqiFinalDateTest {
 @Test public void unstartedFinalKeepsOriginalSundayAcrossSeveralWeeksWithoutWrites()throws Exception {
   InMemoryPrefs db=LongHorizonHifzPersistenceStressTest.freshV6Disk();
   String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("16:0","16:1"));
   db.disk.put("learningSnowball8WeekHistory",new JSONArray().put(new JSONObject()
       .put("week","2026-10-19").put("units",new JSONArray().put(unit))).toString());
   Map<String,?> before=db.getAll();
   for(String day:new String[]{"2026-10-25","2026-10-26","2026-10-27","2026-11-02","2026-11-09"})
     assertEquals(LocalDate.parse("2026-10-25"),new HifzPrefs(db).pendingLearningFinalDate(LocalDate.parse(day)));
   assertNull(new HifzPrefs(db).pendingLearningFinalDate(LocalDate.parse("2026-10-24")));
   assertEquals("Reading overdue dates must never manufacture receipts or credits",before,db.getAll());
   db.disk.put("lastLearningConsolidationDate","2026-10-25");
   assertEquals("Uncredited capped history must wait for a new eligible Sunday",
       LocalDate.parse("2026-11-01"),new HifzPrefs(db).pendingLearningFinalDate(LocalDate.parse("2026-11-09")));
   assertNull(new HifzPrefs(db).pendingLearningFinalDate(LocalDate.parse("2026-10-26")));
   db.disk.put("v6AcquiredCreditLineIds","[\"16:0\",\"16:1\"]");
   assertNull("Completed physical corpus cannot reappear",new HifzPrefs(db).pendingLearningFinalDate(LocalDate.parse("2026-11-09")));
 }
}
