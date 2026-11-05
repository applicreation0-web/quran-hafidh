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
    private static TextView find(View v,String contains){
        if(v instanceof TextView&&((TextView)v).getText().toString().contains(contains))return (TextView)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView t=find(g.getChildAt(i),contains);if(t!=null)return t;}}
        return null;
    }
    @Test public void allFortyFiveRefrainsAreReachableInsideSourcesOnly()throws Exception{
        for(String id:new String[]{"R047","R048","R059"}){
            String[] first=null;int count=0;
            for(String[] n:RefrainTimelineData.NODES)if(id.equals(n[0])){count++;if(first==null)first=n;}
            final String[] anchor=first;final int total=count;
            String[] ref=anchor[5].split(":");
            try(ActivityScenario<CorrespondenceComparisonActivity> scenario=launch(Integer.parseInt(ref[0]),Integer.parseInt(ref[1]),id)){
                ready(scenario);
                scenario.onActivity(a->{
                    call(a,"showSources",new Class<?>[]{});
                    android.app.AlertDialog dialog=(android.app.AlertDialog)field(a,"sourcesDialog");View decor=dialog.getWindow().getDecorView();
                    TextView opener=find(decor,"Frise des refrains");assertNotNull(opener);opener.performClick();
                    assertNotNull(find(decor,total+" occurrences"));
                    for(String[] node:RefrainTimelineData.NODES)if(id.equals(node[0])){
                        TextView station=find(decor,node[7]);assertNotNull(station);
                        View occurrence=find(decor,node[3]+"/"+node[4]+" · "+node[5]);assertNotNull(occurrence);
                        if(((View)occurrence.getParent()).getVisibility()!=View.VISIBLE)station.performClick();
                        occurrence.performClick();assertNotNull(find(decor,"Avant · "+node[8]));assertNotNull(find(decor,"Après · "+node[11]));
                        assertNotNull(find(decor,"Lire "+node[5]));occurrence.performClick();
                    }
                });
                screenshot("frise-"+id);
                scenario.recreate();ready(scenario);
                scenario.onActivity(a->{assertTrue(((RefrainTimeline.State)field(a,"timelineState")).open);assertTrue((Boolean)field(a,"sourcesOpen"));});
            }
        }
    }
}
