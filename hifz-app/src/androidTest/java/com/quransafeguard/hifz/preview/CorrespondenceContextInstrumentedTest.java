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
                    rendered.set(a.bothAuthenticQcfPanesReadyForTest()&&a.visibleTafsirReadyForTest());
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
                    if("R037".equals(dossier)){
                        scenario.onActivity(a->{
                            if(!a.booxTafsirOpenForTest()){
                                ArrayList<View> buttons=new ArrayList<>();
                                a.getWindow().getDecorView().findViewsWithText(buttons,
                                    "Afficher ou masquer le Tafsīr",View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                                assertEquals(1,buttons.size());
                                assertTrue(buttons.get(0).performClick());
                            }
                        });
                        AtomicBoolean commentary=new AtomicBoolean();
                        for(int attempt=0;attempt<60&&!commentary.get();attempt++){
                            scenario.onActivity(a->commentary.set(a.visibleTafsirReadyForTest()));
                            if(!commentary.get())android.os.SystemClock.sleep(100L);
                        }
                        assertTrue("Jalalayn must finish loading before capture",commentary.get());
                        awaitVisualFrame(scenario,2);
                        capture("tafsir");
                    }
                    if("R030".equals(dossier)){
                        AtomicInteger initialWidth=new AtomicInteger();
                        AtomicInteger initialInkWidth=new AtomicInteger();
                        CountDownLatch firstSvgMeasured=new CountDownLatch(1);
                        scenario.onActivity(a->a.qcfPagesForTest()[0].evaluateJavascript(
                            "(function(){var s=document.querySelector('.paper svg');return s?Math.round(s.getBoundingClientRect().width):0;})()",
                            value->{
                                try{initialInkWidth.set(Integer.parseInt(value));}
                                catch(NumberFormatException ignored){}
                                firstSvgMeasured.countDown();
                            }));
                        assertTrue(firstSvgMeasured.await(10,TimeUnit.SECONDS));
                        assertTrue("Initial real QCF ink must already be visible",initialInkWidth.get()>40);
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
                        scenario.onActivity(a->{
                            assertTrue("The selected QCF WebView must gain width",
                                a.qcfPagesForTest()[0].getWidth()>initialWidth.get());
                            assertFalse("Focus must reclaim the Tafsir's unused height",
                                a.booxTafsirOpenForTest());
                        });
                        // The old BOOX screenshot only expanded the container,
                        // leaving the Arabic itself at half-page width. Measure the
                        // actual imported QCF SVG after a real layout frame.
                        final java.util.concurrent.atomic.AtomicInteger renderedInk=
                            new java.util.concurrent.atomic.AtomicInteger();
                        final CountDownLatch svgMeasured=new CountDownLatch(1);
                        scenario.onActivity(a->a.qcfPagesForTest()[0].evaluateJavascript(
                            "(function(){var p=document.querySelector('.paper svg');return p?Math.round(p.getBoundingClientRect().width):0;})()",
                            value->{
                                try{renderedInk.set(Integer.parseInt(value));}
                                catch(NumberFormatException ignored){}
                                svgMeasured.countDown();
                            }));
                        assertTrue(svgMeasured.await(10,TimeUnit.SECONDS));
                        assertTrue("Focus must visibly enlarge the authentic QCF SVG (before="+initialInkWidth.get()+
                            ", after="+renderedInk.get()+")",
                            renderedInk.get()>=Math.round(initialInkWidth.get()*1.15f));
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
        EmulatorLauncherAnr.requireApplicationForeground(InstrumentationRegistry.getInstrumentation());
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
        for(int side=0;side<visiblePanes;side++){
            int index=side;
            AtomicBoolean folio=new AtomicBoolean();
            CountDownLatch measured=new CountDownLatch(1);
            scenario.onActivity(a->a.qcfPagesForTest()[index].evaluateJavascript(
                "(function(){var s=document.querySelector('.paper svg');if(!s)return false;"
                +"var r=s.getBoundingClientRect(),v=s.viewBox.baseVal,m=s.getScreenCTM();"
                +"return r.width>40&&r.height>60&&v.width===345&&v.height===550"
                +"&&!!m&&Math.abs(m.a-m.d)<0.001"
                +"&&!!s.querySelector('path.ayahPolygon');})()",
                value->{folio.set("true".equals(value));measured.countDown();}));
            assertTrue(measured.await(10,TimeUnit.SECONDS));
            assertTrue("Visible folio must keep its original proportions and QCF polygons",folio.get());
        }
    }

    private void capture(String name)throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        EmulatorLauncherAnr.requireApplicationForeground(instrumentation);
        String path="/data/local/tmp/haafidh-"+name+".png";
        long deadline=android.os.SystemClock.uptimeMillis()+15000L;
        do{
            try(java.io.InputStream output=shellOutput("screencap -p "+path)){
                while(output.read()!=-1){ /* Wait for SurfaceFlinger capture to finish. */ }
            }
            try(java.io.InputStream input=shellOutput("cat "+path)){
                android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(input);
                if(bitmap!=null){
                    try{
                        if(hasExpectedFolioInk(name,bitmap))return;
                    }finally{bitmap.recycle();}
                }
            }
            android.os.SystemClock.sleep(220L);
        }while(android.os.SystemClock.uptimeMillis()<deadline);
        fail("Original Android PNG did not contain the expected painted QCF folios: "+name);
    }

    private java.io.InputStream shellOutput(String command){
        return new ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command));
    }

    private boolean hasExpectedFolioInk(String name,android.graphics.Bitmap bitmap){
        switch(name){
            case "lecture-reperes":
            case "retour-lecture":
                // A real reader folio has continuous Arabic across the screen centre.
                return hasFolioInk(bitmap,.48f,.52f,40);
            case "agrandissement":
                // A stale half-width folio on the left cannot satisfy the wider crop.
                return hasFolioInk(bitmap,.27f,.42f,60)&&hasFolioInk(bitmap,.58f,.72f,60);
            default:
                // Reject an empty pane or one single "Chargement du Mushaf QCF" label.
                return hasFolioInk(bitmap,.10f,.40f,100)&&hasFolioInk(bitmap,.60f,.90f,100);
        }
    }

    private boolean hasFolioInk(android.graphics.Bitmap bitmap,float left,float right,int minimum){
        int width=bitmap.getWidth(),height=bitmap.getHeight();
        for(int band=0;band<2;band++){
            int top=Math.round(height*(.34f+band*.115f));
            int bottom=Math.round(height*(.455f+band*.115f));
            int dark=0;
            for(int y=top;y<bottom;y+=3)
                for(int x=Math.round(width*left);x<Math.round(width*right);x+=3){
                    int pixel=bitmap.getPixel(x,y);
                    if(android.graphics.Color.red(pixel)<125&&android.graphics.Color.green(pixel)<125
                        &&android.graphics.Color.blue(pixel)<125)dark++;
                }
            if(dark<minimum)return false;
        }
        return true;
    }

    private void awaitReaderFrame(ActivityScenario<StudyReaderActivity> scenario)throws Exception {
        AtomicBoolean focused=new AtomicBoolean();
        for(int attempt=0;attempt<50&&!focused.get();attempt++){
            scenario.onActivity(a->focused.set(a.hasWindowFocus()));
            if(!focused.get())android.os.SystemClock.sleep(100L);
        }
        assertTrue("Lecture must own the focused window before the capture",focused.get());
        CountDownLatch drawn=new CountDownLatch(1);
        scenario.onActivity(a->{
            MushafView mushaf=(MushafView)readField(a,"mushaf");
            mushaf.postVisualStateCallback(0,new WebView.VisualStateCallback(){
                @Override public void onComplete(long id){
                    mushaf.postOnAnimation(()->mushaf.postOnAnimation(drawn::countDown));
                }
            });
        });
        assertTrue("Lecture QCF frame must be drawable",drawn.await(10,TimeUnit.SECONDS));
    }

    @Test public void readerGutterOpensCorrespondenceAndReturnsToSameFolio()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        int page=GeometryRepository.get(c).pageForVerse(new VerseRef(2,47));
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor monitor=
            ins.addMonitor(CorrespondenceComparisonActivity.class.getName(),null,false);
        try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(
                new Intent(c,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page))){
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Lecture · repère R030");
            awaitReaderFrame(scenario);
            capture("lecture-reperes");
            java.util.concurrent.atomic.AtomicReference<float[]> point=new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<String> diagnostic=new java.util.concurrent.atomic.AtomicReference<>();
            CountDownLatch located=new CountDownLatch(1);
            scenario.onActivity(a->{
                MushafView mushaf=(MushafView)readField(a,"mushaf");
                mushaf.postVisualStateCallback(0,new WebView.VisualStateCallback(){
                    @Override public void onComplete(long requestId){
                        mushaf.evaluateJavascript(
                            "(function(){var b=document.querySelector('button[aria-label=\"Correspondances 2:47\"]');"
                            +"if(!b||getComputedStyle(b).visibility!=='visible')return null;"
                            +"var r=b.getBoundingClientRect(),q=document.querySelector('#mushaf').getBoundingClientRect();"
                            +"var x=r.left+r.width/2,y=r.top+r.height/2;"
                            +"return [x,y,innerWidth,innerHeight,r.left>=q.right,b.contains(document.elementFromPoint(x,y))];})()",
                            value->{
                                try{
                                    org.json.JSONArray bounds=new org.json.JSONArray(value);
                                    int[] offset=new int[2];
                                    mushaf.getLocationOnScreen(offset);
                                    float x=offset[0]+(float)(bounds.getDouble(0)*mushaf.getWidth()/bounds.getDouble(2));
                                    float y=offset[1]+(float)(bounds.getDouble(1)*mushaf.getHeight()/bounds.getDouble(3));
                                    android.graphics.Rect frame=new android.graphics.Rect();
                                    mushaf.getWindowVisibleDisplayFrame(frame);
                                    diagnostic.set("DOM="+bounds+" screen="+x+","+y+" window="+frame
                                        +" focus="+a.hasWindowFocus()+" layoutRequested="+mushaf.isLayoutRequested());
                                    if(bounds.getBoolean(4)&&bounds.getBoolean(5)&&a.hasWindowFocus()
                                        &&!mushaf.isLayoutRequested()&&frame.contains((int)x,(int)y)){
                                        point.set(new float[]{x,y});
                                    }
                                }catch(org.json.JSONException invalid){diagnostic.set(value);}
                                located.countDown();
                            });
                    }
                });
            });
            assertTrue(located.await(10,TimeUnit.SECONDS));
            android.util.Log.i("HaafidhGutterTouch",String.valueOf(diagnostic.get()));
            assertNotNull("Visible R030 touch target must be outside Arabic and own the hit point: "
                +diagnostic.get(),point.get());
            long now=android.os.SystemClock.uptimeMillis();
            android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,
                android.view.MotionEvent.ACTION_DOWN,point.get()[0],point.get()[1],0);
            down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try{ins.sendPointerSync(down);}
            finally{down.recycle();}
            android.os.SystemClock.sleep(80L);
            android.view.MotionEvent up=android.view.MotionEvent.obtain(now,android.os.SystemClock.uptimeMillis(),
                android.view.MotionEvent.ACTION_UP,point.get()[0],point.get()[1],0);
            up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
            try{ins.sendPointerSync(up);}
            finally{up.recycle();}
            android.app.Activity opened=ins.waitForMonitorWithTimeout(monitor,20000);
            assertNotNull("Gutter touch must open the comparison: "+diagnostic.get(),opened);
            ins.runOnMainSync(opened::finish);
            ins.waitForIdleSync();
            scenario.onActivity(a->assertEquals(page,((Integer)readField(a,"page")).intValue()));
            awaitReaderFrame(scenario);
            capture("retour-lecture");
        }finally{ins.removeMonitor(monitor);}
    }

    @Test public void passiveSelectorFreezesClockOnCancelAndThroughComparison()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        new HifzPrefs(c);
        org.json.JSONObject original=HifzBackup.export(c);
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor comparison=
            ins.addMonitor(CorrespondenceComparisonActivity.class.getName(),null,false);
        VerseRef source=new VerseRef(2,47);
        int page=GeometryRepository.get(c).pageForVerse(source);
        try{
            assertTrue(c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE)
                .edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(c);
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
                // ActivityScenario callbacks can run before the last BOOX lifecycle
                // transition. Wait for a real monotonic running clock, not a single
                // scheduler tick, then verify the exact restored reading context.
                AtomicBoolean resumed=new AtomicBoolean();
                for(int retry=0;retry<40;retry++){
                    scenario.onActivity(a->resumed.set(clock(a).isRunning()));
                    if(resumed.get())break;
                    Thread.sleep(100L);
                }
                assertTrue("Passive review clock must restart on return from a real comparison",resumed.get());
                scenario.onActivity(a->{
                    assertEquals(page,((Integer)readField(a,"currentPage")).intValue());
                    assertEquals(0,((Integer)readField(a,"currentMask")).intValue());
                });
            }
        }finally{
            ins.removeMonitor(comparison);
            HifzBackup.restore(c,original);
        }
    }

    @Test public void activeCorrespondenceRequiresPersistedValidationAndSurvivesRecreation()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        new HifzPrefs(c);
        org.json.JSONObject original=HifzBackup.export(c);
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor comparison=
            ins.addMonitor(CorrespondenceComparisonActivity.class.getName(),null,false);
        VerseRef source=new VerseRef(2,47),end=new VerseRef(2,48);
        GeometryRepository geometry=GeometryRepository.get(c);
        String date="2026-10-29";
        try{
            assertTrue(c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE)
                .edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(c);
            assertTrue(prefs.setV6AcquiredRanges(java.util.Collections.singletonList(
                new com.quransafeguard.hifz.core.VerseRange(source,new VerseRef(2,51))),geometry));
            android.content.SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
            String originalCredit=raw.getString("v6AcquiredCreditLineIds",null);
            assertNotNull(originalCredit);
            VerseRef expectedCursor=new VerseRef(2,49);
            prefs.setActiveMurajaahCursor(source);
            prefs.setActiveMurajaahPage(geometry.pageForVerse(source));
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(
                    new Intent(c,HifzSessionActivity.class)
                        .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                        .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,date))){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Active Correspondances");
                scenario.onActivity(a->{
                    a.onVerseTap(end);
                    a.onCorrespondenceTap(source);
                    assertEquals(View.GONE,((View)readField(a,"validatedCorrespondenceButton")).getVisibility());
                    assertEquals("",prefs.lastActiveMurajaahDate());
                });
                assertNull("A bridge tap cannot reveal an unvalidated dossier",
                    comparison.waitForActivityWithTimeout(500L));
                scenario.onActivity(a->{
                    ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(c);
                    int current=(Integer)readField(a,"currentPage");
                    if(!store.revealed(date,source).contains(current)){
                        android.widget.Button reveal=findAction(a,"Révéler");
                        assertTrue(reveal.isEnabled());
                        long now=android.os.SystemClock.uptimeMillis();
                        for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
                            android.view.MotionEvent event=android.view.MotionEvent.obtain(now,
                                android.os.SystemClock.uptimeMillis(),action,1,1,0);
                            try{assertTrue(reveal.dispatchTouchEvent(event));}
                            finally{event.recycle();}
                        }
                    }
                    assertTrue("A real reveal must be persisted before validation",
                        store.revealed(date,source).contains(current));
                    View validate=(View)readField(a,"murajaahFinishButton");
                    assertNotNull("The active session must expose its validation action",validate);
                    assertTrue(validate.isEnabled());
                    assertTrue(validate.performClick());
                    // Click the genuine confirmation button in Android's own
                    // AlertDialog. Do not bypass the actual production receipt path
                    // or insert a synthetic credit into SharedPreferences.
                    android.app.AlertDialog prompt=(android.app.AlertDialog)
                        readField(a,"murajaahPartialValidationDialog");
                    if(prompt!=null&&prompt.isShowing()){
                        android.widget.Button positive=prompt.getButton(
                            android.content.DialogInterface.BUTTON_POSITIVE);
                        assertNotNull("Real partial-validation button must exist",positive);
                        assertTrue("Real confirmation must be enabled",positive.isEnabled());
                        assertTrue("Only actual user confirmation may validate",positive.performClick());
                    }
                });
                // The authentic QCF may be revealed by the fail-open landmark
                // policy when exact word geometry is unavailable. This is a real
                // revealed page, not a missed Hifz receipt. The production flow
                // MUST require visible rereading and then fully masked recall.
                ActiveReviewCorrectionStore correction=new ActiveReviewCorrectionStore(c);
                // Android dialog dismissal and the QCF correction transition can be
                // posted after the original button callback. A single immediate
                // preferences read races that transition on e-ink-size emulators.
                // An immediate receipt fails because the reveal was recorded above.
                ActiveReviewCorrectionStore.State pending=null;
                for(int wait=0;wait<120;wait++){
                    ins.waitForIdleSync();
                    pending=correction.pending();
                    if(pending!=null||date.equals(prefs.lastActiveMurajaahDate()))break;
                    Thread.sleep(125L);
                }
                assertNotNull("A revealed page must reach persisted corrective replay before any receipt",pending);
                assertEquals("Missing QCF anchors must never bypass the reread", "READ",pending.stage);
                assertEquals(date,pending.date);
                assertEquals("",prefs.lastActiveMurajaahDate());
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    scenario,"mushaf","Correspondances · vraie relecture corrective");
                scenario.onActivity(a->{
                    assertEquals(0,((Integer)readField(a,"currentMask")).intValue());
                    android.widget.Button read=findAction(a,"Page relue");
                    assertTrue(read.isEnabled());
                    assertTrue("Real visible reread is mandatory",read.performClick());
                });
                assertEquals("RECALL",correction.pending().stage);
                assertEquals("Reread alone never earns a receipt","",prefs.lastActiveMurajaahDate());
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    scenario,"mushaf","Correspondances · vrai rappel masqué");
                // Never fake onPageShown or set the readiness flag manually.
                // Wait for the real QCF mask to paint before its button enables.
                boolean painted=false;
                for(int wait=0;wait<80&&!painted;wait++){
                    java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
                    scenario.onActivity(a->{
                        android.widget.Button done=(android.widget.Button)
                            readField(a,"correctionPageDoneButton");
                        ready.set(done!=null&&done.isEnabled()
                            &&((Integer)readField(a,"currentMask")).intValue()==100
                            &&(Boolean)readField(a,"correctionMaskedPageReady")
                            &&((View)readField(a,"validatedCorrespondenceButton")).getVisibility()==View.GONE);
                    });
                    painted=ready.get();
                    if(!painted)Thread.sleep(125L);
                }
                assertTrue("Original QCF mask must be painted before unaided validation",painted);
                scenario.onActivity(a->{
                    assertEquals("RECALL",correction.pending().stage);
                    assertFalse("No assistance may be counted as unaided recall",correction.pending().assisted);
                    assertTrue("Only a real validated recall may earn a receipt",
                        findAction(a,"Page récitée sans aide").performClick());
                });
                long deadline=android.os.SystemClock.uptimeMillis()+8000L;
                while(!date.equals(prefs.lastActiveMurajaahDate())
                      &&android.os.SystemClock.uptimeMillis()<deadline)Thread.sleep(100L);
                java.util.concurrent.atomic.AtomicReference<String> afterReplay=
                    new java.util.concurrent.atomic.AtomicReference<>("unavailable");
                scenario.onActivity(a->{
                    ActiveReviewCorrectionStore.State state=correction.pending();
                    afterReplay.set("correctionStage="+(state==null?"CLEARED":state.stage)
                        +" assisted="+(state==null?"n/a":state.assisted)
                        +" actDate="+prefs.lastActiveMurajaahDate()
                        +" actCursor="+prefs.activeMurajaahCursor()
                        +" actualEnd="+prefs.activeMurajaahActualEnd()
                        +" sessionDate="+readField(a,"sessionDate")
                        +" complete="+readField(a,"sessionCompleted")
                        +" renderFailed="+readField(a,"sessionRenderFailed")
                        +" selected="+readField(a,"validatedCorrespondenceVerse")
                        +" page="+readField(a,"currentPage")
                        +" program="+((android.widget.TextView)readField(a,"program")).getText()
                        +" progress="+((android.widget.TextView)readField(a,"progress")).getText());
                });
                assertEquals("Active receipt only after actual corrective replay; "+afterReplay.get(),
                    date,prefs.lastActiveMurajaahDate());
                assertEquals(expectedCursor,prefs.activeMurajaahCursor());
                assertEquals(originalCredit,raw.getString("v6AcquiredCreditLineIds",null));
                assertNull("Reread/recall checkpoint must be cleared after real validation",
                    correction.pending());
                scenario.onActivity(a->assertEquals(View.VISIBLE,
                    ((View)readField(a,"validatedCorrespondenceButton")).getVisibility()));
                scenario.recreate();
                scenario.onActivity(a->{
                    assertEquals(1,((Integer)readField(a,"currentPage")).intValue());
                    View button=(View)readField(a,"validatedCorrespondenceButton");
                    assertEquals(View.VISIBLE,button.getVisibility());
                    assertTrue(button.performClick());
                });
                android.app.Activity opened=ins.waitForMonitorWithTimeout(comparison,20000L);
                assertNotNull(opened);
                ins.runOnMainSync(opened::finish);
                ins.waitForIdleSync();
                scenario.onActivity(a->{
                    assertEquals(1,((Integer)readField(a,"currentPage")).intValue());
                    assertFalse(clock(a).isRunning());
                    assertEquals(date,prefs.lastActiveMurajaahDate());
                    assertEquals(expectedCursor,prefs.activeMurajaahCursor());
                    assertEquals(originalCredit,raw.getString("v6AcquiredCreditLineIds",null));
                });
            }
        }finally{
            ins.removeMonitor(comparison);
            HifzBackup.restore(c,original);
        }
    }

    private static android.widget.Button findAction(HifzSessionActivity activity,String name){
        android.widget.LinearLayout actions=(android.widget.LinearLayout)readField(activity,"actions");
        for(int i=0;i<actions.getChildCount();i++){
            View row=actions.getChildAt(i);
            if(!(row instanceof android.view.ViewGroup))continue;
            android.view.ViewGroup group=(android.view.ViewGroup)row;
            for(int j=0;j<group.getChildCount();j++){
                View candidate=group.getChildAt(j);
                if(candidate instanceof android.widget.Button
                    &&name.contentEquals(candidate.getContentDescription()))
                    return (android.widget.Button)candidate;
            }
        }
        throw new AssertionError("Required genuine Hifz action not found: "+name);
    }

    private static Object readField(Object activity,String name){
        try{
            java.lang.reflect.Field field=activity.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(activity);
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }

    private static SessionClock clock(HifzSessionActivity activity){
        return (SessionClock)readField(activity,"clock");
    }

    private static android.app.AlertDialog selector(HifzSessionActivity activity){
        return (android.app.AlertDialog)readField(activity,"correspondenceSelector");
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