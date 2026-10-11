package com.quransafeguard.hifz.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Restart and tampering contract for immutable four-date Itqan snapshots. */
public final class ItqanFourSessionSnapshotTest {
 static GeometryRepository geometry;
 @BeforeClass public static void init()throws Exception {
   Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
   if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
   geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
 }
 private static ItqanFourSessionSnapshot sample(int page) {
   List<String> ids=new ArrayList<>(geometry.lineIdsOnPage(page));
   if(page<604)ids.addAll(geometry.lineIdsOnPage(page+1));
   List<GeometryRepository.LineMeta> ls=geometry.linesForExactIds(ids);
   ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.plan(ls,page<604);
   return ItqanFourSessionSnapshot.freeze(plan,ItqanRotationPolicy.State.startOfTail(),
       LocalDate.of(2026,10,12),geometry);
 }
 @Test public void actualQafDhariyatAndLastNasPageSurviveRestartWithoutReplanning() {
   for(int p:new int[]{520,603,604}){
     ItqanFourSessionSnapshot original=sample(p);
     String exact=original.encode();
     ItqanFourSessionSnapshot recovered=ItqanFourSessionSnapshot.decode(exact,geometry);
     assertEquals(original.plan.lines(),recovered.plan.lines());
     assertEquals(original.plan.fullWindow,recovered.plan.fullWindow);
     assertEquals(exact,recovered.encode());
     assertEquals(original.fingerprint,recovered.fingerprint);
   }
 }
 @Test public void partiallyCompletedSnapshotPreservesExactSlotAndRep() {
   ItqanFourSessionSnapshot original=sample(520);
   ItqanFourSessionSnapshot inProgress=original.withProgress(1,0,27,geometry);
   ItqanFourSessionSnapshot read=ItqanFourSessionSnapshot.decode(inProgress.encode(),geometry);
   assertEquals(1,read.sessionIndex);assertEquals(0,read.blockIndex);assertEquals(27,read.reps);
   assertEquals(original.fingerprint,read.fingerprint);
 }
 @Test(expected=IllegalStateException.class)
 public void corruptLineIdentityIsNeverSilentlyRecomputed()throws Exception{
   JSONObject json=new JSONObject(sample(520).encode());
   JSONArray parts=json.getJSONArray("sessions").getJSONObject(0).getJSONArray("blocks");
   parts.getJSONObject(0).getJSONArray("lineIds").put(0,"p999-l99");
   ItqanFourSessionSnapshot.decode(json.toString(),geometry);
 }
 @Test(expected=IllegalStateException.class)
 public void alteredProgressOutside40RepsMustFail()throws Exception{
   JSONObject json=new JSONObject(sample(603).encode());
   json.put("reps",41);
   ItqanFourSessionSnapshot.decode(json.toString(),geometry);
 }
 @Test(expected=IllegalStateException.class)
 public void alteredSourceFingerprintMustFail()throws Exception{
   JSONObject json=new JSONObject(sample(603).encode());
   json.put("sha256","0000");
   ItqanFourSessionSnapshot.decode(json.toString(),geometry);
 }
}
