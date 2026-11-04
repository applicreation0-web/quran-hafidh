package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual KFQC paired SVG and window layout, phone + BOOX portrait emulator. */
public final class CorrespondenceContextInstrumentedTest {
    @Test public void authenticDoubleQcfAndResponsiveTafsir()throws Exception {
        verifyPair(12,4,"R037","progression");
    }

    @Test public void repetitionAndIndividualEnlargement()throws Exception {
        verifyPair(2,47,"R030","repetition");
    }

    private void verifyPair(int surah,int ayah,String dossier,String capture)throws Exception {
        assertTrue("Isolated workflow must explicitly enable editorial pilot",
            BuildConfig.CORRESPONDENCES_EDITORIAL_PREVIEW);
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=new Intent(context,CorrespondenceComparisonActivity.class)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_SURAH,surah)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_AYAH,ayah)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_ID,dossier)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_ORIGIN,"Lecture");
        try(ActivityScenario<CorrespondenceComparisonActivity> scenario=ActivityScenario.launch(intent)){
            AtomicBoolean rendered=new AtomicBoolean();
            for(int retry=0;retry<90;retry++){
                scenario.onActivity(a->{
                    rendered.set(a.bothAuthenticQcfPanesReadyForTest());
                    assertEquals(2,a.qcfPagesForTest().length);
                    if(rendered.get()){
                        for(MapQcfThumbnails pane:a.qcfPagesForTest()){
                            assertTrue("Original QCF must have measurable width",pane.getWidth()>70);
                            assertTrue("Original QCF must have measurable height",pane.getHeight()>130);
                            assertEquals(View.VISIBLE,pane.getVisibility());
                            assertNotEquals("No synthetic blank stand-ins","none",pane.rendererDiagnosticForTest());
                        }
                        int width=a.getResources().getConfiguration().screenWidthDp;
                        boolean boox=a.getResources().getConfiguration().orientation==
                            android.content.res.Configuration.ORIENTATION_PORTRAIT&&width>=900;
                        if(boox)assertTrue("BOOX portrait opens Jalalayn beneath original QCF",a.booxTafsirOpenForTest());
                    }
                });
                if(rendered.get()){
                    awaitVisualFrame(scenario,2);
                    capture(capture);
                    if("R030".equals(dossier)){
                        AtomicInteger initialWidth=new AtomicInteger();
                        scenario.onActivity(a->{
                            initialWidth.set(a.qcfPagesForTest()[0].getWidth());
                            ArrayList<View> buttons=new ArrayList<>();
                            a.getWindow().getDecorView().findViewsWithText(buttons,
                                "Agrandir le Mushaf A",View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                            assertEquals(1,buttons.size());
                            assertTrue(buttons.get(0).performClick());
                            assertEquals(View.GONE,((View)a.qcfPagesForTest()[1].getParent()).getVisibility());
                        });
                        awaitVisualFrame(scenario,1);
                        scenario.onActivity(a->assertTrue("The selected QCF pane must gain width",
                            a.qcfPagesForTest()[0].getWidth()>initialWidth.get()));
                        capture("agrandissement");
                    }
                    return;
                }
                Thread.sleep(250L);
            }
            fail("Both authentic KFQC pages failed to render");
        }
    }
    private void awaitVisualFrame(ActivityScenario<CorrespondenceComparisonActivity> scenario,
                                  int visiblePanes)throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        CountDownLatch frames=new CountDownLatch(visiblePanes);
        scenario.onActivity(a->{
            MapQcfThumbnails[] panes=a.qcfPagesForTest();
            for(int side=0;side<visiblePanes;side++){
                MapQcfThumbnails pane=panes[side];
                pane.postVisualStateCallback(side,new WebView.VisualStateCallback(){
                    @Override public void onComplete(long requestId){
                        pane.postOnAnimation(()->pane.postOnAnimation(frames::countDown));
                    }
                });
            }
        });
        assertTrue("QCF WebViews must submit the captured frame",frames.await(15,TimeUnit.SECONDS));
    }

    private void capture(String name)throws Exception {
        EmulatorLauncherAnr.requireApplicationForeground(InstrumentationRegistry.getInstrumentation());
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("screencap -p /data/local/tmp/haafidh-"+name+".png");
            java.io.InputStream output=new ParcelFileDescriptor.AutoCloseInputStream(
                ParcelFileDescriptor.dup(fd.getFileDescriptor()))){
            while(output.read()!=-1){ /* Wait for the emulator command to finish. */ }
        }
    }

    @Test public void passiveSelectorFreezesClockOnCancelAndThroughComparison()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        HifzPrefs prefs=new HifzPrefs(c);
        org.json.JSONObject original=HifzBackup.export(c);
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor comparison=
            ins.addMonitor(CorrespondenceComparisonActivity.class.getName(),null,false);
        VerseRef source=new VerseRef(2,47);
        int page=GeometryRepository.get(c).pageForVerse(source);
        try{
            assertTrue(c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE)
                .edit().clear().commit());
            assertTrue(prefs.setV6AcquiredRanges(java.util.Collections.singletonList(
                new com.quransafeguard.hifz.core.VerseRange(source,new VerseRef(2,51))),
                GeometryRepository.get(c)));
            prefs.setMurajaahCursor(source);
            prefs.setMurajaahPage(page);
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(
                    new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,
                        HifzSessionActivity.MURAJAAH))){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Passive Correspondances");
                CorrespondenceCases.Entry pilot=CorrespondenceCases.forVerse(source).get(0);
                // Synthetic second choice exercises the real dialog without changing the catalogue.
                CorrespondenceCases.Entry synthetic=new CorrespondenceCases.Entry(
                    "TEST","TEST","Choix synthétique","TEST","TEST",source,new VerseRef(2,48));
                java.util.List<CorrespondenceCases.Entry> entries=java.util.Arrays.asList(pilot,synthetic);
                java.util.concurrent.atomic.AtomicLong frozen=new java.util.concurrent.atomic.AtomicLong();
                scenario.onActivity(a->{
                    assertTrue(clock(a).isRunning());
                    showSelector(a,source,entries);
                    assertFalse(clock(a).isRunning());
                    frozen.set(clock(a).elapsedMs());
                });
                Thread.sleep(1150L);
                scenario.onActivity(a->{
                    assertEquals(frozen.get(),clock(a).elapsedMs());
                    selector(a).cancel();
                });
                ins.waitForIdleSync();
                scenario.onActivity(a->{
                    assertTrue(clock(a).isRunning());
                    showSelector(a,source,entries);
                    frozen.set(clock(a).elapsedMs());
                    android.widget.ListView list=selector(a).getListView();
                    assertTrue(list.performItemClick(list.getChildAt(0),0,list.getItemIdAtPosition(0)));
                });
                android.app.Activity opened=ins.waitForMonitorWithTimeout(comparison,20000L);
                assertNotNull(opened);
                Thread.sleep(1150L);
                assertEquals(frozen.get(),prefs.elapsedFor(HifzSessionActivity.MURAJAAH));
                assertEquals(page,prefs.murajaahPage());
                ins.runOnMainSync(opened::finish);
                ins.waitForIdleSync();
                scenario.onActivity(a->{
                    assertTrue(clock(a).isRunning());
                    assertEquals(page,((Integer)field(a,"currentPage")).intValue());
                    assertEquals(0,((Integer)field(a,"currentMask")).intValue());
                });
            }
        }finally{
            ins.removeMonitor(comparison);
            HifzBackup.restore(c,original);
        }
    }

    private static Object field(HifzSessionActivity activity,String name){
        try{
            java.lang.reflect.Field field=HifzSessionActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(activity);
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }

    private static SessionClock clock(HifzSessionActivity activity){
        return (SessionClock)field(activity,"clock");
    }

    private static android.app.AlertDialog selector(HifzSessionActivity activity){
        return (android.app.AlertDialog)field(activity,"correspondenceSelector");
    }

    private static void showSelector(HifzSessionActivity activity,VerseRef source,
                                     java.util.List<CorrespondenceCases.Entry> entries){
        try{
            java.lang.reflect.Method select=HifzSessionActivity.class.getDeclaredMethod(
                "selectRevisionCorrespondence",VerseRef.class,java.util.List.class);
            select.setAccessible(true);
            select.invoke(activity,source,entries);
            assertNotNull(selector(activity));
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }

    @Test public void originalVerseAnchorsAreNotGuessed(){
        Context c=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(c);
        int page=geometry.pageForVerse(new VerseRef(12,4));
        assertTrue(CorrespondenceCases.versesOnPage(page,geometry).contains(new VerseRef(12,4)));
        assertFalse(CorrespondenceCases.forVerse(new VerseRef(12,5)).size()>0);
    }
}