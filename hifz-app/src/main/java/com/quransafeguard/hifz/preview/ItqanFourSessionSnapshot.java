package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/** Versioned, immutable, fail-closed frozen 2-page / 4-session Itqan window.
 * A partially completed window is NEVER replanned from current corpus.
 * This is a durable model; enabling it requires atomic session/V6 integration.
 */
final class ItqanFourSessionSnapshot {
 static final int VERSION=1;
 final ItqanFourSessionPolicy.Plan plan;
 final String originLeg,originCursor,scheduledWeek;
 final boolean originalTailCompleted;
 final int sessionIndex,blockIndex,reps;
 final String fingerprint;

 private ItqanFourSessionSnapshot(ItqanFourSessionPolicy.Plan plan,String leg,
       String cursor,String week,boolean tail,int slot,int block,int repetitions,String digest){
   this.plan=Objects.requireNonNull(plan);
   this.originLeg=Objects.requireNonNull(leg);
   this.originCursor=Objects.requireNonNull(cursor);
   this.scheduledWeek=Objects.requireNonNull(week);
   this.originalTailCompleted=tail;
   this.sessionIndex=slot;this.blockIndex=block;this.reps=repetitions;
   this.fingerprint=digest;
 }

 static ItqanFourSessionSnapshot freeze(ItqanFourSessionPolicy.Plan plan,
             ItqanRotationPolicy.State rotation,LocalDate week,GeometryRepository geo) {
   if(rotation==null||week==null)throw new IllegalArgumentException("Itqan origin required");
   ItqanFourSessionSnapshot snap=new ItqanFourSessionSnapshot(plan,rotation.leg.name(),
      rotation.cursor.toString(),week.toString(),rotation.initialTailCompleted,0,0,0,null);
   snap.validate(geo);
   return snap.withDigest();
 }

 private ItqanFourSessionSnapshot withDigest(){
   return new ItqanFourSessionSnapshot(plan,originLeg,originCursor,scheduledWeek,
      originalTailCompleted,sessionIndex,blockIndex,reps,digest(plan,originLeg,originCursor,scheduledWeek,originalTailCompleted));
 }

 private static String digest(ItqanFourSessionPolicy.Plan plan,String leg,String cursor,
                              String week,boolean latched){
   StringBuilder b=new StringBuilder("ITQAN4|V1|").append(plan.firstPage).append('|')
      .append(plan.lastPage).append('|').append(plan.fullWindow).append('|')
      .append(leg).append('|').append(cursor).append('|').append(week)
      .append('|').append(latched);
   for(ItqanFourSessionPolicy.Session session:plan.sessions){
     b.append("|S").append(session.index);
     for(ItqanFourSessionPolicy.Block block:session.blocks){
       b.append("|B").append(block.surah);
       for(String id:block.lineIds)b.append(',').append(id);
     }
   }
   try{
     byte[] h=MessageDigest.getInstance("SHA-256")
         .digest(b.toString().getBytes(StandardCharsets.UTF_8));
     StringBuilder hex=new StringBuilder(64);
     for(byte v:h)hex.append(String.format(Locale.ROOT,"%02x",v&255));
     return hex.toString();
   }catch(Exception e){throw new IllegalStateException("Cannot fingerprint Itqan window",e);}
 }

 String encode(){
   try{
     JSONObject data=new JSONObject();
     data.put("version",VERSION);
     data.put("firstPage",plan.firstPage).put("lastPage",plan.lastPage)
       .put("fullWindow",plan.fullWindow).put("originLeg",originLeg)
       .put("originCursor",originCursor).put("scheduledWeek",scheduledWeek)
       .put("tailCompletedAtStart",originalTailCompleted)
       .put("sessionIndex",sessionIndex).put("blockIndex",blockIndex)
       .put("reps",reps).put("sha256",fingerprint);
     JSONArray sessions=new JSONArray();
     for(ItqanFourSessionPolicy.Session s:plan.sessions){
       JSONObject slot=new JSONObject();slot.put("index",s.index).put("lineCount",s.lineCount);
       JSONArray parts=new JSONArray();
       for(ItqanFourSessionPolicy.Block b:s.blocks){
         JSONArray ids=new JSONArray();for(String id:b.lineIds)ids.put(id);
         parts.put(new JSONObject().put("surah",b.surah).put("lineIds",ids));
       }
       slot.put("blocks",parts);sessions.put(slot);
     }
     data.put("sessions",sessions);
     return data.toString();
   }catch(Exception e){throw new IllegalStateException("Cannot persist Itqan window",e);}
 }

