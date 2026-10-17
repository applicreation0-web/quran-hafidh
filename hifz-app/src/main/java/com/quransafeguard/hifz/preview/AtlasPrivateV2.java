package com.quransafeguard.hifz.preview;

import android.content.Context;
import com.quransafeguard.hifz.core.VerseRef;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Original bibliographic metadata. Does not alter Quran, Ayah Themes, or Hifz state. */
final class AtlasPrivateV2 {
 static final String INDEX_ASSET="atlas_private/atlas_366_comments_offline_reference_v1.json";
 static final String PDF_ASSET="atlas_private/maududi_original.pdf";
 static final String INDEX_SHA256="8f514ffa367d67a49198799be279691ba0555363fac0b030f09fbf39fe3c3484";
 static final String PDF_SHA256="1d207bb3dd58d45e7a77b0b5942f88a5c216b3a4273ea5a4474fc9e140b0d62e";
 static final String SCHEMA="HAafidh_ATLAS_PRIVATE_COMMENTARY_SOURCES_V1";
 static final class Unit {
  final int surah,start,end,ordinal,pdfStart,pdfEnd;
  final String id,bibliography,sourceStatus,offlineAuthor;
  Unit(int s,int a,int b,int o,String uid,int pa,int pb,String bi,String ss,String au){
   surah=s;start=a;end=b;ordinal=o;id=uid;pdfStart=pa;pdfEnd=pb;
   bibliography=bi;sourceStatus=ss;offlineAuthor=au;
  }
 }
 private final List<Unit> all;
 private AtlasPrivateV2(List<Unit> list){all=Collections.unmodifiableList(list);}
 List<Unit> units(){return all;}
 Unit at(int s,int ayah){
  for(Unit u:all)if(u.surah==s&&u.start<=ayah&&ayah<=u.end)return u;
  throw new IllegalArgumentException("Unknown Atlas location "+s+":"+ayah);
 }
 static String sha256(byte[] bytes)throws Exception {
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
  StringBuilder result=new StringBuilder();
  for(byte b:digest)result.append(String.format(Locale.ROOT,"%02x",b&255));
  return result.toString();
 }
 static AtlasPrivateV2 load(Context c,MawdudiPassageIndex passages)throws Exception{
  try(InputStream in=c.getAssets().open(INDEX_ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){
    if(out.size()+n>1000000)throw new IllegalStateException("Atlas index too large");
    out.write(b,0,n);
   }
   byte[] raw=out.toByteArray();
   if(!INDEX_SHA256.equals(sha256(raw)))throw new IllegalStateException("Atlas index SHA-256 mismatch");
   return parse(new String(raw,StandardCharsets.UTF_8),passages,GeometryRepository.get(c));
  }
 }
 static AtlasPrivateV2 parse(String raw,MawdudiPassageIndex passages,GeometryRepository qcf)throws Exception{
  JSONObject root=new JSONObject(raw);
  if(!SCHEMA.equals(root.getString("schema"))||root.getInt("unit_count")!=366
     ||root.getInt("surah_count")!=114||!root.getBoolean("original_source_only")
     ||root.getBoolean("generative_commentary")||!root.getBoolean("frozen_qcf_and_ayah_themes"))
   throw new IllegalStateException("Unapproved Atlas index");
  JSONObject pdf=root.getJSONObject("offline_maududi_pdf");
  if(!PDF_SHA256.equals(pdf.getString("sha256"))||pdf.getInt("pdf_pages")!=3117
    ||!pdf.getBoolean("copy_only_to_private_android_assets"))
   throw new IllegalStateException("Unapproved Maududi source");
  JSONArray rows=root.getJSONArray("units");
  if(rows.length()!=366)throw new IllegalStateException("366 units required");
  List<Unit> units=new ArrayList<>(366);
  int s=1,next=1,ordinal=0;
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.getJSONObject(i);
   int rowS=row.getInt("surah"), a=row.getInt("from"),b=row.getInt("to");
   if(rowS!=s){
    if(rowS!=s+1||next!=passages.forSurah(s).get(passages.forSurah(s).size()-1).lastAyah+1)
     throw new IllegalStateException("Missing Atlas boundary");
    s=rowS;next=1;ordinal=0;
   }
   if(a!=next||b<a||b>passages.forSurah(s).get(passages.forSurah(s).size()-1).lastAyah
      ||row.getInt("order_in_surah")!=ordinal+1)
    throw new IllegalStateException("Overlapping Atlas range");
   String id=row.getString("unit_id");
   if(!id.equals(String.format(Locale.ROOT,"s%03da%03db%03d",s,a,b)))
    throw new IllegalStateException("Atlas ID changed");
   JSONObject origin=row.getJSONObject("primary_reference");
   if(origin.optString("bibliography").trim().isEmpty()||origin.optString("verification_status").trim().isEmpty())
    throw new IllegalStateException("Missing original attribution");
   JSONObject source=row.getJSONObject("offline_commentary");
   int pA=source.getInt("pdf_page_first_1based"),pB=source.getInt("pdf_page_last_1based");
   if(pA<1||pB<pA||pB>3117||!"SOURCE_ORIGINAL_PDF_NO_AI_TEXT".equals(source.getString("kind")))
    throw new IllegalStateException("Bad source locator");
   JSONObject mushaf=row.getJSONObject("qcf");
   int first=qcf.pageForVerse(new VerseRef(s,a));
   int last=MawdudiMapModel.lastPageForVerse(new VerseRef(s,b),qcf);
   if(mushaf.getInt("first_page")!=first||mushaf.getInt("last_page")!=last)
    throw new IllegalStateException("QCF page mapping mismatch");
   JSONArray pages=mushaf.getJSONArray("pages");
   if(pages.length()!=last-first+1)throw new IllegalStateException("QCF page count mismatch");
   for(int k=0;k<pages.length();k++)if(pages.getInt(k)!=first+k)
    throw new IllegalStateException("QCF page noncontiguous");
   JSONArray ids=row.getJSONArray("ayah_themes_ids");
   if(ids.length()==0)throw new IllegalStateException("Missing Ayah Themes stations");
   for(int k=0;k<ids.length();k++){
    String pid=ids.getString(k);boolean valid=false;
    for(MawdudiPassageIndex.Passage p:passages.forSurah(s))
     if(p.id.equals(pid)&&p.firstAyah<=b&&p.lastAyah>=a){valid=true;break;}
    if(!valid)throw new IllegalStateException("Ayah Themes ID changed");
   }
   units.add(new Unit(s,a,b,ordinal+1,id,pA,pB,
     origin.getString("bibliography"),origin.getString("verification_status"),source.getString("author")));
   next=b+1;ordinal++;
  }
  if(s!=114||next!=passages.forSurah(114).get(passages.forSurah(114).size()-1).lastAyah+1)
   throw new IllegalStateException("An-Nas not covered");
  return new AtlasPrivateV2(units);
 }
}