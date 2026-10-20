package com.quransafeguard.hifz.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** No old Ibn Kathir view can be reached by the user-facing Atlas home action. */
public final class AtlasActiveEntrySourceContractTest {
 private static String source(String relative)throws Exception {
  for(String prefix:new String[]{"","../","quran-hafidh/"}) {
   Path file=Paths.get(prefix+relative);
   if(Files.isRegularFile(file))return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
  }
  throw new IllegalStateException("Source contract file missing: "+relative);
 }
 @Test public void homeOpensTheNewAtlasActivityNotTheLegacyIbnKathirMap()throws Exception {
  String home=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
  String manifest=source("hifz-app/src/main/AndroidManifest.xml");
  assertTrue(home.contains("homeNavLine(root, \"Atlas\""));
  assertTrue(home.contains("new Intent(this, AtlasMapActivity.class)"));
  assertFalse(home.contains("new Intent(this, IbnKathirMapActivity.class)"));
  assertTrue(manifest.contains("<activity android:name=\".AtlasMapActivity\" android:exported=\"false\""));
 }
 @Test public void verifiedPrivateAtlasNeverFallsBackToIbnKathirOrMawdudiCommentary()throws Exception {
  String atlas=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasMapActivity.java");
  String unitReader=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MawdudiMapScreen.java");
  assertTrue(atlas.contains("!BuildConfig.AYAH_THEMES_FINAL_PRIVATE"));
  assertTrue(atlas.contains("new MawdudiMapScreen("));
  assertTrue(atlas.contains("atlas.returnFromReader("));
  assertTrue(atlas.contains("atlas.save(out)"));
  assertTrue(atlas.contains("atlas.destroy()"));
  assertFalse(atlas.contains("IbnKathirGroupIndex"));
  assertFalse(atlas.contains("IbnKathirFullCommentaryPrivate"));
  assertFalse(atlas.contains("IbnKathirQuranComPrivateTitles"));
  assertFalse(atlas.contains("BaqaraPilotScreen"));
  assertFalse(atlas.contains("IbnKathirMapActivity"));
  assertTrue(unitReader.contains("new MapJalalaynDialog("));
 }
}
