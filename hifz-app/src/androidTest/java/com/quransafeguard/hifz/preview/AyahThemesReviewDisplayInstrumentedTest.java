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
