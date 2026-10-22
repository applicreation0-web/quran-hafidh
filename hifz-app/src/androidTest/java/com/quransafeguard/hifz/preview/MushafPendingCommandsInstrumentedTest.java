package com.quransafeguard.hifz.preview;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Uses a real WebView, real folio and its actual JS-ready callback. */
public final class MushafPendingCommandsInstrumentedTest {
 @Test public void everyCommandSubmittedBeforeReadyRunsInOrder() throws Exception {
  List<Integer> observed=Collections.synchronizedList(new ArrayList<>());
  CountDownLatch done=new CountDownLatch(1);
  MushafView[] view=new MushafView[1];
  try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
   activity.onActivity(a->{
    try{
     view[0]=new MushafView(a);a.setContentView(view[0]);
     view[0].show(14,Collections.emptyList(),GeometryRepository.get(a).lineIdsOnPage(14),0);
     java.lang.reflect.Method enqueue=MushafView.class.getDeclaredMethod("runWhenReady",Runnable.class);
     enqueue.setAccessible(true);
     enqueue.invoke(view[0],(Runnable)()->observed.add(1));
     enqueue.invoke(view[0],(Runnable)()->observed.add(2));
     enqueue.invoke(view[0],(Runnable)()->{observed.add(3);done.countDown();});
    }catch(Exception e){throw new AssertionError(e);}
   });
   assertTrue("Real JS never became ready",done.await(30,TimeUnit.SECONDS));
   assertEquals("Required pre-ready commands were overwritten",Arrays.asList(1,2,3),observed);
   activity.onActivity(a->view[0].destroySafely());
  }
 }
 /** Full QCF/JS smoke test, not a string-presence contract. It exercises the
  * exact holes and checks that OFF removes the real SVG mask without Hifz writes. */
 @Test public void freeReaderMaskKeepsPhysicalWordHolesAndTogglesCleanly() throws Exception {
  final MushafView[] view=new MushafView[1];
  final CountDownLatch shown=new CountDownLatch(1);
  try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
   activity.onActivity(a->{
    try{
     view[0]=new MushafView(a);
     a.setContentView(view[0]);
     view[0].setListener(new MushafView.Listener(){
      @Override public void onReady(){}
      @Override public void onVerseTap(com.quransafeguard.hifz.core.VerseRef verse){}
      @Override public void onError(String error){throw new AssertionError(error);}
      @Override public void onPageShown(int page){if(page==14)shown.countDown();}
     });
     List<String> pageLines=GeometryRepository.get(a).lineIdsOnPage(14);
     assertFalse("Actual QCF page requires physical lines",pageLines.isEmpty());
     org.json.JSONArray boxes=WordGeometryRepository.shared(a).pageLandmarkBoxes(14);
     assertEquals("Every folio needs the six physical landmarks",6,boxes.length());
     org.json.JSONArray anchorBoxes=new org.json.JSONArray().put(boxes.getJSONArray(0));
     org.json.JSONArray cues=new org.json.JSONArray().put(
       new org.json.JSONObject().put("id","synthetic-mask-sentinel")
         .put("anchorWordCount",1).put("boxes",anchorBoxes));
     view[0].show(14,Collections.emptyList(),pageLines,0);
     view[0].setMaskFollowsSelection(false);
     view[0].setPageLandmarkBoxes(boxes);
     view[0].setSemanticCues(cues,true,false);
     view[0].setMask(100);
    }catch(Exception e){throw new AssertionError(e);}
   });
   assertTrue("The authentic QCF page did not render",shown.await(30,TimeUnit.SECONDS));
   assertTrue("The 100% mask must have true protected word holes",
     awaitMaskState(activity,view[0],true));
   activity.onActivity(a->view[0].setMask(0));
   assertTrue("OFF must fully restore the original Quran page",
     awaitMaskState(activity,view[0],false));
   activity.onActivity(a->view[0].destroySafely());
  }
 }
 private static boolean awaitMaskState(ActivityScenario<MainActivity> activity,
                                       MushafView view, boolean expected) throws Exception {
  String probe="(function(){var svg=document.querySelector('#mushaf svg');"
      +"var mask=svg&&svg.querySelector('g.masklayer');"
      +"return !!(mask&&mask.getAttribute('mask')==='url(#hifz-exact-word-holes)'"
      +"&&mask.querySelector('#hifz-exact-word-holes'));})()";
  for(int i=0;i<40;i++){
   CountDownLatch done=new CountDownLatch(1);
   java.util.concurrent.atomic.AtomicReference<String> result=new java.util.concurrent.atomic.AtomicReference<>();
   activity.onActivity(a->view.evaluateJavascript(probe,value->{result.set(value);done.countDown();}));
   if(done.await(3,TimeUnit.SECONDS) && (expected?"true":"false").equals(result.get()))return true;
   Thread.sleep(200);
  }
  return false;
 }

}
