package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Native tests prepared for a separately authorized test build. No production approval is created. */
public final class CorrespondenceNavigationInstrumentedTest {
 @Test public void disabledCatalogueProducesNoButtonAndNoSessionWrite(){
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(StudyReaderActivity.class)){
   scenario.onActivity(activity->{
    Map<String,?> before=activity.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll();
    LinearLayout host=new LinearLayout(activity);
    CorrespondenceEntryPoints.populate(activity,host,CorrespondenceEntryPoints.origin(activity,CorrespondenceAccess.Surface.LECTURE,"lecture",2,255,42,"unit","passage","mask","2:255",255,255));
    assertEquals(0,host.getChildCount());assertEquals(View.GONE,host.getVisibility());
    assertEquals(before,activity.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll());
   });
  }
 }
 @Test public void realBundleRestoresFreshDialogAndHostDestructionDoesNotIssueCloseCallback(){
  AtomicInteger callbacks=new AtomicInteger();
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(StudyReaderActivity.class)){
   scenario.onActivity(activity->{
    Geometry geometry=new CorrespondenceGeometry(GeometryRepository.get(activity),WordGeometryRepository.shared(activity));
    VerseRange a=new VerseRange(2,255,255,geometry),b=new VerseRange(3,2,2,geometry);
    Relation fixture=new Relation("SYNTHETIC-NATIVE-ONLY",Type.RESSEMBLANCE,Mode.TWO_PAGES,Arrays.asList(a,b),
     Arrays.asList(new Anchor(0,a,true,"SYNTHETIC locator",0,0),new Anchor(1,b,true,"SYNTHETIC locator",0,0)),
     new Gates(true,true,true,true,true),geometry,new Presentation("SYNTHETIC title","SYNTHETIC fact",Arrays.asList(new SourceReference("SYNTHETIC author","SYNTHETIC work","SYNTHETIC edition","SYNTHETIC locator"))));
    ReaderSnapshot origin=new ReaderSnapshot(2,255,a.firstPage,"unit","passage","mask",1,"2:255");
    CorrespondenceScreen first=CorrespondenceScreen.open(activity,fixture,origin,geometry,o->callbacks.incrementAndGet());assertNotNull(first);
    Bundle saved=first.saveCheckpoint();first.detachForHostDestruction();assertEquals(0,callbacks.get());
    CorrespondenceScreen restored=CorrespondenceScreen.restore(activity,fixture,origin,geometry,saved,o->callbacks.incrementAndGet());assertNotNull(restored);
    assertEquals("mask",restored.saveState().origin.mask);assertEquals("passage",restored.saveState().origin.passageId);
    restored.onBackPressed();
   });
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   assertEquals(1,callbacks.get());
  }
 }
 @Test public void readerRecreationDoesNotReplayLaunchJumpOverSavedPage(){
  Context context=ApplicationProvider.getApplicationContext();
  Intent intent=new Intent(context,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,42).putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,"2:255");
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)){
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(activity->{
    try{java.lang.reflect.Method set=StudyReaderActivity.class.getDeclaredMethod("setPage",int.class);set.setAccessible(true);set.invoke(activity,43);}catch(Exception e){throw new AssertionError(e);}
   });
   scenario.recreate();InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(activity->{
    try{java.lang.reflect.Field field=StudyReaderActivity.class.getDeclaredField("page");field.setAccessible(true);assertEquals(43,field.getInt(activity));}catch(Exception e){throw new AssertionError(e);}
   });
  }
 }
 /** Exercises actual native windows and authentic QCF pixels with a visibly synthetic
  * relation. This is NOT approved-catalogue CorrespondenceActivity lifecycle evidence. */
 @Test public void syntheticQcfComparisonHasIndependentNativeGeometryAndRestoresEnlargedViewport(){
  final CorrespondenceScreen[] current=new CorrespondenceScreen[1];
  final Bundle[] checkpoint=new Bundle[1];
  final CorrespondenceState[] before=new CorrespondenceState[1];
  AtomicInteger callbacks=new AtomicInteger();
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(StudyReaderActivity.class)){
   scenario.onActivity(activity->{
    Geometry geometry=new CorrespondenceGeometry(GeometryRepository.get(activity),WordGeometryRepository.shared(activity));
    Relation relation=syntheticRelation(geometry);
    ReaderSnapshot origin=new ReaderSnapshot(2,255,42,"native-unit","native-passage","native-mask",1.25,"2:255",7,9);
    current[0]=CorrespondenceScreen.open(activity,relation,origin,geometry,o->callbacks.incrementAndGet());
    assertNotNull(current[0]);
   });
   awaitRendered(scenario,current);
   scenario.onActivity(activity->{
    LinearLayout body=(LinearLayout)field(current[0],"body");
    assertEquals(2,body.getChildCount());
    View left=body.getChildAt(0),right=body.getChildAt(1);
    assertTrue("Both folios must have usable native width",left.getWidth()>Ui.dp(activity,48));
    assertTrue(Math.abs(left.getWidth()-right.getWidth())<=1);
    assertTrue("Independent folios must not overlap",left.getRight()<=right.getLeft());
    assertEquals(body.getHeight(),left.getHeight());
    View enlarge=findDescription(current[0].getWindow().getDecorView(),"Agrandir le feuillet B");
    assertNotNull(enlarge);
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-correspondence-synthetic-comparison.png");
   assertVisibleInk(scenario,current);
   scenario.onActivity(activity->{
    findDescription(current[0].getWindow().getDecorView(),"Agrandir le feuillet B").performClick();
   });
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(activity->{
    before[0]=current[0].saveState();assertEquals(1,before[0].enlargedPane);
    checkpoint[0]=current[0].saveCheckpoint();current[0].detachForHostDestruction();
    assertEquals("Host destruction must not issue a return callback",0,callbacks.get());
    Geometry geometry=new CorrespondenceGeometry(GeometryRepository.get(activity),WordGeometryRepository.shared(activity));
    current[0]=CorrespondenceScreen.restore(activity,syntheticRelation(geometry),before[0].origin,geometry,checkpoint[0],o->callbacks.incrementAndGet());
    assertNotNull(current[0]);
   });
   awaitRendered(scenario,current);
   scenario.onActivity(activity->{
    CorrespondenceState restored=current[0].saveState();
    assertEquals(1,restored.enlargedPane);
    assertEquals("native-unit",restored.origin.unitId);assertEquals("native-passage",restored.origin.passageId);
    assertEquals("native-mask",restored.origin.mask);assertEquals("2:255",restored.origin.selection);
    assertEquals(42,restored.origin.page);assertEquals(1.25,restored.origin.zoom,0);
    assertEquals(7,restored.origin.scrollX);assertEquals(9,restored.origin.scrollY);
    for(int pane=0;pane<2;pane++){
     assertEquals(before[0].panes.get(pane).page,restored.panes.get(pane).page);
     assertEquals(before[0].panes.get(pane).zoom,restored.panes.get(pane).zoom,0.03);
     assertEquals(before[0].panes.get(pane).scrollX,restored.panes.get(pane).scrollX);
     assertEquals(before[0].panes.get(pane).scrollY,restored.panes.get(pane).scrollY);
    }
    LinearLayout body=(LinearLayout)field(current[0],"body");
    assertEquals(1,body.getChildCount());assertEquals(body.getWidth(),body.getChildAt(0).getWidth());
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-correspondence-synthetic-enlarged-restored.png");
   assertVisibleInk(scenario,current);
   scenario.onActivity(activity->{
    current[0].onBackPressed();assertEquals(2,((LinearLayout)field(current[0],"body")).getChildCount());
   });
   awaitRendered(scenario,current);assertVisibleInk(scenario,current);
   scenario.onActivity(activity->current[0].onBackPressed());
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals(1,callbacks.get());
  }
 }
 @Test public void actualCorrespondenceActivityRejectsForgedUnapprovedLaunchWithoutChangingLearningState(){
  Context context=ApplicationProvider.getApplicationContext();
  Map<String,?> before=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll();
  Bundle forged=new Bundle();forged.putString("surface","LECTURE");forged.putString("mode","lecture");
  forged.putString("relationId","SYNTHETIC-NATIVE-ONLY");forged.putBoolean("approved",true);
  Intent intent=new Intent(context,CorrespondenceActivity.class).putExtra("relationId","SYNTHETIC-NATIVE-ONLY").putExtra("origin",forged);
  try(ActivityScenario<CorrespondenceActivity> scenario=ActivityScenario.launch(intent)){
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED,scenario.getState());
   assertEquals(before,context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll());
  }
 }

 /** The immutable trusted runtime is replaced only inside instrumentation and
  * restored in finally. No fixture, pin, approval or setter enters the app APK. */
 @Test public void actualActivityRecreationRestoresSyntheticComparisonAndExactOrigin() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();
  Geometry geometry=new CorrespondenceGeometry(GeometryRepository.get(context),WordGeometryRepository.shared(context));
  CorrespondenceCatalog catalog=syntheticCatalog(geometry);
  java.lang.reflect.Constructor<CorrespondenceRuntime> constructor=CorrespondenceRuntime.class.getDeclaredConstructor(CorrespondenceCatalog.class,Geometry.class);
  constructor.setAccessible(true);
  java.lang.reflect.Field cached=CorrespondenceRuntime.class.getDeclaredField("cached");cached.setAccessible(true);
  Object originalRuntime=cached.get(null);
  Map<String,?> before=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll();
  CorrespondenceScreen[] current={null};CorrespondenceState[] beforeRecreation={null};
  try {
   cached.set(null,constructor.newInstance(catalog,geometry));
   ReaderSnapshot snapshot=new ReaderSnapshot(2,255,42,"native-unit","native-passage","native-mask",1.25,"2:255",7,9);
   CorrespondenceAccess.Origin origin=new CorrespondenceAccess.Origin(CorrespondenceAccess.Surface.LECTURE,"lecture",snapshot,new VerseRange(2,255,255,geometry));
   Intent intent=new Intent(context,CorrespondenceActivity.class).putExtra("relationId","SYNTHETIC-NATIVE-ONLY").putExtra("origin",CorrespondenceEntryPoints.encode(origin));
   try(ActivityScenario<CorrespondenceActivity> scenario=ActivityScenario.launch(intent)){
    scenario.onActivity(activity->{assertFalse(activity.isFinishing());current[0]=(CorrespondenceScreen)field(activity,"screen");assertNotNull(current[0]);});
    awaitRendered(scenario,current);
    scenario.onActivity(activity->{
     LinearLayout body=(LinearLayout)field(current[0],"body");assertEquals(2,body.getChildCount());
     assertTrue(Math.abs(body.getChildAt(0).getWidth()-body.getChildAt(1).getWidth())<=1);
     findDescription(current[0].getWindow().getDecorView(),"Agrandir le feuillet B").performClick();
    });
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    scenario.onActivity(activity->{
     beforeRecreation[0]=current[0].saveState();
     // A recreation must use the saved, validated relation/origin rather than
     // replaying stale launch extras over the retained comparison checkpoint.
     activity.getIntent().putExtra("relationId","FORGED-REPLAY-MUST-NOT-WIN");
     activity.getIntent().putExtra("origin",new Bundle());
    });
    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-correspondence-synthetic-activity-before-recreation.png");
    scenario.recreate();
    scenario.onActivity(activity->{assertFalse("Recreation must not finish the host",activity.isFinishing());current[0]=(CorrespondenceScreen)field(activity,"screen");assertNotNull(current[0]);});
    awaitRendered(scenario,current);
    scenario.onActivity(activity->{
     CorrespondenceState restored=current[0].saveState();
     assertEquals(1,restored.enlargedPane);assertEquals(0,restored.selectedOccurrence);
     assertEquals("native-unit",restored.origin.unitId);assertEquals("native-passage",restored.origin.passageId);
     assertEquals("native-mask",restored.origin.mask);assertEquals("2:255",restored.origin.selection);
     assertEquals(2,restored.origin.surah);assertEquals(255,restored.origin.ayah);assertEquals(42,restored.origin.page);
     assertEquals(1.25,restored.origin.zoom,0);assertEquals(7,restored.origin.scrollX);assertEquals(9,restored.origin.scrollY);
     assertEquals(2,restored.comparison.size());
     for(int pane=0;pane<2;pane++){
      assertEquals(beforeRecreation[0].panes.get(pane).page,restored.panes.get(pane).page);
      assertEquals(beforeRecreation[0].panes.get(pane).zoom,restored.panes.get(pane).zoom,0.03);
      assertEquals(beforeRecreation[0].panes.get(pane).scrollX,restored.panes.get(pane).scrollX);
      assertEquals(beforeRecreation[0].panes.get(pane).scrollY,restored.panes.get(pane).scrollY);
     }
     LinearLayout body=(LinearLayout)field(current[0],"body");assertEquals(1,body.getChildCount());
     assertEquals(body.getWidth(),body.getChildAt(0).getWidth());
    });
    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-correspondence-synthetic-activity-after-recreation.png");
    assertVisibleInk(scenario,current);
    scenario.onActivity(activity->activity.onBackPressed());
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    scenario.onActivity(activity->{assertFalse(activity.isFinishing());assertEquals(2,((LinearLayout)field(current[0],"body")).getChildCount());});
    awaitRendered(scenario,current);assertVisibleInk(scenario,current);
    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-correspondence-synthetic-activity-comparison-restored.png");
    scenario.onActivity(activity->activity.onBackPressed());
    // finish() initiates an asynchronous Android lifecycle transition. A main-
    // thread idle barrier alone does not guarantee that onDestroy has run.
    // Never force the scenario to DESTROYED: the real Back behavior must do it.
    long closeDeadline=android.os.SystemClock.uptimeMillis()+10000;
    while(scenario.getState()!=androidx.lifecycle.Lifecycle.State.DESTROYED
        &&android.os.SystemClock.uptimeMillis()<closeDeadline){
     InstrumentationRegistry.getInstrumentation().waitForIdleSync();
     android.os.SystemClock.sleep(50);
    }
    assertEquals("Second Back must finish and destroy the comparison activity",
      androidx.lifecycle.Lifecycle.State.DESTROYED,scenario.getState());
   }
   assertEquals("Synthetic comparison must not write learning state",before,context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).getAll());
  } finally { cached.set(null,originalRuntime); }
 }
 private static CorrespondenceCatalog syntheticCatalog(Geometry geometry) throws Exception {
  String json="{\"schema\":\"HAAFIDH_APPROVED_CORRESPONDENCES_V1\",\"revision\":1,\"entries\":[{"
   +"\"id\":\"SYNTHETIC-NATIVE-ONLY\",\"type\":\"RESSEMBLANCE\",\"mode\":\"TWO_PAGES\","
   +"\"gates\":{\"source\":true,\"classification\":true,\"anchors\":true,\"visual\":true,\"qa\":true},"
   +"\"rights\":{\"authorized\":true,\"usage\":\"PERSONAL_OFFLINE_ONLY\",\"evidence\":\"SYNTHETIC instrumentation fixture only\"},"
   +"\"originals\":[{\"surah\":2,\"start\":255,\"end\":255},{\"surah\":3,\"start\":2,\"end\":2}],"
   +"\"anchors\":[{\"original_index\":0,\"range\":{\"surah\":2,\"start\":255,\"end\":255},\"source_attested\":true,\"source_locator\":\"SYNTHETIC\",\"first_word\":0,\"last_word\":0},"
   +"{\"original_index\":1,\"range\":{\"surah\":3,\"start\":2,\"end\":2},\"source_attested\":true,\"source_locator\":\"SYNTHETIC\",\"first_word\":0,\"last_word\":0}],"
   +"\"presentation\":{\"short_title\":\"SYNTHETIC test fixture\",\"factual_link\":\"SYNTHETIC test fixture only\",\"sources\":[{\"author\":\"SYNTHETIC\",\"work\":\"SYNTHETIC\",\"edition\":\"SYNTHETIC\",\"locator\":\"SYNTHETIC\"}]}}]}";
  byte[] bytes=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  return CorrespondenceCatalog.parse(bytes,hash.toString(),geometry);
 }
 private static Relation syntheticRelation(Geometry geometry){
  VerseRange a=new VerseRange(2,255,255,geometry),b=new VerseRange(3,2,2,geometry);
  return new Relation("SYNTHETIC-NATIVE-ONLY",Type.RESSEMBLANCE,Mode.TWO_PAGES,Arrays.asList(a,b),
   Arrays.asList(new Anchor(0,a,true,"SYNTHETIC locator",0,0),new Anchor(1,b,true,"SYNTHETIC locator",0,0)),
   new Gates(true,true,true,true,true),geometry,new Presentation("SYNTHETIC test fixture","SYNTHETIC test fixture only",Arrays.asList(new SourceReference("SYNTHETIC author","SYNTHETIC work","SYNTHETIC edition","SYNTHETIC locator"))));
 }
 private static Object field(Object target,String name){
  try{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
  catch(Exception error){throw new AssertionError(error);}
 }
 private static View findDescription(View root,String description){
  if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
  if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++){View found=findDescription(group.getChildAt(i),description);if(found!=null)return found;}}
  return null;
 }
 private static void awaitRendered(ActivityScenario<? extends android.app.Activity> scenario,CorrespondenceScreen[] current){
  long until=android.os.SystemClock.uptimeMillis()+20000;
  boolean[] ready={false};String[] status={"No surface observed"};
  while(android.os.SystemClock.uptimeMillis()<until){
   scenario.onActivity(activity->{
    ready[0]=true;int visible=0;StringBuilder diagnostics=new StringBuilder();
    for(CorrespondenceQcfSurface surface:(CorrespondenceQcfSurface[])field(current[0],"surfaces")){
     android.webkit.WebView web=(android.webkit.WebView)field(surface,"web");
     android.widget.TextView caption=(android.widget.TextView)field(surface,"position");
     assertFalse("Authentic QCF asset could not load",caption.getText().toString().contains("indisponible"));
     assertFalse("QCF focus must be verified",caption.getText().toString().contains("VISUEL_NON_VALIDÉ"));
     assertFalse("Production QCF WebView must stay script-free",web.getSettings().getJavaScriptEnabled());
     diagnostics.append(" [").append(caption.getText()).append(" shown=").append(web.isShown())
      .append(" loading=").append(field(surface,"loading")).append(" height=").append(web.getContentHeight()).append(']');
     // An enlarged comparison deliberately detaches its other WebView. Android
     // may defer that document's completion until it is attached again. Its
     // requested viewport remains in the checkpoint; require paint when shown.
     if(web.isShown()){
      visible++;ready[0]&=!(Boolean)field(surface,"loading")&&web.getContentHeight()>0;
     }
    }
    ready[0]&=visible>0;status[0]=diagnostics.toString();
   });
   if(ready[0]){InstrumentationRegistry.getInstrumentation().waitForIdleSync();return;}
   android.os.SystemClock.sleep(100);
  }
  fail("Timed out waiting for authentic QCF WebView content:"+status[0]);
 }
 private static void assertVisibleInk(ActivityScenario<? extends android.app.Activity> scenario,CorrespondenceScreen[] current){
  long until=android.os.SystemClock.uptimeMillis()+10000;
  String failure="At least one folio must be visibly laid out";
  do{
   java.util.List<android.graphics.Rect> areas=new java.util.ArrayList<>();
   scenario.onActivity(activity->{
    for(CorrespondenceQcfSurface surface:(CorrespondenceQcfSurface[])field(current[0],"surfaces")){
     android.webkit.WebView web=(android.webkit.WebView)field(surface,"web");
     android.graphics.Rect area=new android.graphics.Rect();if(web.getGlobalVisibleRect(area))areas.add(area);
    }
   });
   android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   assertNotNull(image);
   boolean ink=!areas.isEmpty();
   try{for(android.graphics.Rect area:areas){
    // Count actual ink pixels. Sampling every other row and column can miss
    // most thin QCF strokes in a small pane (the short 3:2 focus has 188 actual
    // dark pixels but only 34 sampled pixels on the CI emulator).
    int dark=0;for(int y=Math.max(0,area.top);y<Math.min(image.getHeight(),area.bottom);y++)
     for(int x=Math.max(0,area.left);x<Math.min(image.getWidth(),area.right);x++){
      int pixel=image.getPixel(x,y);if(android.graphics.Color.red(pixel)<150&&android.graphics.Color.green(pixel)<150&&android.graphics.Color.blue(pixel)<150)dark++;
     }
    if(dark<=100){ink=false;failure="QCF viewport must contain rendered ink, not a blank WebView: "+dark+" dark pixels in "+area;}
   }}finally{image.recycle();}
   if(ink)return;
   // onPageFinished/contentHeight do not guarantee a compositor frame. Wait
   // for actual screenshot ink after a folio is reattached, without a fixed delay.
   android.os.SystemClock.sleep(100);
  }while(android.os.SystemClock.uptimeMillis()<until);
  fail(failure);
 }

}
