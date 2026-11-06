package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.quransafeguard.hifz.core.VerseRef;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Activity, original QCF polygons and original English Jalalayn, no private data. */
public final class CorrespondenceNavigationInstrumentedTest {
    private static Object field(Object o,String n){try{
        java.lang.reflect.Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);
    }catch(Exception e){throw new AssertionError(e);}}
    private static Object call(Object o,String n,Class<?>[] types,Object... args){try{
        java.lang.reflect.Method m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);
    }catch(Exception e){throw new AssertionError(e);}}
    private static ActivityScenario<CorrespondenceComparisonActivity> launch(int s,int v,String id){
        Context c=ApplicationProvider.getApplicationContext();
        return ActivityScenario.launch(new Intent(c,CorrespondenceComparisonActivity.class)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_SURAH,s)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_AYAH,v)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_ID,id));
    }
    private static void ready(ActivityScenario<CorrespondenceComparisonActivity> scenario)throws Exception{
        long until=System.currentTimeMillis()+45000;
        AtomicBoolean ok=new AtomicBoolean();
        while(System.currentTimeMillis()<until){
            scenario.onActivity(a->ok.set(a.bothAuthenticQcfPanesReadyForTest()&&a.visibleTafsirReadyForTest()));
            if(ok.get())return;Thread.sleep(100);
        }
        fail("Original QCF / English Tafsir did not become ready");
    }
    private static void screenshot(String name)throws Exception{
        MandatCaptureInstrumentedTest.capture(name);
    }
    private static void step(Activity a,int side,int delta){call(a,"stepVerse",new Class<?>[]{int.class,int.class},side,delta);}
    private static VerseRef selected(Activity a,int side){return ((VerseRef[])field(a,"selectedVerses"))[side];}
    private static void assertSelection(ActivityScenario<CorrespondenceComparisonActivity> scenario,int side,VerseRef expected)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicBoolean correct=new AtomicBoolean();
        scenario.onActivity(a->{
            assertEquals(expected,selected(a,side));
            assertTrue(((TextView[])field(a,"tafsirLabels"))[side].getText().toString().contains(expected.toString()));
            assertTrue(((TextView[])field(a,"pageLabels"))[side].getText().toString().contains(expected.toString()));
            a.qcfPagesForTest()[side].evaluateJavascript("(function(){var s=document.querySelector('#haafidh-active-verse');return !!s && s.textContent.indexOf(\"surah='"+expected.getSurah()+"'\")>=0 && s.textContent.indexOf(\"ayah='"+expected.getAyah()+"'\")>=0;})()",r->{correct.set("true".equals(r));done.countDown();});
        });
        assertTrue(done.await(10,TimeUnit.SECONDS));assertTrue("Exact local polygon stylesheet must follow active verse",correct.get());
    }
    @Test public void fixedTwoSurahFolioHasIndependentBoundedTafsirAndRestoration()throws Exception{
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(12,3,"R008")){
            scenario.onActivity(a->call(a,"setTafsirOpen",new Class<?>[]{boolean.class},true));ready(scenario);
            scenario.onActivity(a->{
                int[] pages=(int[])field(a,"shownPages");assertArrayEquals(new int[]{235,235},pages);
                VerseRef b=selected(a,1);
                for(int i=0;i<30;i++)step(a,0,-1);
                assertEquals(new VerseRef(12,1),selected(a,0));assertEquals(b,selected(a,1));
                assertFalse(((Button[])field(a,"previousVerses"))[0].isEnabled());
                for(int i=0;i<30;i++)step(a,0,1);
                assertEquals(12,selected(a,0).getSurah());assertEquals(b,selected(a,1));
                assertFalse(((Button[])field(a,"nextVerses"))[0].isEnabled());
                VerseRef frozenA=selected(a,0);
                for(int i=0;i<40;i++)step(a,1,1);
                assertEquals(new VerseRef(11,123),selected(a,1));assertEquals(frozenA,selected(a,0));
                assertArrayEquals(new int[]{235,235},(int[])field(a,"shownPages"));
            });
            ready(scenario);assertSelection(scenario,1,new VerseRef(11,123));
            screenshot("navigation-p235");scenario.recreate();ready(scenario);
            assertSelection(scenario,1,new VerseRef(11,123));
            // Rapid alternating loads: final reference, commentary and SVG must agree.
            scenario.onActivity(a->{for(int i=0;i<20;i++){step(a,0,-1);step(a,1,-1);step(a,0,1);step(a,1,1);}});
            ready(scenario);
            scenario.onActivity(a->{
                TafsirRepository repository=new TafsirRepository(a);
                for(int side=0;side<2;side++)try{
                    TafsirRepository.Entry entry=repository.load(selected(a,side));StringBuilder expected=new StringBuilder();
                    for(TafsirRepository.Run run:entry.commentaryRuns)expected.append(run.text);
                    assertEquals(expected.toString().trim(),((TextView[])field(a,"commentaries"))[side].getText().toString());
                }catch(Exception failure){throw new AssertionError(failure);}
            });
        }
    }
    @Test public void surahSeventeenEighteenBoundaryNeverMovesItsFolio()throws Exception{
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(17,111,"R011")){
            ready(scenario);
            scenario.onActivity(a->{for(int i=0;i<60;i++){step(a,0,1);step(a,1,-1);}
                assertEquals(new VerseRef(17,111),selected(a,0));assertEquals(new VerseRef(18,1),selected(a,1));
                assertArrayEquals(new int[]{293,293},(int[])field(a,"shownPages"));});
            screenshot("navigation-p293");
        }
    }
    @Test public void initialLoadingSelectionAndDelayedStaleResultsNeverReplaceTheActiveVerse()throws Exception{
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(12,3,"R008")){
            // Move before the original SVG has completed loading.
            scenario.onActivity(a->{call(a,"setTafsirOpen",new Class<?>[]{boolean.class},true);step(a,0,-1);step(a,0,-1);step(a,1,1);});
            ready(scenario);assertSelection(scenario,0,new VerseRef(12,1));assertSelection(scenario,1,new VerseRef(11,121));
            CountDownLatch blocked=new CountDownLatch(1),release=new CountDownLatch(1);
            java.util.List<String> seen=new java.util.concurrent.CopyOnWriteArrayList<>();
            String[] stale=new String[1];
            scenario.onActivity(a->{
                ((java.util.concurrent.ExecutorService)field(a,"tafsirIo")).submit(()->{blocked.countDown();try{release.await(20,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
                try{StringBuilder text=new StringBuilder();for(TafsirRepository.Run run:new TafsirRepository(a).load(new VerseRef(12,2)).commentaryRuns)text.append(run.text);stale[0]=text.toString().trim();}catch(Exception e){throw new AssertionError(e);}
                ((TextView[])field(a,"commentaries"))[0].addTextChangedListener(new android.text.TextWatcher(){
                    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                    public void onTextChanged(CharSequence s,int start,int before,int count){seen.add(s.toString());}
                    public void afterTextChanged(android.text.Editable s){}
                });
            });
            assertTrue(blocked.await(5,TimeUnit.SECONDS));
            try{scenario.onActivity(a->{step(a,0,1);step(a,0,1);step(a,1,1);});}finally{release.countDown();}
            ready(scenario);assertSelection(scenario,0,new VerseRef(12,3));assertSelection(scenario,1,new VerseRef(11,122));
            assertFalse("Delayed obsolete 12:2 commentary must never replace active 12:3",seen.contains(stale[0]));
        }
    }
    @Test public void unavailableCommentaryClearsOnlyThatSideAndKeepsItsExactReference()throws Exception{
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(12,3,"R008")){
            scenario.onActivity(a->call(a,"setTafsirOpen",new Class<?>[]{boolean.class},true));ready(scenario);
            String[] other=new String[1];
            scenario.onActivity(a->{
                other[0]=((TextView[])field(a,"commentaries"))[1].getText().toString();
                android.content.Context broken=new android.content.ContextWrapper(a){
                    @Override public Context getApplicationContext(){return this;}
                    @Override public java.io.File getNoBackupFilesDir(){return new java.io.File("/proc/haafidh-unavailable-test");}
                };
                try{java.lang.reflect.Field f=a.getClass().getDeclaredField("tafsir");f.setAccessible(true);f.set(a,new TafsirRepository(broken));}catch(Exception e){throw new AssertionError(e);}
                step(a,0,-1);
            });
            AtomicBoolean missing=new AtomicBoolean();long deadline=System.currentTimeMillis()+15000;
            while(System.currentTimeMillis()<deadline){scenario.onActivity(a->missing.set("Commentaire indisponible.".contentEquals(((TextView[])field(a,"commentaries"))[0].getText())));if(missing.get())break;Thread.sleep(100);}
            assertTrue(missing.get());assertSelection(scenario,0,new VerseRef(12,2));
            scenario.onActivity(a->assertEquals(other[0],((TextView[])field(a,"commentaries"))[1].getText().toString()));
        }
    }
    private static View named(View v,String name){
        if(name.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())
                ||(v instanceof TextView&&name.contentEquals(((TextView)v).getText())))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View t=named(g.getChildAt(i),name);if(t!=null)return t;}}
        return null;
    }
    private static View sourcesDecor(Activity a){
        return ((android.app.AlertDialog)field(a,"sourcesDialog")).getWindow().getDecorView();
    }
    private static void tap(ActivityScenario<CorrespondenceComparisonActivity> scenario,String name,boolean dialog)throws Exception{
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();ins.waitForIdleSync();
        // After returning from StudyReaderActivity the BOOX emulator may report
        // RESUMED before SurfaceFlinger has made the comparison window visible.
        // Wait for a *real, on-screen* touch target; never performClick offscreen.
        long deadline=android.os.SystemClock.uptimeMillis()+15000;
        boolean touchable=false;
        while(android.os.SystemClock.uptimeMillis()<deadline){
            AtomicBoolean visible=new AtomicBoolean(false);
            scenario.onActivity(a->{
                View target=named(dialog?sourcesDecor(a):a.getWindow().getDecorView(),name);
                if(target==null||!target.isShown()||target.getWindowVisibility()!=View.VISIBLE)return;
                target.requestRectangleOnScreen(new android.graphics.Rect(
                    0,0,target.getWidth(),target.getHeight()),true);
                android.graphics.Rect rect=new android.graphics.Rect();
                visible.set(target.getLocalVisibleRect(rect)
                    &&rect.width()>a.getResources().getDisplayMetrics().density*20
                    &&rect.height()>a.getResources().getDisplayMetrics().density*20);
            });
            ins.waitForIdleSync();
            if(visible.get()){touchable=true;break;}
            Thread.sleep(150);
        }
        assertTrue("Action did not become physically visible/touchable: "+name,touchable);
        float[] point=new float[2];
        scenario.onActivity(a->{
            View target=named(dialog?sourcesDecor(a):a.getWindow().getDecorView(),name);
            android.graphics.Rect visible=new android.graphics.Rect();
            assertTrue("Action outside scroll viewport: "+name,target.getLocalVisibleRect(visible));
            assertTrue(visible.height()>a.getResources().getDisplayMetrics().density*20);
            // Dialog roots are offset from the screen. Inject SCREEN coordinates,
            // not root-relative getGlobalVisibleRect coordinates.
            int[] screen=new int[2];target.getLocationOnScreen(screen);
            point[0]=screen[0]+visible.exactCenterX();point[1]=screen[1]+visible.exactCenterY();
        });
        long now=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,point[0],point[1],0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_UP,point[0],point[1],0);
        try{ins.sendPointerSync(down);ins.sendPointerSync(up);}finally{down.recycle();up.recycle();}
        ins.waitForIdleSync();
    }
    private static void followReading(ActivityScenario<CorrespondenceComparisonActivity> scenario,String action,String ref,int page,boolean dialog)throws Exception{
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor monitor=ins.addMonitor(StudyReaderActivity.class.getName(),null,false);
        try{
            tap(scenario,action,dialog);
            Activity reader=ins.waitForMonitorWithTimeout(monitor,15000);assertNotNull("Reading route must open",reader);
            assertEquals(ref,reader.getIntent().getStringExtra(StudyReaderActivity.EXTRA_JUMP_VERSE));
            assertEquals(page,reader.getIntent().getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,-1));
            ins.runOnMainSync(reader::finish);ins.waitForIdleSync();
        }finally{ins.removeMonitor(monitor);}
    }
    @Test public void eachActiveTafsirHeaderOpensExactVerseInReadingAndReturns()throws Exception{
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(12,3,"R008")){
            scenario.onActivity(a->call(a,"setTafsirOpen",new Class<?>[]{boolean.class},true));ready(scenario);
            scenario.onActivity(a->{step(a,0,-1);step(a,1,1);});ready(scenario);
            followReading(scenario,"Lire le verset 12:2 · A","12:2",235,false);
            followReading(scenario,"Lire le verset 11:121 · B","11:121",235,false);
            assertSelection(scenario,0,new VerseRef(12,2));assertSelection(scenario,1,new VerseRef(11,121));
        }
    }
    private static View startsWith(View v,String description) {
        CharSequence label=v.getContentDescription();
        if(label!=null&&label.toString().startsWith(description))return v;
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View found=startsWith(g.getChildAt(i),description);
                if(found!=null)return found;
            }
        }
        return null;
    }
    @Test public void allFortyFiveRefrainsAreReachableInCompactAtlasRail()throws Exception{
        for(String id:new String[]{"R047","R048","R059"}){
            String[] first=null,last=null;int count=0;
            for(String[] n:RefrainTimelineData.NODES)if(id.equals(n[0])){
                count++;if(first==null)first=n;last=n;
            }
            final String[] initial=first,finalNode=last;
            final int total=count;
            assertEquals("R047".equals(id)?4:"R048".equals(id)?31:10,total);
            String[] ref=initial[5].split(":");
            try(ActivityScenario<CorrespondenceComparisonActivity> scenario=
                    launch(Integer.parseInt(ref[0]),Integer.parseInt(ref[1]),id)){
                ready(scenario);
                screenshot("frise-"+id);
                // A compact rail, not a Sources-dialog accordion. Every actual QCF anchor is present.
                scenario.onActivity(a->{
                    View root=a.getWindow().getDecorView();
                    assertNotNull(startsWith(root,"Refrain 1 sur "+total+", "+initial[5]+" : "));
                    for(String[] n:RefrainTimelineData.NODES){
                        if(!id.equals(n[0]))continue;
                        String prefix="Refrain "+n[3]+" sur "+n[4]+", "+n[5]+" : ";
                        View chip=startsWith(root,prefix);
                        assertNotNull("Missing compact occurrence "+prefix,chip);
                        assertTrue(chip.performClick());
                        RefrainTimeline.State state=(RefrainTimeline.State)field(a,"timelineState");
                        assertEquals(Integer.parseInt(n[3])-1,state.selected);
                        View detail=startsWith(root,"Lire "+n[5]+" dans le Mushaf.");
                        assertNotNull("Selected refrain must expose an exact reading route",detail);
                        assertTrue(((TextView)detail).getText().toString().contains(n[5]));
                    }
                });
                final String[] readingAction=new String[1];
                scenario.onActivity(a->{
                    View detail=startsWith(a.getWindow().getDecorView(),
                        "Lire "+finalNode[5]+" dans le Mushaf.");
                    assertNotNull(detail);
                    readingAction[0]=detail.getContentDescription().toString();
                });
                followReading(scenario,readingAction[0],finalNode[5],
                    Integer.parseInt(finalNode[6]),false);
                ready(scenario);
                screenshot("frise-"+id+"-fin");
                scenario.recreate();ready(scenario);
                scenario.onActivity(a->{
                    assertEquals(total-1,((RefrainTimeline.State)field(a,"timelineState")).selected);
                    assertNotNull(startsWith(a.getWindow().getDecorView(),
                        "Lire "+finalNode[5]+" dans le Mushaf."));
                    call(a,"showSources",new Class<?>[]{});
                    assertNull("Source sheet must not repeat the old accordion",
                        named(sourcesDecor(a),"Frise des refrains"));
                });
            }
        }
    }
}