 static ItqanFourSessionSnapshot decode(String raw,GeometryRepository geometry){
   if(raw==null||raw.isEmpty())throw new IllegalStateException("Missing frozen Itqan plan");
   try{
     JSONObject obj=new JSONObject(raw);
     if(obj.getInt("version")!=VERSION)throw new IllegalStateException("Unknown Itqan snapshot version");
     JSONArray rawSlots=obj.getJSONArray("sessions");
     if(rawSlots.length()!=4)throw new IllegalStateException("Exactly four Itqan slots required");
     ArrayList<ItqanFourSessionPolicy.Session> slots=new ArrayList<>();
     for(int i=0;i<4;i++){
       JSONObject slot=rawSlots.getJSONObject(i);
       if(slot.getInt("index")!=i+1)throw new IllegalStateException("Reordered date slot");
       JSONArray parts=slot.getJSONArray("blocks");
       ArrayList<ItqanFourSessionPolicy.Block> blocks=new ArrayList<>();
       int count=0;
       for(int k=0;k<parts.length();k++){
         JSONObject part=parts.getJSONObject(k);
         JSONArray idsJson=part.getJSONArray("lineIds");
         ArrayList<String> ids=new ArrayList<>();
         for(int l=0;l<idsJson.length();l++)ids.add(idsJson.getString(l));
         count+=ids.size();
         blocks.add(new ItqanFourSessionPolicy.Block(part.getInt("surah"),ids));
       }
       if(count!=slot.getInt("lineCount"))throw new IllegalStateException("Stale Itqan slot count");
       slots.add(new ItqanFourSessionPolicy.Session(i+1,blocks,count));
     }
     ItqanFourSessionPolicy.Plan plan=new ItqanFourSessionPolicy.Plan(
        obj.getInt("firstPage"),obj.getInt("lastPage"),obj.getBoolean("fullWindow"),slots);
     ItqanFourSessionSnapshot result=new ItqanFourSessionSnapshot(plan,
       obj.getString("originLeg"),obj.getString("originCursor"),obj.getString("scheduledWeek"),
       obj.getBoolean("tailCompletedAtStart"),obj.getInt("sessionIndex"),
       obj.getInt("blockIndex"),obj.getInt("reps"),obj.getString("sha256"));
     result.validate(geometry);
     return result;
   }catch(Exception malformed){
     throw new IllegalStateException("Corrupt frozen Itqan snapshot; do not replan",malformed);
   }
 }

 private void validate(GeometryRepository geo){
   if(geo==null||plan.sessions.size()!=4||plan.firstPage<1||plan.lastPage>604
      ||plan.lastPage<plan.firstPage||plan.lastPage>plan.firstPage+1
      ||sessionIndex<0||sessionIndex>4||blockIndex<0||reps<0||reps>40)
     throw new IllegalStateException("Invalid Itqan snapshot coordinates");
   ItqanRotationPolicy.Leg.valueOf(originLeg);
   GeometryRepository.parseVerse(originCursor);
   LocalDate.parse(scheduledWeek);
   LinkedHashSet<String> seen=new LinkedHashSet<>();
   int lastGlobal=-1;
   for(ItqanFourSessionPolicy.Session slot:plan.sessions){
     int length=0;
     for(ItqanFourSessionPolicy.Block block:slot.blocks){
       if(block.lineIds.isEmpty()||block.lineIds.size()>11)
         throw new IllegalStateException("Invalid same-surah subblock size");
       List<GeometryRepository.LineMeta> physical=geo.linesForExactIds(block.lineIds);
       if(physical.size()!=block.lineIds.size())throw new IllegalStateException("Unknown QCF lines");
       for(GeometryRepository.LineMeta line:physical){
         if(!seen.add(line.id)||lastGlobal!=-1&&line.globalIndex!=lastGlobal+1
            ||line.page<plan.firstPage||line.page>plan.lastPage)
           throw new IllegalStateException("Repeated, missing or shifted QCF line");
         lastGlobal=line.globalIndex;length++;
         for(VerseRef v:line.verses)if(v.getSurah()!=block.surah)
           throw new IllegalStateException("Itqan block crosses a surah");
       }
     }
     if(length!=slot.lineCount)throw new IllegalStateException("Wrong QCF session quota");
   }
   if(seen.isEmpty()||plan.lines()!=seen.size())
      throw new IllegalStateException("Itqan coverage mismatch");
   if(sessionIndex<4&&blockIndex>plan.sessions.get(sessionIndex).blocks.size())
      throw new IllegalStateException("Invalid Itqan subblock cursor");
   if(sessionIndex==4&&(blockIndex!=0||reps!=0))
      throw new IllegalStateException("Finished snapshot still carries reps");
   String computed=digest(plan,originLeg,originCursor,scheduledWeek,originalTailCompleted);
   if(fingerprint!=null&&!fingerprint.equals(computed))
      throw new IllegalStateException("Itqan geometry/frozen source signature differs");
 }

 ItqanFourSessionSnapshot withProgress(int slot,int block,int repetition,GeometryRepository geo){
   ItqanFourSessionSnapshot next=new ItqanFourSessionSnapshot(plan,originLeg,originCursor,
      scheduledWeek,originalTailCompleted,slot,block,repetition,fingerprint);
   next.validate(geo);
   return next;
 }
}
