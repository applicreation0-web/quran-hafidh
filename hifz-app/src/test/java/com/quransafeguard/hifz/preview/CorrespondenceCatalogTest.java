package com.quransafeguard.hifz.preview;
import org.junit.Test;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;
/** All metadata/approvals in this file are synthetic fixtures, never production approvals. */
public final class CorrespondenceCatalogTest {
 static final Geometry G=new Geometry(){public int ayahCount(int s){return 40;}public int firstPage(int s,int a){return s;}public int lastPage(int s,int a){return s;}public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}};
 static JSONObject range(int s,int a)throws Exception{return new JSONObject().put("surah",s).put("start",a).put("end",a);}
 static JSONObject entry(String id)throws Exception{
  JSONObject a=range(2,2),b=range(3,3);
  return new JSONObject().put("id",id).put("type","RESSEMBLANCE").put("mode","TWO_PAGES")
   .put("originals",new JSONArray().put(a).put(b)).put("anchors",new JSONArray()
    .put(new JSONObject().put("original_index",0).put("range",a).put("source_attested",true).put("source_locator","SYNTHETIC locator").put("first_word",0).put("last_word",0))
    .put(new JSONObject().put("original_index",1).put("range",b).put("source_attested",true).put("source_locator","SYNTHETIC locator").put("first_word",0).put("last_word",0)))
   .put("gates",new JSONObject().put("source",true).put("classification",true).put("anchors",true).put("visual",true).put("qa",true))
   .put("rights",new JSONObject().put("authorized",true).put("usage","PERSONAL_OFFLINE_ONLY").put("evidence","SYNTHETIC permission"))
   .put("presentation",new JSONObject().put("short_title","SYNTHETIC title").put("factual_link","SYNTHETIC factual link").put("sources",new JSONArray().put(new JSONObject().put("author","SYNTHETIC author").put("work","SYNTHETIC work").put("edition","SYNTHETIC edition").put("locator","SYNTHETIC locator"))));
 }
 static byte[] pack(JSONObject... entries)throws Exception{JSONArray a=new JSONArray();for(JSONObject e:entries)a.put(e);return new JSONObject().put("schema","HAAFIDH_APPROVED_CORRESPONDENCES_V1").put("revision",1).put("entries",a).toString().getBytes(StandardCharsets.UTF_8);}
 static String sha(byte[] b)throws Exception{StringBuilder h=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(b))h.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return h.toString();}
 static CorrespondenceCatalog load(byte[] b)throws Exception{return CorrespondenceCatalog.parse(b,sha(b),G);}
 interface Invalid {void run()throws Exception;}
 static void reject(Invalid f)throws Exception{try{f.run();fail("Untrusted catalogue accepted");}catch(IllegalArgumentException expected){}}
 @Test public void pinnedSubsetCreatesRealDirectIndex()throws Exception{CorrespondenceCatalog c=load(pack(entry("SYNTHETIC-1")));assertEquals(1,c.relations.size());assertEquals(1,c.index.at(2,2).size());assertTrue(c.index.at(2,1).isEmpty());}
 @Test public void emptyApprovedSetIsNotAnActivation()throws Exception{assertTrue(load(pack()).relations.isEmpty());}
 @Test public void missingOrDifferentTrustedPinCannotActivateEvenFullyApprovedFlags()throws Exception{byte[] b=pack(entry("SYNTHETIC-1"));reject(()->CorrespondenceCatalog.parse(b,"",G));reject(()->CorrespondenceCatalog.parse(b,new String(new char[64]).replace('\0','0'),G));}
 @Test public void eachUncertifiedGateRejectsPackage()throws Exception{for(String gate:new String[]{"source","classification","anchors","visual","qa"}){JSONObject e=entry("SYNTHETIC-1");e.getJSONObject("gates").put(gate,false);reject(()->load(pack(e)));}}
 @Test public void approvalStringsAreNotBooleans()throws Exception{JSONObject e=entry("SYNTHETIC-1");e.getJSONObject("gates").put("qa","true");reject(()->load(pack(e)));}
 @Test public void rightsAndEvidenceAreRequiredIndependentlyOfCertification()throws Exception{JSONObject e=entry("SYNTHETIC-1");e.getJSONObject("rights").put("authorized",false);reject(()->load(pack(e)));e.getJSONObject("rights").put("authorized",true).put("evidence","");reject(()->load(pack(e)));}
 @Test public void unsupportedRendererCannotEnterRuntimeCatalogue()throws Exception{JSONObject e=entry("SYNTHETIC-1");e.put("type","CENTRE");reject(()->load(pack(e)));}
 @Test public void duplicateIdsAndEmptyBibliographyAreRejected()throws Exception{reject(()->load(pack(entry("SYNTHETIC-1"),entry("SYNTHETIC-1"))));JSONObject e=entry("SYNTHETIC-1");e.getJSONObject("presentation").put("sources",new JSONArray());reject(()->load(pack(e)));}
 @Test public void resultIsImmutable()throws Exception{CorrespondenceCatalog c=load(pack(entry("SYNTHETIC-1")));try{c.relations.clear();fail("Mutable");}catch(UnsupportedOperationException expected){}}
}
