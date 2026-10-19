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
}
