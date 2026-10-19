package com.quransafeguard.hifz.preview;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Pinned approved subset only. Audit candidates and untrusted approval flags never activate UI. */
final class CorrespondenceCatalog {
 final List<Relation> relations;
 final Index index;
 private CorrespondenceCatalog(List<Relation> rows){
  relations=Collections.unmodifiableList(new ArrayList<>(rows));index=new Index(relations);
 }
 static CorrespondenceCatalog empty(){return new CorrespondenceCatalog(Collections.emptyList());}
 static CorrespondenceCatalog parse(byte[] bytes,String trustedSha256,Geometry geometry)throws Exception{
  require(bytes!=null&&bytes.length<=1048576&&geometry!=null,"catalogue size/geometry");
  require(trustedSha256!=null&&trustedSha256.matches("[a-f0-9]{64}"),"missing trusted catalogue pin");
  StringBuilder hash=new StringBuilder();
  for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  require(hash.toString().equals(trustedSha256),"catalogue integrity mismatch");
  JSONObject root=new JSONObject(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString());
  require("HAAFIDH_APPROVED_CORRESPONDENCES_V1".equals(text(root,"schema")),"catalogue schema");
  require(integer(root,"revision")>0,"catalogue revision");
  JSONArray entries=root.getJSONArray("entries");require(entries.length()<=110,"catalogue subset size");
  List<Relation> rows=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int k=0;k<entries.length();k++){
   JSONObject e=entries.getJSONObject(k);String id=text(e,"id");require(ids.add(id),"duplicate relation");
   JSONObject gates=e.getJSONObject("gates"),rights=e.getJSONObject("rights");
   Gates approved=new Gates(bool(gates,"source"),bool(gates,"classification"),bool(gates,"anchors"),bool(gates,"visual"),bool(gates,"qa"));
   require(approved.allApproved(),"uncertified relation");
   require(bool(rights,"authorized"),"missing rights authorization");
   String usage=text(rights,"usage");require("PERSONAL_OFFLINE_ONLY".equals(usage)||"PUBLIC_DISTRIBUTION".equals(usage),"unsupported rights scope");
   text(rights,"evidence");
   JSONArray originals=e.getJSONArray("originals"),anchors=e.getJSONArray("anchors");
   require(originals.length()<=110&&anchors.length()<=110,"relation size");
   List<VerseRange> ranges=new ArrayList<>();for(int j=0;j<originals.length();j++)ranges.add(range(originals.getJSONObject(j),geometry));
   List<Anchor> focal=new ArrayList<>();for(int j=0;j<anchors.length();j++){
    JSONObject a=anchors.getJSONObject(j);focal.add(new Anchor(integer(a,"original_index"),range(a.getJSONObject("range"),geometry),bool(a,"source_attested"),text(a,"source_locator"),integer(a,"first_word"),integer(a,"last_word")));
   }
   JSONObject p=e.getJSONObject("presentation");JSONArray sources=p.getJSONArray("sources");require(sources.length()<=32,"source size");
   List<SourceReference> refs=new ArrayList<>();for(int j=0;j<sources.length();j++){
    JSONObject s=sources.getJSONObject(j);refs.add(new SourceReference(text(s,"author"),text(s,"work"),text(s,"edition"),text(s,"locator")));
   }
   Relation relation=new Relation(id,Type.valueOf(text(e,"type")),Mode.valueOf(text(e,"mode")),ranges,focal,approved,geometry,new Presentation(text(p,"short_title"),text(p,"factual_link"),refs));
   require(CorrespondenceDocument.displayable(relation,geometry),"unsupported relation rendering");rows.add(relation);
  }
  return new CorrespondenceCatalog(rows);
 }
 private static VerseRange range(JSONObject r,Geometry g)throws Exception{return new VerseRange(integer(r,"surah"),integer(r,"start"),integer(r,"end"),g);}
 private static String text(JSONObject o,String k)throws Exception{Object v=o.get(k);require(v instanceof String&&!((String)v).trim().isEmpty()&&((String)v).length()<=2000,"invalid text "+k);return (String)v;}
 private static boolean bool(JSONObject o,String k)throws Exception{Object v=o.get(k);require(v instanceof Boolean,"invalid boolean "+k);return (Boolean)v;}
 private static int integer(JSONObject o,String k)throws Exception{Object v=o.get(k);require(v instanceof Integer||v instanceof Long,"invalid integer "+k);long n=((Number)v).longValue();require(n>=0&&n<=Integer.MAX_VALUE,"integer overflow");return (int)n;}
 private static void require(boolean c,String m){if(!c)throw new IllegalArgumentException(m);}
}
