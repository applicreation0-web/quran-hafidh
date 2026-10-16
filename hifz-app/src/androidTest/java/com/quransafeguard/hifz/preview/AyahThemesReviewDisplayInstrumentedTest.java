package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real QCF WebView, synthetic labels only: independent titles cannot open new recall holes. */
public final class AyahThemesReviewDisplayInstrumentedTest {
 @Test public void independentTitlesLeaveFullMaskAndHistoricalHolesExactlyUnchanged() throws Exception {
  Context c=ApplicationProvider.getApplicationContext();
  Intent intent=new Intent(c,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,518);
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)) {
   HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Independent synthetic title targets");
   scenario.onActivity(a->{
    MushafView view=mushaf(a);GeometryRepository g=GeometryRepository.get(a);
    SemanticPassageRepository old=SemanticPassageRepository.shared(a);
    view.setMaskFollowsSelection(false);
    view.setSelection(Collections.emptyList(),g.lineIdsOnPage(518));
    view.setPageLandmarkBoxes(old.pageLandmarkBoxes(518));
    view.setSemanticCues(old.readerCuesForPage(518),true,false);
    view.setMask(100);
   });
   String before=js(scenario,"JSON.stringify({mask:mask,holes:protectedWordBoxes(),layers:Array.from(document.querySelectorAll('.masklayer')).map(n=>n.outerHTML)})");
   JSONObject state=new JSONObject(before);
   assertEquals(100,state.getInt("mask"));
   assertTrue("Real historical recall holes must exist",state.getJSONArray("holes").length()>0);
   assertTrue("Real full-page mask must exist",state.getJSONArray("layers").length()>0);
   scenario.onActivity(a->{
    try {
     MawdudiPassageIndex canonical=MawdudiPassageIndex.load(a);JSONArray rows=new JSONArray();
     for(MawdudiPassageIndex.Passage p:canonical.all()) {
      JSONObject r=new JSONObject().put("id",p.id).put("surah",p.surah)
       .put("start",p.firstAyah).put("end",p.lastAyah).put("source_immutability","LOCKED")
       .put("display_label","SYNTHETIC "+p.id)
       .put("source",p.editorialAdaptation?JSONObject.NULL:"Ayah Themes")
       .put("source_title_exact",p.editorialAdaptation?JSONObject.NULL:"SYNTHETIC "+p.id)
       .put("display_label_origin",p.editorialAdaptation?AyahThemesCueIndex.EDITORIAL:AyahThemesCueIndex.ORIGINAL)
       .put("display_label_approved_by_user",p.editorialAdaptation);
      rows.put(r);
     }
     AyahThemesCueIndex index=AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA)
       .put("passages",rows).toString(),canonical,WordGeometryRepository.shared(a));
     AyahThemesReviewDisplay.apply(a,mushaf(a),index,518);
    } catch(Exception e){throw new AssertionError(e);}
   });
   String after=js(scenario,"JSON.stringify({mask:mask,holes:protectedWordBoxes(),layers:Array.from(document.querySelectorAll('.masklayer')).map(n=>n.outerHTML)})");
   assertEquals("Display cannot change a single mask/hole byte",before,after);
   assertTrue("New independent exact-word targets must be visible",
     Integer.parseInt(js(scenario,"String(document.querySelectorAll('.ayah-themes-title-targets rect').length)"))>0);
  }
 }
 @Test public void graphical114MapUsesRealQcfAndReturnsToExactPassage() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();
  Intent intent=new Intent(context,IbnKathirMapActivity.class)
   .putExtra(IbnKathirMapActivity.EXTRA_SURAH,3).putExtra(IbnKathirMapActivity.EXTRA_AYAH,175);
  AtomicReference<MawdudiMapScreen> screen=new AtomicReference<>();
  AtomicReference<MawdudiPassageIndex.Passage> selected=new AtomicReference<>();
  try(ActivityScenario<IbnKathirMapActivity> scenario=ActivityScenario.launch(intent)) {
   HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"pagePreview","Historical audit host");
   scenario.onActivity(a->{
    try {
     MawdudiPassageIndex canonical=MawdudiPassageIndex.load(a);JSONArray rows=new JSONArray(),macros=new JSONArray();
     for(MawdudiPassageIndex.Passage p:canonical.all())rows.put(new JSONObject().put("id",p.id).put("surah",p.surah)
      .put("start",p.firstAyah).put("end",p.lastAyah).put("source_immutability","LOCKED")
      .put("display_label","SYNTHETIC original navigation heading "+p.id)
      .put("source",p.editorialAdaptation?JSONObject.NULL:"Ayah Themes")
      .put("source_title_exact",p.editorialAdaptation?JSONObject.NULL:"SYNTHETIC original navigation heading "+p.id)
      .put("display_label_origin",p.editorialAdaptation?AyahThemesCueIndex.EDITORIAL:AyahThemesCueIndex.ORIGINAL)
      .put("display_label_approved_by_user",p.editorialAdaptation));
     for(int n=1;n<=114;n++) {
      java.util.List<MawdudiPassageIndex.Passage> ps=canonical.forSurah(n);int last=ps.get(ps.size()-1).lastAyah;
      int[][] bounds=n==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:n==9?new int[][]{{1,37},{38,72},{73,129}}:new int[][]{{1,last}};
      for(int[] b:bounds)macros.put(new JSONObject().put("surah",n).put("start",b[0]).put("end",b[1])
       .put("label","SYNTHETIC macro heading").put("status",bounds.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF"));
     }
     AyahThemesCueIndex titles=AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA).put("passages",rows).toString(),canonical,WordGeometryRepository.shared(a));
     MawdudiMapData macro=MawdudiMapData.parse(new JSONObject().put("schema","MAWDUDI_SOURCE_SAFE_MACROS_V1").put("macros",macros).toString(),canonical);
     ((MushafView)field(a,"pagePreview")).destroySafely();
     MawdudiMapScreen map=new MawdudiMapScreen(a,GeometryRepository.get(a),canonical,titles,macro,3,175,null);
     java.lang.reflect.Field f=IbnKathirMapActivity.class.getDeclaredField("map114");f.setAccessible(true);f.set(a,map);
     map.display();a.attachPilotPreview(map.mushaf());screen.set(map);selected.set(canonical.at(3,175));
    }catch(Exception ex){throw new AssertionError(ex);}
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"pagePreview","114-surah graphical QCF map");
   int expected=GeometryRepository.get(context).pageForVerse(new com.quransafeguard.hifz.core.VerseRef(3,selected.get().firstAyah));
   scenario.onActivity(a->{
    assertEquals(expected,((Integer)field(screen.get(),"page")).intValue());
    assertEquals("Graph",field(screen.get(),"graph").getClass().getSimpleName());
    assertTrue(((android.view.View)field(screen.get(),"graph")).getWidth()>0);
    assertFalse(((java.util.List<?>)field(screen.get().mushaf(),"lastSelection")).isEmpty());
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-map114-synthetic-graph-qcf.png");
   scenario.onActivity(a->screen.get().openReader());
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   AtomicReference<StudyReaderActivity> reader=new AtomicReference<>();
   for(int attempt=0;attempt<44;attempt++) {
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
     for(android.app.Activity a:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
      if(a instanceof StudyReaderActivity && Boolean.TRUE.equals(field(mushaf((StudyReaderActivity)a),"pageShown")))reader.set((StudyReaderActivity)a);
    });
    if(reader.get()!=null)break;android.os.SystemClock.sleep(250);
   }
   assertNotNull("The real reader must render the map passage",reader.get());
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    assertEquals(expected,((Integer)field(reader.get(),"page")).intValue());
    java.util.List<?> focus=(java.util.List<?>)field(mushaf(reader.get()),"lastSelection");assertFalse(focus.isEmpty());
    for(Object v:focus){com.quransafeguard.hifz.core.VerseRef verse=(com.quransafeguard.hifz.core.VerseRef)v;assertEquals(3,verse.getSurah());assertTrue(selected.get().contains(verse.getAyah()));}
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-map114-synthetic-reader-focus.png");
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->assertEquals(expected,((Integer)field(screen.get(),"page")).intValue()));
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-map114-synthetic-return.png");
  }
 }
 private static Object field(Object o,String name) {
  try{java.lang.reflect.Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}
 }
 private static MushafView mushaf(StudyReaderActivity a) {
  try{java.lang.reflect.Field f=StudyReaderActivity.class.getDeclaredField("mushaf");f.setAccessible(true);return (MushafView)f.get(a);}
  catch(Exception e){throw new AssertionError(e);}
 }
 private static String js(ActivityScenario<StudyReaderActivity> s,String expression)throws Exception {
  CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
  s.onActivity(a->mushaf(a).evaluateJavascript(expression,v->{result.set(v);done.countDown();}));
  assertTrue("WebView JavaScript result timed out",done.await(15,TimeUnit.SECONDS));
  return new JSONArray("["+result.get()+"]").getString(0);
 }
}
