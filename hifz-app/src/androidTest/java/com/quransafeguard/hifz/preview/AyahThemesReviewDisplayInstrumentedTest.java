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
      int[][] bounds=n==2?new int[][]{{1,20},{21,29},{30,39},{40,120},{121,141},{142,152},{153,251},{252,260},{261,283},{284,286}}:n==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:n==4?new int[][]{{1,35},{36,42},{43,43},{44,57},{58,72},{73,100},{101,103},{104,104},{105,135},{136,175},{176,176}}:n==5?new int[][]{{1,10},{11,26},{27,32},{33,40},{41,50},{51,69},{70,86},{87,108},{109,119},{120,120}}:n==6?new int[][]{{1,12},{13,24},{25,32},{33,73},{74,90},{91,108},{109,154},{155,160},{161,165}}:n==7?new int[][]{{1,10},{11,25},{26,53},{54,58},{59,171},{172,174},{175,179},{180,198},{199,206}}:n==8?new int[][]{{1,41},{42,54},{55,59},{60,66},{67,71},{72,75}}:n==9?new int[][]{{1,12},{13,37},{38,72},{73,90},{91,110},{111,118},{119,127},{128,129}}:new int[][]{{1,last}};
      for(int[] b:bounds)macros.put(new JSONObject().put("surah",n).put("start",b[0]).put("end",b[1])
       .put("label","SYNTHETIC macro heading").put("status",bounds.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":n==5&&b[0]==120?"SOURCE_CONCLUSION_FROM_INTRODUCTION":(n==2||n>=4)?"SOURCE_BOUNDARY_CONFIRMED_IN_INTRODUCTION":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF"));
     }
     AyahThemesCueIndex titles=AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA).put("passages",rows).toString(),canonical,WordGeometryRepository.shared(a));
     MawdudiMapData macro=MawdudiMapData.parse(new JSONObject().put("schema","MAWDUDI_SOURCE_SAFE_MACROS_V1").put("macros",macros).toString(),canonical);
     ((MushafView)field(a,"pagePreview")).destroySafely();
     MawdudiMapScreen map=new MawdudiMapScreen(a,GeometryRepository.get(a),canonical,titles,macro,3,175,null);
     java.lang.reflect.Field f=IbnKathirMapActivity.class.getDeclaredField("map114");f.setAccessible(true);f.set(a,map);
     map.display();
     // The map now correctly starts on level 1. Enter level 2 explicitly before
     // asserting that its passage graph is attached, measured and navigable.
     java.lang.reflect.Method showPassages=MawdudiMapScreen.class.getDeclaredMethod("setLevel",boolean.class);
     showPassages.setAccessible(true);showPassages.invoke(map,false);
     // Opening an unit deliberately shows only its QCF thumbnails. A separate
     // passage selection reveals the single-page focus and full reader action.
     java.lang.reflect.Field passageOpen=MawdudiMapScreen.class.getDeclaredField("passageOpen");
     passageOpen.setAccessible(true);passageOpen.setBoolean(map,true);
     map.refresh();
     a.attachPilotPreview(map.mushaf());screen.set(map);selected.set(canonical.at(3,175));
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
 @Test public void titleTargetPreservesPassiveEndpointAndArmedActiveWeakMark()throws Exception {
  Context c=ApplicationProvider.getApplicationContext();JSONObject backup=HifzBackup.export(c);
  try {
   c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).edit().clear().commit();
   HifzPrefs prefs=new HifzPrefs(c);GeometryRepository g=GeometryRepository.get(c);
   com.quransafeguard.hifz.core.VerseRef verse=new com.quransafeguard.hifz.core.VerseRef(1,1);
   assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new com.quransafeguard.hifz.core.VerseRange(verse,new com.quransafeguard.hifz.core.VerseRef(1,7))),g));
   prefs.setMurajaahCursor(verse); // Bootstrap default 2:1 lies outside this Fatiha-only fixture.
   for(String mode:new String[]{HifzSessionActivity.MURAJAAH,HifzSessionActivity.MURAJAAH_ACTIVE}) {
    try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode))) {
     HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Review title gesture "+mode);
     CountDownLatch done=new CountDownLatch(1);
     scenario.onActivity(a->{
      try {
       assertNotNull("Review fixture must have a plan: "+mode+" / "+((android.widget.TextView)field(a,"program")).getText(),field(a,"murajaahPlan"));
       java.lang.reflect.Field armed=a.getClass().getDeclaredField("weakMarkMode");armed.setAccessible(true);armed.setBoolean(a,HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode));
       String script;
       try(java.io.InputStream in=a.getAssets().open("ayah_themes_display/review_titles.js")) {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);script=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
       }
       // Real production installer/native bridge over exact QCF first-word geometry.
       WordGeometryRepository.WordBox word=null;
       for(WordGeometryRepository.WordBox w:WordGeometryRepository.shared(a).wordsForPage(1))if(w.surah==1&&w.ayah==1&&w.word==1){word=w;break;}
       assertNotNull(word);
       JSONArray cues=new JSONArray().put(new JSONObject().put("id","s001a001b007").put("surah",1).put("start",1).put("boxes",new JSONArray().put(word.boxJson())));
       ((MushafView)field(a,"mushaf")).evaluateJavascript(script+";window.AyahThemesReviewDisplay.install("+cues+");document.querySelector('.ayah-themes-title-targets rect').dispatchEvent(new MouseEvent('click',{bubbles:true}));",v->done.countDown());
      }catch(Exception e){throw new AssertionError(e);}
     });
     assertTrue(done.await(15,TimeUnit.SECONDS));
     androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
     scenario.onActivity(a->{
      if(HifzSessionActivity.MURAJAAH.equals(mode))assertEquals(verse,field(a,"murajaahActualEnd"));
      else {assertEquals(false,field(a,"weakMarkMode"));assertTrue(new HifzPrefs(a).murajaahWeakVerses().contains(verse));}
     });
    }
   }
  }finally{HifzBackup.restore(c,backup);}
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
