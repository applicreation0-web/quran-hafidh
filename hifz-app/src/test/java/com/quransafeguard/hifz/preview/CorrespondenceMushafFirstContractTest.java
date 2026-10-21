package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Android-level delivery gates for the page-first Correspondances entry point.
 * Source contracts complement, never replace, instrumented BOOX visual tests. */
public final class CorrespondenceMushafFirstContractTest {
 private static String code(String file)throws Exception {
  Path p=Paths.get(file);if(!Files.exists(p))p=Paths.get("..",file);
  return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
 }
 private static final String ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";
 @Test public void homeEntersTheQcfMushafNotTheHistoricalDirectory()throws Exception{
  String home=code(ROOT+"MainActivity.java");
  String manifest=code("hifz-app/src/main/AndroidManifest.xml");
  assertTrue(home.contains("new Intent(this, CorrespondenceMushafActivity.class)"));
  assertFalse(home.contains("new Intent(this, CorrespondenceDirectoryActivity.class)"));
  assertTrue(manifest.contains("android:name=\".CorrespondenceMushafActivity\""));
 }
 @Test public void originalQcfFolioIsOpeningSurface()throws Exception{
  String screen=code(ROOT+"CorrespondenceMushafActivity.java");
  String document=code(ROOT+"CorrespondenceDocument.java");
  assertTrue(screen.contains("getSharedPreferences(\"hifz_study\",MODE_PRIVATE).getInt(\"page\",1)"));
  assertTrue(screen.contains("full.show(CorrespondenceDocument.plainPage(page),null)"));
  assertTrue(screen.contains("full.setPageListener(p->"));
  assertTrue(document.contains("if(focus.plainPage)return new Render(CorrespondenceSvgFocus.untouched(svg),true);"));
  assertTrue(document.contains("if(page<1||page>604)throw"));
  assertFalse("A read-only explorer cannot alter Hifz progress",screen.contains(".edit()."));
 }
 @Test public void historicalLinksMustUseExactVerifiedPrivateBounds()throws Exception{
  String screen=code(ROOT+"CorrespondenceMushafActivity.java");
  assertTrue(screen.contains("SHA.equals(sha.toString())"));
  assertTrue(screen.contains("rows.length()!=110"));
  assertTrue(screen.contains("last>QuranCanon.INSTANCE.ayahCount(surah)"));
  assertTrue(screen.contains("MawdudiMapModel.lastPageForVerse(new VerseRef(surah,last),geometry)"));
  assertTrue(screen.contains("if(!onPage.contains(e))onPage.add(e)"));
  assertTrue(screen.contains("if(!SHA.equals(sha.toString()))throw"));
 }
 @Test public void twoFoliosOnlyWhenPhysicalLocationsDiffer()throws Exception{
  String screen=code(ROOT+"CorrespondenceMushafActivity.java");
  assertTrue(screen.contains("int secondary=series?(begin+1)%ranges.length():begin+1;"));
  assertTrue(screen.contains("int count=samePage?1:requested;"));
  assertTrue(screen.contains("focus=samePage"));
  assertTrue(screen.contains("combined.focalRanges=discrete"));
  assertTrue(screen.contains("remembered.get(position)"));
 }
 @Test public void eightGlyphsRemainReadOnlyProposalsUntilCertified()throws Exception{
  String screen=code(ROOT+"CorrespondenceMushafActivity.java");
  assertTrue(screen.contains("CorrespondenceScreen.icon(CorrespondenceModel.Type.valueOf(type))"));
  assertTrue(screen.contains("symbol.setImageTintList(null)"));
  assertTrue(screen.contains("runtime.find(row.optString(\"id\"))!=null"));
  assertTrue(screen.contains(" · proposition non certifiée"));
  assertTrue(screen.contains("series=\"REPETITION\".equals(active.optString(\"type\"))"));
  assertTrue(screen.contains("renderOccurrenceRail(ranges)"));
 }
}
