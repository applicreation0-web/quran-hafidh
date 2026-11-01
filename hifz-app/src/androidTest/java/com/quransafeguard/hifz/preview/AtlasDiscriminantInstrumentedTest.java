package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.os.Environment;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real Android UI: two actual QCF page WebViews, one two-strip comparison, read-only Hifz. */
public final class AtlasDiscriminantInstrumentedTest {
    private static int exactPairIndex(int firstSurah,int firstAyah,int secondSurah,int secondAyah){
        for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++){
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(i);
            if(pair.first.getSurah()==firstSurah&&pair.first.getAyah()==firstAyah
                &&pair.second.getSurah()==secondSurah&&pair.second.getAyah()==secondAyah)return i;
        }
        throw new AssertionError("Required audited pair missing: "+firstSurah+":"+firstAyah+
            " / "+secondSurah+":"+secondAyah);
    }
    private static AtlasDiscriminantCases.Entry samplePair(){
        return AtlasDiscriminantCases.PILOT.get(exactPairIndex(2,63,2,93));
    }
    private static Intent pairIntent(Context context,int index){
        AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(index);
        return new Intent(context,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,pair.first.getSurah())
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,pair.first.getAyah())
            .putExtra(AtlasDiscriminantActivity.EXTRA_CASE_INDEX,index)
            .putExtra(AtlasDiscriminantActivity.EXTRA_ORIGIN,"Lecture");
    }

    private static int countViews(View root,Class<?> expected) {
        int count=expected.isInstance(root)?1:0;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++)count+=countViews(group.getChildAt(i),expected);
        }
        return count;
    }

    private static void waitForQcf(ActivityScenario<AtlasDiscriminantActivity> scenario) throws Exception {
        AtomicBoolean ready=new AtomicBoolean();
        for(int attempt=0;attempt<60;attempt++){
            scenario.onActivity(a->{
                assertEquals(a.qcfFailureDetailForTest(),0,a.qcfPagesFailedForTest());
                ready.set(a.qcfPagesReadyForTest()&&a.qcfStripsReadyForTest());
            });
            if(ready.get())return;
            Thread.sleep(250L);
        }
        fail("Authentic paired QCF rendering timed out");
    }

    /** Read geometry only after consecutive drawn frames, including the deferred page fit. */
    private static Rect[] waitForStablePageBounds(
        ActivityScenario<AtlasDiscriminantActivity> scenario,boolean contextual)throws Exception{
        java.util.concurrent.CountDownLatch settled=new java.util.concurrent.CountDownLatch(1);
        AtomicReference<Rect[]> result=new AtomicReference<>();
        AtomicReference<String> lastFrame=new AtomicReference<>("No pre-draw frame observed");
        AtomicReference<android.view.ViewTreeObserver.OnPreDrawListener> observer=new AtomicReference<>();
        scenario.onActivity(a->{
            View root=a.getWindow().getDecorView();
            android.view.ViewTreeObserver.OnPreDrawListener listener=new android.view.ViewTreeObserver.OnPreDrawListener(){
                private Rect[] previous;
                private int stableFrames;
                private String previousWindow;
                private long stableSince;
                @Override public boolean onPreDraw(){
                    Rect[] bounds=a.qcfPageBoundsForTest();
                    Rect visibleWindow=new Rect();root.getWindowVisibleDisplayFrame(visibleWindow);
                    android.view.WindowInsets insets=root.getRootWindowInsets();
                    int orientation=a.getResources().getConfiguration().orientation;
                    boolean portrait=orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT;
                    String window=orientation+":"+root.getDisplay().getRotation()+":"+
                        root.getWidth()+":"+root.getHeight()+":"+visibleWindow+":"+insets;
                    boolean ready=a.hasWindowFocus()&&root.hasWindowFocus()&&root.isAttachedToWindow()
                        &&insets!=null&&!visibleWindow.isEmpty()
                        &&(portrait?root.getHeight()>=root.getWidth():root.getWidth()>=root.getHeight())
                        &&!root.isLayoutRequested()&&a.bothQcfPagesFullyVisibleForTest()
                        &&(!contextual||a.tafsirHeadingVisibleForTest());
                    for(MapQcfThumbnails page:a.qcfViewsForTest())ready&=!page.isLayoutRequested();
                    // Failure diagnostics only: preserve every strict visual gate.
                    StringBuilder clips=new StringBuilder();
                    for(MapQcfThumbnails page:a.qcfViewsForTest()){
                        Rect clipped=new Rect();
                        boolean exposed=page.getGlobalVisibleRect(clipped);
                        clips.append(" exposed=").append(exposed)
                            .append(" measured=").append(page.getWidth()).append('x').append(page.getHeight())
                            .append(" visible=").append(clipped)
                            .append(" parent=").append(page.getParent());
                    }
                    lastFrame.set("activityFocus="+a.hasWindowFocus()
                        +" rootFocus="+root.hasWindowFocus()
                        +" attached="+root.isAttachedToWindow()
                        +" insetsReady="+(insets!=null)
                        +" rootLayoutRequested="+root.isLayoutRequested()
                        +" leftLayoutRequested="+a.qcfViewsForTest()[0].isLayoutRequested()
                        +" rightLayoutRequested="+a.qcfViewsForTest()[1].isLayoutRequested()
                        +" bothVisible="+a.bothQcfPagesFullyVisibleForTest()
                        +" scrollY="+a.comparisonScrollYForTest()
                        +" firstSize="+a.qcfViewsForTest()[0].getWidth()+"x"+a.qcfViewsForTest()[0].getHeight()
                        +" secondSize="+a.qcfViewsForTest()[1].getWidth()+"x"+a.qcfViewsForTest()[1].getHeight()
                        +" headingVisible="+a.tafsirHeadingVisibleForTest()
                        +" orientation="+orientation
                        +" root="+root.getWidth()+"x"+root.getHeight()
                        +" visibleWindow="+visibleWindow
                        +" bounds="+java.util.Arrays.toString(bounds)
                        +" clipDiagnostic="+clips
                        +" viewport="+a.qcfViewportDetailForTest());
                    boolean same=ready&&java.util.Arrays.equals(previous,bounds)&&window.equals(previousWindow);
                    if(!same)stableSince=android.os.SystemClock.uptimeMillis();
                    stableFrames=same?stableFrames+1:0;
                    previous=bounds;previousWindow=window;
                    result.set(bounds);
                    if(stableFrames>=4&&android.os.SystemClock.uptimeMillis()-stableSince>=250L){
                        root.getViewTreeObserver().removeOnPreDrawListener(this);
                        settled.countDown();
                    }else root.postInvalidateOnAnimation();
                    return true;
                }
            };
            observer.set(listener);
            root.getViewTreeObserver().addOnPreDrawListener(listener);
            root.postInvalidateOnAnimation();
        });
        // A Pixel Launcher ANR on Google API emulator must not steal the target
        // window's focus. Recover only the exact unrelated *system* Wait dialog.
        long deadline=android.os.SystemClock.uptimeMillis()+18000L;
        boolean ready=false;
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        while(android.os.SystemClock.uptimeMillis()<deadline){
            if(settled.await(200,java.util.concurrent.TimeUnit.MILLISECONDS)){ready=true;break;}
            EmulatorLauncherAnr.resumeIfPresent(ins);
        }
        scenario.onActivity(a->a.getWindow().getDecorView().getViewTreeObserver()
            .removeOnPreDrawListener(observer.get()));
        if(!ready)
            HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("atlas-qcf-viewport-failure.png");
        assertTrue("Focused activity, orientation, insets and complete QCF pages must settle after layout: "+
            java.util.Arrays.toString(result.get())+"; final pre-draw: "+lastFrame.get(),ready);
        return result.get();
    }

    private static void assertContextPanesVisible(ActivityScenario<AtlasDiscriminantActivity> scenario)throws Exception{
        waitForStablePageBounds(scenario,true);
    }

    private static void waitForSelectedTafsir(
        ActivityScenario<AtlasDiscriminantActivity> scenario,int side)throws Exception{
        AtomicBoolean selected=new AtomicBoolean();
        for(int attempt=0;attempt<40;attempt++){
            scenario.onActivity(a->selected.set(a.tafsirPanelVisibleForTest()&&a.selectedTafsirForTest()==side));
            if(selected.get())return;
            Thread.sleep(100L);
        }
        fail("Touching the actual QCF page must select tafsir side "+side);
    }

    private static void switchTafsirAndAssertPagesRetained(
        ActivityScenario<AtlasDiscriminantActivity> scenario,int side,
        MapQcfThumbnails[] originalPages)throws Exception{
        Rect[] before=waitForStablePageBounds(scenario,true);
        scenario.onActivity(a->{
            View tab=findByDescription(a.getWindow().getDecorView(),side==0?
                "Tafsīr du premier verset":"Tafsīr du second verset");
            assertNotNull(tab);assertTrue(tab.performClick());
        });
        Rect[] after=waitForStablePageBounds(scenario,true);
        scenario.onActivity(a->{
            assertEquals(side,a.selectedTafsirForTest());
            assertSame("Switching commentary must retain first QCF WebView",originalPages[0],a.qcfViewsForTest()[0]);
            assertSame("Switching commentary must retain second QCF WebView",originalPages[1],a.qcfViewsForTest()[1]);
            assertTrue(a.qcfPagesReadyForTest());
        });
        assertArrayEquals("Both QCF localizations must stay at the same settled bounds",before,after);
    }

    private static void assertOriginalPageShapeAndWordMarks(
        ActivityScenario<AtlasDiscriminantActivity> scenario,AtlasDiscriminantCases.Entry expected)throws Exception{
        java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(2);
        AtomicReference<String> first=new AtomicReference<>(),second=new AtomicReference<>();
        scenario.onActivity(a->{
            MapQcfThumbnails[] pages=a.qcfViewsForTest();
            for(int side=0;side<2;side++){
                final AtomicReference<String> result=side==0?first:second;
                pages[side].evaluateJavascript("(function(){var svg=document.querySelector('.paper svg');"
                    +"if(!svg)return '';return svg.getAttribute('viewBox')+'|'+"
                    +"svg.querySelectorAll('#atlas-exact-word-differences line').length;})()",
                    value->{result.set(value);latch.countDown();});
            }
        });
        assertTrue("QCF DOM audit callback timed out",latch.await(5,java.util.concurrent.TimeUnit.SECONDS));
        int side=0;
        for(String value:new String[]{first.get(),second.get()}){
            String actual=new org.json.JSONArray("["+value+"]").getString(0);
            String[] parts=actual.split("\\|");
            String[] box=parts[0].trim().split("\\s+");
            assertEquals("Original QCF page width must remain 345",345,Double.parseDouble(box[2]),0.001);
            assertEquals("Original QCF page height must remain 550",550,Double.parseDouble(box[3]),0.001);
            assertEquals("QCF underlines reflect real present words, never a common-word anchor",
                AtlasDiscriminantOmissions.actualDifference(expected,side==0).length,
                Integer.parseInt(parts[1]));
            side++;
        }
    }

    /** Resolve screen coordinates from the current focused window after rotation/insets settle. */
    private static void tapQcfPage(ActivityScenario<AtlasDiscriminantActivity> scenario,
        int side,boolean contextual)throws Exception{
        tapQcfPage(scenario,side,contextual,null);
    }
    private static void tapQcfPage(ActivityScenario<AtlasDiscriminantActivity> scenario,
        int side,boolean contextual,com.quransafeguard.hifz.core.VerseRef requested)throws Exception{
        waitForStablePageBounds(scenario,contextual);
        Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
        instrument.waitForIdleSync();
        // Full-page centres can fall in a surah title gap (reproduced on page 385).
        // Resolve a point INSIDE the requested original ayah polygon, then inject
        // a real Android touch using the current WebView CSS/display conversion.
        AtomicReference<org.json.JSONObject> polygonPoint=new AtomicReference<>();
        if(requested!=null){
            java.util.concurrent.CountDownLatch found=new java.util.concurrent.CountDownLatch(1);
            AtomicReference<String> coordinates=new AtomicReference<>();
            String script="(function(){var svg=document.querySelector('.paper svg');if(!svg)return '';"
                +"var p=svg.querySelector('path.ayahPolygon[surah=\""+requested.getSurah()+"\"][ayah=\""+requested.getAyah()+"\"]');if(!p)return '';"
                +"var b=p.getBBox(),m=p.getScreenCTM();if(!m)return '';"
                +"var fractions=[0.5,0.375,0.625,0.25,0.75,0.125,0.875];"
                +"var all=svg.querySelectorAll('path.ayahPolygon');"
                +"function winner(q){for(var k=all.length-1;k>=0;k--){var c=all[k],ct=c.getScreenCTM();"
                +"if(ct&&c.isPointInFill(q.matrixTransform(ct.inverse())))return c;}return null;}"
                +"for(var x of fractions)for(var y of fractions){var local=svg.createSVGPoint();"
                +"local.x=b.x+b.width*x;local.y=b.y+b.height*y;"
                +"if(p.isPointInFill(local)){var q=local.matrixTransform(m),safe=true;"
                +"for(var dx of [-1,0,1])for(var dy of [-1,0,1]){var test=svg.createSVGPoint();"
                +"test.x=q.x+dx;test.y=q.y+dy;if(winner(test)!==p)safe=false;}"
                +"if(safe)return JSON.stringify({x:q.x,y:q.y,w:innerWidth,h:innerHeight});}}return '';})()";
            scenario.onActivity(a->a.qcfViewsForTest()[side].evaluateJavascript(script,
                value->{coordinates.set(value);found.countDown();}));
            assertTrue("Actual ayah polygon coordinate callback",found.await(10,java.util.concurrent.TimeUnit.SECONDS));
            String decoded=new org.json.JSONArray("["+coordinates.get()+"]").getString(0);
            assertFalse("Requested original verse polygon must contain a real touch point",decoded.isEmpty());
            polygonPoint.set(new org.json.JSONObject(decoded));
        }
        int[] point=new int[2];
        AtomicReference<android.os.IBinder> windowToken=new AtomicReference<>();
        scenario.onActivity(a->{
            View root=a.getWindow().getDecorView();
            assertTrue("The Atlas activity must own input focus before a Mushaf touch",a.hasWindowFocus());
            assertTrue(root.hasWindowFocus());
            MapQcfThumbnails page=a.qcfViewsForTest()[side];
            assertTrue(a.bothQcfPagesFullyVisibleForTest());
            assertFalse(root.isLayoutRequested());assertFalse(page.isLayoutRequested());
            // getGlobalVisibleRect is in root coordinates; input injection needs display coordinates.
            page.getLocationOnScreen(point);
            if(polygonPoint.get()==null){point[0]+=page.getWidth()/2;point[1]+=page.getHeight()/2;}
            else try{
                org.json.JSONObject q=polygonPoint.get();
                double cssToDisplay=page.getScale();
                point[0]+=(int)Math.round(q.getDouble("x")*cssToDisplay);
                point[1]+=(int)Math.round(q.getDouble("y")*cssToDisplay);
                android.util.Log.i("HaafidhEnduranceTouch","verse="+requested+" side="+side
                    +" css="+q+" scale="+cssToDisplay+" display="+java.util.Arrays.toString(point));
            }catch(org.json.JSONException invalid){throw new AssertionError(invalid);}
            Rect visibleWindow=new Rect();root.getWindowVisibleDisplayFrame(visibleWindow);
            assertTrue("Mushaf touch must lie inside the current application window",visibleWindow.contains(point[0],point[1]));
            windowToken.set(root.getWindowToken());
            assertNotNull(windowToken.get());
        });
        long now=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,
            android.view.MotionEvent.ACTION_DOWN,point[0],point[1],0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+80,
            android.view.MotionEvent.ACTION_UP,point[0],point[1],0);
        down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try{
            if(requested!=null){
                // Endurance isolates the actual Android/WebView touch handler from
                // Google emulator Taskbar input rejection. Original contextual tests
                // below retain full system injection; this is not OS-input proof.
                scenario.onActivity(a->{
                    MapQcfThumbnails view=a.qcfViewsForTest()[side];
                    try{
                        float x=(float)(polygonPoint.get().getDouble("x")*view.getScale());
                        float y=(float)(polygonPoint.get().getDouble("y")*view.getScale());
                        down.setLocation(x,y);up.setLocation(x,y);
                        assertTrue("WebView accepts real local Android touch down",view.dispatchTouchEvent(down));
                        assertTrue("WebView accepts real local Android touch up",view.dispatchTouchEvent(up));
                    }catch(org.json.JSONException invalid){throw new AssertionError(invalid);}
                });
            }else{
                instrument.sendPointerSync(down);
                scenario.onActivity(a->{
                    assertTrue("Atlas must retain input focus through the touch",a.hasWindowFocus());
                    assertSame("Rotation must not replace the window during input",windowToken.get(),
                        a.getWindow().getDecorView().getWindowToken());
                });
                instrument.sendPointerSync(up);
            }
        }finally{down.recycle();up.recycle();}
        instrument.waitForIdleSync();
    }

    private static void saveComparisonScreenshot(Context context,String label)throws Exception{
        Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,"haafidh-mutash-"+label+".png");
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/HaafidhAudit");
        Uri uri=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(uri);
        try(OutputStream out=context.getContentResolver().openOutputStream(uri)){
            assertNotNull(out);assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));
        }
        screenshot.recycle();
    }

    /** Run on phone and BOOX profiles: page touch, retained localizations and rotation. */
    @Test public void mushafTapOpensContextualTafsirAndSwitchPreservesBothQcfPages()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=pairIntent(context,exactPairIndex(2,63,2,93));
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            try {
            scenario.onActivity(a->a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            AtomicBoolean portrait=new AtomicBoolean();
            for(int attempt=0;attempt<60;attempt++){
                scenario.onActivity(a->portrait.set(a.getResources().getConfiguration().orientation==
                    android.content.res.Configuration.ORIENTATION_PORTRAIT));
                if(portrait.get())break;
                Thread.sleep(100L);
            }
            assertTrue("Portrait layout must be exercised",portrait.get());
            waitForQcf(scenario);
            assertOriginalPageShapeAndWordMarks(scenario,samplePair());
            AtomicReference<Rect[]> pageBounds=new AtomicReference<>();
            AtomicReference<MapQcfThumbnails[]> originalPages=new AtomicReference<>();
            scenario.onActivity(a->{
                assertFalse("No buried, permanently allocated tafsir region",a.tafsirPanelVisibleForTest());
                originalPages.set(a.qcfViewsForTest());
            });
            pageBounds.set(waitForStablePageBounds(scenario,false));
            Rect target=pageBounds.get()[0];
            assertFalse("First Mushaf must be directly visible",target.isEmpty());
            tapQcfPage(scenario,0,false,samplePair().first);
            waitForSelectedTafsir(scenario,0);
            assertContextPanesVisible(scenario);
            switchTafsirAndAssertPagesRetained(scenario,1,originalPages.get());
            switchTafsirAndAssertPagesRetained(scenario,0,originalPages.get());
            // The second actual source page also switches the contextual panel.
            tapQcfPage(scenario,1,true,samplePair().second);
            waitForSelectedTafsir(scenario,1);
            assertContextPanesVisible(scenario);
            scenario.onActivity(a->assertEquals(1,a.selectedTafsirForTest()));
            AtomicBoolean loaded=new AtomicBoolean();
            for(int attempt=0;attempt<40;attempt++){
                scenario.onActivity(a->{
                    CharSequence[] texts=a.tafsirTextsForTest();
                    loaded.set(texts[0].length()>100&&texts[1].length()>100);
                });
                if(loaded.get())break;
                Thread.sleep(100L);
            }
            assertTrue("Both original offline Jalalayn texts must load",loaded.get());
            AtomicBoolean firstVisibleJalalayn=new AtomicBoolean();
            for(int attempt=0;attempt<30;attempt++){
                scenario.onActivity(a->firstVisibleJalalayn.set(a.tafsirTextVisibleForTest()));
                if(firstVisibleJalalayn.get())break;
                Thread.sleep(100L);
            }
            if(!firstVisibleJalalayn.get())
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("haafidh-tafsir-hidden-text.png");
            assertTrue("Actual al-Jalalayn commentary must be visible under the compact QCF pivot, without requiring initial scroll",
                firstVisibleJalalayn.get());
            AtomicBoolean paragraph=new AtomicBoolean();
            for(int attempt=0;attempt<30;attempt++){
                scenario.onActivity(a->paragraph.set(a.tafsirCommentaryVisibleForTest()));
                if(paragraph.get())break;
                Thread.sleep(100L);
            }
            if(!paragraph.get())saveComparisonScreenshot(context,"commentary-below-viewport");
            assertTrue("The first paragraph of the actual Jalalayn commentary, not only its bibliographic heading, must be visible without scrolling",
                paragraph.get());
            String profile=context.getResources().getConfiguration().smallestScreenWidthDp>=600?"boox":"phone";
            saveComparisonScreenshot(context,profile+"-portrait-contextual");
            scenario.recreate();waitForQcf(scenario);
            scenario.onActivity(a->{assertEquals("Selected tafsir survives recreation",1,a.selectedTafsirForTest());
                assertTrue(a.tafsirPanelVisibleForTest());});
            scenario.onActivity(a->a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            AtomicBoolean landscape=new AtomicBoolean();
            for(int attempt=0;attempt<60;attempt++){
                scenario.onActivity(a->landscape.set(a.getResources().getConfiguration().orientation==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE));
                if(landscape.get())break;
                Thread.sleep(100L);
            }
            assertTrue("Landscape layout must be exercised",landscape.get());
            waitForQcf(scenario);
            scenario.onActivity(a->{assertEquals(1,a.selectedTafsirForTest());assertTrue(a.tafsirPanelVisibleForTest());});
            assertOriginalPageShapeAndWordMarks(scenario,samplePair());
            assertContextPanesVisible(scenario);
            AtomicBoolean landscapeCommentary=new AtomicBoolean();
            for(int attempt=0;attempt<35;attempt++){
                scenario.onActivity(a->landscapeCommentary.set(
                    a.tafsirFirstCommentaryLineVisibleForTest()));
                if(landscapeCommentary.get())break;
                Thread.sleep(100L);
            }
            if(!landscapeCommentary.get())
                saveComparisonScreenshot(context,profile+"-landscape-commentary-hidden");
            assertTrue("The first actual Jalalayn commentary line must fit below the authentic QCF pivot in landscape",
                landscapeCommentary.get());
            scenario.onActivity(a->originalPages.set(a.qcfViewsForTest()));
            tapQcfPage(scenario,0,true,samplePair().first);
            waitForSelectedTafsir(scenario,0);
            switchTafsirAndAssertPagesRetained(scenario,1,originalPages.get());
            switchTafsirAndAssertPagesRetained(scenario,0,originalPages.get());
            switchTafsirAndAssertPagesRetained(scenario,1,originalPages.get());
            saveComparisonScreenshot(context,profile+"-landscape-contextual");
            } finally {
                scenario.onActivity(a->a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                AtomicBoolean restored=new AtomicBoolean();
                for(int attempt=0;attempt<60;attempt++){
                    scenario.onActivity(a->restored.set(a.getResources().getConfiguration().orientation==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT));
                    if(restored.get())break;
                    Thread.sleep(100L);
                }
                assertTrue("Restore portrait for subsequent UI tests",restored.get());
            }
        }
    }

    /**
     * The two ORIGINAL QCF pages can contain many neighbouring ayat.
     * A tap on any other polygon must not silently replace the two fixed
     * al-Jalalayn references inside a memorisation correspondence.
     */
    @Test public void onlyTwoComparedAyatMayOpenPairLockedTafsir() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        AtlasDiscriminantCases.Entry pair=samplePair();
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(
                pairIntent(context,exactPairIndex(2,63,2,93)))){
            waitForQcf(scenario);
            for(int side=0;side<2;side++){
                final int position=side,anchorAyah=side==0?63:93;
                AtomicReference<Integer> wasSelected=new AtomicReference<>();
                scenario.onActivity(a->{
                    wasSelected.set(a.selectedTafsirForTest());
                    assertTrue("Only the canonical two verses may be referenced",
                        a.tafsirPairLockedForTest());
                });
                String js="(function(){var svg=document.querySelector('.paper svg');if(!svg)return '';"
                    +"var paths=svg.querySelectorAll('path.ayahPolygon');for(var i=0;i<paths.length;i++){"
                    +"var p=paths[i],surah=Number(p.getAttribute('surah')),ayah=Number(p.getAttribute('ayah'));"
                    +"if(surah!==2||ayah==="+anchorAyah+")continue;"
                    +"var box=p.getBBox(),m=p.getScreenCTM();if(!m)continue;"
                    +"for(var x=1;x<6;x++)for(var y=1;y<6;y++){var local=svg.createSVGPoint();"
                    +"local.x=box.x+box.width*x/6;local.y=box.y+box.height*y/6;"
                    +"if(p.isPointInFill(local)){var touch=local.matrixTransform(m);"
                    +"readPage(currentSvgPage,{clientX:touch.x,clientY:touch.y});return surah+':'+ayah;}}}"
                    +"return '';})()";
                java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(1);
                AtomicReference<String> result=new AtomicReference<>("");
                scenario.onActivity(a->a.qcfViewsForTest()[position].evaluateJavascript(js,
                    value->{result.set(value);latch.countDown();}));
                assertTrue("Original QCF polygon callback must respond",
                    latch.await(8,java.util.concurrent.TimeUnit.SECONDS));
                String neighbor=new org.json.JSONArray("["+result.get()+"]").getString(0);
                assertFalse("A different ayah must share this physical Quran page",neighbor.isEmpty());
                assertNotEquals("Nonpaired ayah was not actually selected",
                    "2:"+anchorAyah,neighbor);
                Thread.sleep(250L);
                scenario.onActivity(a->{
                    assertEquals("A neighbouring ayah may not open/replace the Tafsir",
                        (int)wasSelected.get(),a.selectedTafsirForTest());
                    assertTrue("Both locked references must remain unchanged",a.tafsirPairLockedForTest());
                });
                com.quransafeguard.hifz.core.VerseRef canonical=
                    position==0?pair.first:pair.second;
                tapQcfPage(scenario,position,wasSelected.get()>=0,canonical);
                waitForSelectedTafsir(scenario,position);
                scenario.onActivity(a->{
                    assertEquals("The Tafsir must display the exact corresponding ayah",
                        canonical,a.selectedTafsirVerseForTest());
                    assertTrue("Actual underlined QCF pivot must move inside Tafsir",
                        a.tafsirPivotEmbeddedForTest());
                    assertTrue(a.tafsirPairLockedForTest());
                });
            }
            scenario.recreate();waitForQcf(scenario);
            scenario.onActivity(a->{
                assertEquals("The same canonical right Tafsir survives recreation",
                    pair.second,a.selectedTafsirVerseForTest());
                assertEquals(pair.first.toString(),a.tafsirTabLabelForTest(0));
                assertEquals(pair.second.toString(),a.tafsirTabLabelForTest(1));
                assertTrue(a.tafsirPivotEmbeddedForTest());
                assertTrue(a.tafsirPairLockedForTest());
            });
        }
    }

    private static void assertDisplayedQcfClips(ActivityScenario<AtlasDiscriminantActivity> scenario,
            AtlasDiscriminantCases.Entry pair)throws Exception{
        java.util.concurrent.CountDownLatch clipsChecked=new java.util.concurrent.CountDownLatch(1);
        AtomicReference<String> clipResult=new AtomicReference<>();
        scenario.onActivity(activity->{
            try {
                java.lang.reflect.Field stripsField=AtlasDiscriminantActivity.class.getDeclaredField("strips");
                stripsField.setAccessible(true);
                ((QcfAtlasVerseStrips)stripsField.get(activity)).evaluateJavascript(
                    "(function(){var ids=[],prefixes=['atlas-word-window-"+
                    pair.first.getSurah()+"-"+pair.first.getAyah()+"','atlas-word-window-"+
                    pair.second.getSurah()+"-"+pair.second.getAyah()+"'];var expected=["+
                    AtlasDiscriminantOmissions.actualDifference(pair,true).length+","+AtlasDiscriminantOmissions.actualDifference(pair,false).length+"];"
                    +"for(var i=0;i<2;i++){var s=document.querySelector('#crop'+i+' svg');if(!s)return false;"
                    +"var clips=s.querySelectorAll('clipPath');if(!clips.length)return false;"
                    +"var view=s.viewBox.baseVal,rect=s.getBoundingClientRect();"
                    +"if(!s.hasAttribute('data-qcf-shared-scale')||view.width<=0||view.height<=0)return false;"
                    +"var glyphScale=rect.width/view.width;"
                    +"if(i===0)window.__qcfCompareScale=glyphScale;"
                    +"else if(Math.abs(glyphScale-window.__qcfCompareScale)>0.015)return false;"
                    +"for(var j=0;j<clips.length;j++){var clip=clips[j];"
                    +"if(!clip.id||clip.id.indexOf(prefixes[i])!==0||ids.indexOf(clip.id)>=0)return false;"
                    +"if(document.getElementById(clip.id)!==clip)return false;ids.push(clip.id);}"
                    +"var groups=s.querySelectorAll('g[clip-path]');if(!groups.length)return false;"
                    +"for(var k=0;k<groups.length;k++){var ref=groups[k].getAttribute('clip-path');"
                    +"if(!ref||ref.indexOf('url(#')!==0)return false;"
                    +"var target=document.getElementById(ref.slice(5,-1));if(!target||!s.contains(target))return false;}"
                    +"if(s.querySelectorAll('[id^=exact-word-differences] line').length!==expected[i])return false;}return true;})()",
                    value->{clipResult.set(value);clipsChecked.countDown();});
            }catch(ReflectiveOperationException invalid){throw new AssertionError(invalid);}
        });
        assertTrue("Paired QCF clip DOM callback missing",clipsChecked.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("Each inline QCF excerpt must resolve its own exact-word clip", "true",clipResult.get());
    }

    private static void assertSelectedVerse(ActivityScenario<AtlasDiscriminantActivity> scenario,
            com.quransafeguard.hifz.core.VerseRef expected)throws Exception{
        AtomicReference<com.quransafeguard.hifz.core.VerseRef> actual=new AtomicReference<>();
        scenario.onActivity(a->actual.set(a.selectedTafsirVerseForTest()));
        if(!expected.equals(actual.get()))
            HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("endurance-exact-verse-touch-failure.png");
        assertEquals("Real injected touch must select its requested original QCF polygon",expected,actual.get());
    }

    private static void awaitBothTafsirs(ActivityScenario<AtlasDiscriminantActivity> scenario)throws Exception{
        AtomicBoolean loaded=new AtomicBoolean();
        for(int poll=0;poll<100;poll++){
            scenario.onActivity(a->{
                CharSequence[] texts=a.tafsirTextsForTest();
                loaded.set(a.selectedTafsirVerseForTest()!=null&&texts[0].length()>100&&texts[1].length()>100);
            });
            if(loaded.get())return;
            Thread.sleep(100L);
        }
        fail("Both real offline Tafsir bodies must load during endurance");
    }

    /** Opt-in endurance. Reuses the strict real-window, DOM and original-glyph gates.
     * A tafsir closes by leaving Atlas: there is no independent close-panel action.
     * Detaching/destroying a WebView incorrectly makes the parent/lifecycle checks fail.
     */
    @Test public void enduranceTafsirRotationsAndQcfDisposal()throws Exception{
        org.junit.Assume.assumeTrue("true".equals(
            InstrumentationRegistry.getArguments().getString("atlasEndurance")));
        Context context=ApplicationProvider.getApplicationContext();new HifzPrefs(context);
        SharedPreferences prefs=context.getSharedPreferences("quran_hifz_preview_v1",0);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        org.json.JSONObject fullBefore=RevisionAtlasInstrumentedTest.snapshot(context);
        android.os.Bundle enduranceArgs=InstrumentationRegistry.getArguments();
        int total=Integer.parseInt(enduranceArgs.getString("atlasTafsirCycles","200"));
        int requestedRotations=Math.min(100,total);
        int startIndex=Integer.parseInt(enduranceArgs.getString("atlasTafsirStartIndex","0"));
        assertTrue(total>=1&&total<=200);assertTrue(startIndex>=0&&startIndex<AtlasDiscriminantCases.PILOT.size());
        int rotations=0;
        for(int cycle=0;cycle<total;cycle++){
            int index=(startIndex+cycle)%AtlasDiscriminantCases.PILOT.size();
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(index);
            AtomicReference<AtlasDiscriminantActivity> host=new AtomicReference<>();
            AtomicReference<MapQcfThumbnails[]> pages=new AtomicReference<>();
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(pairIntent(context,index))){
                waitForQcf(scenario);
                assertOriginalPageShapeAndWordMarks(scenario,pair);
                assertDisplayedQcfClips(scenario,pair);
                assertExactQcfWords(context,pair.first,pair.firstWordIndices);
                assertExactQcfWords(context,pair.second,pair.secondWordIndices);
                tapQcfPage(scenario,0,false,pair.first);waitForSelectedTafsir(scenario,0);
                assertSelectedVerse(scenario,pair.first);
                tapQcfPage(scenario,1,true,pair.second);waitForSelectedTafsir(scenario,1);
                assertSelectedVerse(scenario,pair.second);
                assertContextPanesVisible(scenario);awaitBothTafsirs(scenario);
                if(cycle<requestedRotations){
                    AtomicReference<MapQcfThumbnails[]> old=new AtomicReference<>();
                    int[] desired=new int[1];
                    scenario.onActivity(a->{
                        old.set(a.qcfViewsForTest());
                        desired[0]=a.getResources().getConfiguration().orientation==
                            android.content.res.Configuration.ORIENTATION_PORTRAIT?
                            android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                            android.content.res.Configuration.ORIENTATION_PORTRAIT;
                        a.setRequestedOrientation(desired[0]==android.content.res.Configuration.ORIENTATION_PORTRAIT?
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    });
                    AtomicBoolean rotated=new AtomicBoolean();
                    for(int poll=0;poll<100;poll++){
                        scenario.onActivity(a->rotated.set(a.getResources().getConfiguration().orientation==desired[0]
                            &&a.qcfViewsForTest()[0]!=old.get()[0]));
                        if(rotated.get())break;
                        Thread.sleep(100L);
                    }
                    assertTrue("Rotation must replace actual QCF views",rotated.get());
                    waitForQcf(scenario);assertContextPanesVisible(scenario);awaitBothTafsirs(scenario);
                    assertDisplayedQcfClips(scenario,pair);
                    assertOriginalPageShapeAndWordMarks(scenario,pair);
                    scenario.onActivity(a->{
                        assertEquals(1,a.selectedTafsirForTest());
                        for(MapQcfThumbnails view:old.get())assertNull("Old WebView must detach on rotation",view.getParent());
                    });
                    rotations++;
                }
                scenario.onActivity(a->{host.set(a);pages.set(a.qcfViewsForTest());});
            }
            assertTrue("Closing Atlas must destroy its host",host.get().isDestroyed());
            for(MapQcfThumbnails view:pages.get())assertNull("Closed WebView must detach",view.getParent());
            assertEquals("Tafsir/rotation must preserve all Hifz values",before,prefs.getAll());
            assertTrue("Every backup store must remain unchanged during Tafsir/rotation",
                RevisionAtlasInstrumentedTest.sameJson(fullBefore,RevisionAtlasInstrumentedTest.snapshot(context)));
            EnduranceAuditMetrics.sample(total==200?"tafsir-close":"tafsir-smoke",cycle+1);
        }
        assertEquals("Actual orientation changes",requestedRotations,rotations);
        android.util.Log.i("HaafidhEndurance","SUMMARY tafsir_open_close="+total+" rotations="+rotations+" qcf_side_selections="+(total*2));
    }

    /** A delayed new render must never expose the previous pair's excerpt. */
    @Test public void changingCaseImmediatelyClearsOldQcfExcerpts()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();
        int page=GeometryRepository.get(context).pageForVerse(samplePair().first);
        java.util.List<Integer> indices=AtlasDiscriminantCases.indicesForPage(page);
        assertTrue("Fixture page must have multiple verified relations",indices.size()>1);
        Intent intent=pairIntent(context,indices.get(0)).putExtra(AtlasDiscriminantActivity.EXTRA_PAGE,page);
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            waitForQcf(scenario);
            scenario.onActivity(a->{
                assertTrue("Initial source excerpts must be visible",a.qcfStripsVisibleForTest());
                int previousGeneration=a.qcfStripsGenerationForTest();
                View next=findByDescription(a.getWindow().getDecorView(),"Comparaison suivante de cette page");
                assertNotNull(next);assertTrue(next.performClick());
                assertTrue("New labels invalidate all old excerpt callbacks",
                    a.qcfStripsGenerationForTest()>previousGeneration);
                assertFalse("Previous glyphs must disappear before asynchronous page loading",
                    a.qcfStripsVisibleForTest());
                assertFalse("Old excerpt readiness must not survive a pair change",a.qcfStripsReadyForTest());
            });
            waitForQcf(scenario);
            scenario.onActivity(a->assertTrue("Only the new completed excerpts may reappear",a.qcfStripsVisibleForTest()));
        }
    }

    /** Exact one-based markers must all exist in the shipped word geometry, never estimated. */
    private static void assertExactQcfWords(Context context,
        com.quransafeguard.hifz.core.VerseRef verse,int[] marked) throws Exception {
        GeometryRepository g=GeometryRepository.get(context);
        int page=g.pageForVerse(verse);
        assertEquals("Do not crop a partial cross-page ayah: "+verse,
            page,MawdudiMapModel.lastPageForVerse(verse,g));
        java.util.List<WordGeometryRepository.WordBox> words=
            WordGeometryRepository.shared(context).wordsForPage(page);
        for(int requested:marked) {
            boolean found=false;
            for(WordGeometryRepository.WordBox box:words)
                if(box.surah==verse.getSurah() && box.ayah==verse.getAyah()
                    && box.word==requested){found=true;break;}
            assertTrue("No exact QCF word "+verse+":"+requested+" on page "+page,found);
        }
        String svg=MapQcfThumbnails.readSvg(context,page);
        String excerpt=QcfAtlasVerseStrips.cropSvg(svg,words,verse,marked,
            QcfAtlasVerseStrips.physicalLines(g,page),QcfAtlasVerseStrips.safeLineCuts(context,page,svg));
        assertFalse("Original QCF SVG excerpt cannot be cropped for "+verse,excerpt.isEmpty());
        assertTrue("Original Quran word glyph paths were lost for "+verse,
            excerpt.contains("ayahPolygon"));
        int renderedLines=excerpt.split("<line x1=", -1).length-1;
        assertEquals("Wrong number of QCF discriminant underlines for "+verse,
            marked.length,renderedLines);
    }
    /** Finds the actual Atlas navigation button by its stable accessible label. */
    private static View findByDescriptionPrefix(View root,String prefix){
        CharSequence label=root.getContentDescription();
        if(label!=null&&label.toString().startsWith(prefix))return root;
        if(root instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)root;
            for(int i=0;i<parent.getChildCount();i++){
                View found=findByDescriptionPrefix(parent.getChildAt(i),prefix);
                if(found!=null)return found;
            }
        }
        return null;
    }

    private static View findByDescription(View root,String description) {
        CharSequence label=root.getContentDescription();
        if(label!=null && description.contentEquals(label))return root;
        if(root instanceof ViewGroup) {
            ViewGroup parent=(ViewGroup)root;
            for(int i=0;i<parent.getChildCount();i++) {
                View found=findByDescription(parent.getChildAt(i),description);
                if(found!=null)return found;
            }
        }
        return null;
    }

    /**
     * Isolates page 148 / 285 without previous QCF DOMs; distinguishes an intrinsic
     * SVG/geometry failure from a cumulative renderer-memory navigation failure.
     */
    @Test public void secondPilotRendersAloneWithoutPriorHeavyPages() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=pairIntent(context,exactPairIndex(6,151,17,31));
        // Validate the exact underlying page bytes and crop independently of Chromium.
        for(AtlasDiscriminantCases.Entry pair:java.util.Collections.singletonList(
            AtlasDiscriminantCases.PILOT.get(exactPairIndex(6,151,17,31)))) {
            assertExactQcfWords(context,pair.first,pair.firstWordIndices);
            assertExactQcfWords(context,pair.second,pair.secondWordIndices);
        }
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            AtomicBoolean ready=new AtomicBoolean(false);
            for(int i=0;i<40;i++){
                Thread.sleep(500L);
                scenario.onActivity(activity->{
                    assertEquals("Page 148 / 285 direct load failed : "+
                        activity.qcfFailureDetailForTest(),0,activity.qcfPagesFailedForTest());
                    ready.set(activity.qcfPagesReadyForTest()&&activity.qcfStripsReadyForTest());
                });
                if(ready.get())break;
            }
            assertTrue("Direct 6:151 / 17:31 genuine QCF pages did not render",ready.get());
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(exactPairIndex(6,151,17,31));
            assertExactQcfWords(context,pair.first,pair.firstWordIndices);
            assertExactQcfWords(context,pair.second,pair.secondWordIndices);
        }
    }

    /** True pixel gate: original QCF glyphs must paint, not just create SVG DOM. */
    private static boolean inkVisible(Bitmap shot,Rect visible) {
        if(shot==null||visible==null)return false;
        int left=Math.max(0,visible.left),top=Math.max(0,visible.top);
        int right=Math.min(shot.getWidth(),visible.right),bottom=Math.min(shot.getHeight(),visible.bottom);
        if(right-left<=40||bottom-top<=20)return false;
        int dark=0,samples=0;
        for(int y=top+8;y<bottom-8;y+=3)for(int x=left+8;x<right-8;x+=3) {
            int px=shot.getPixel(x,y);
            if(Color.red(px)<125&&Color.green(px)<125&&Color.blue(px)<125)dark++;
            samples++;
        }
        // Verified real phone QCF (2:63, 1080x1920) has 128 dark pixels
        // across 14,388 samples (0.89% ink) in its complete equal-width viewport.
        // The former 1.2% threshold rejected authentic visible source Arabic.
        // Keep strict NO-BLANK enforcement: at least 60 sampled ink pixels
        // and >=0.6% ink inside the actual QCF excerpt rectangle.
        return samples>0&&dark>=60&&dark*1000 >= samples*6;
    }

    private static void assertInkIsVisible(Bitmap shot,Rect visible,String description) {
        assertTrue(description+" rendered blank / clipped / missing its genuine QCF glyph ink",
            inkVisible(shot,visible));
    }

    @Test public void pilotOpensAuthenticPairedMushafAndKeepsProgressUntouched() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        Intent intent=pairIntent(context,exactPairIndex(2,63,2,93));
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                View root=activity.findViewById(android.R.id.content);
                assertEquals(2,countViews(root,MapQcfThumbnails.class));
                assertEquals(1,countViews(root,QcfAtlasVerseStrips.class));
                assertEquals("2:63 must be at page mapped by real geometry",
                    GeometryRepository.get(activity).pageForVerse(
                        new com.quransafeguard.hifz.core.VerseRef(2,63)),
                    activity.getCurrentLeftPageForTest());
            });
            AtomicBoolean ready=new AtomicBoolean(false);
            AtomicReference<Rect[]> bounds=new AtomicReference<>();
            // BOOX-safe staged decoding is not instantaneous: poll boundedly off the UI thread.
            for(int i=0;i<40;i++){
                Thread.sleep(500L);
                scenario.onActivity(activity->{
                    ready.set(activity.qcfPagesReadyForTest()&&activity.qcfStripsReadyForTest());
                    bounds.set(activity.qcfPageBoundsForTest());
                    assertEquals("An exact QCF SVG reported invalid / unrenderable",
                        0,activity.qcfPagesFailedForTest());
                });
                if(ready.get())break;
            }
            assertTrue("Both actual QCF pages AND their comparison strips must visibly render",ready.get());
            // A native WebView paint is asynchronous after DOM's second animation frame.
            Thread.sleep(1200L);
            // Verify real SVG crop and exact word underline, not simply Android view creation.
            GeometryRepository g=GeometryRepository.get(context);
            com.quransafeguard.hifz.core.VerseRef verse=
                new com.quransafeguard.hifz.core.VerseRef(2,63);
            int page=g.pageForVerse(verse);
            String original=MapQcfThumbnails.readSvg(context,page);
            String cropped=QcfAtlasVerseStrips.cropSvg(original,
                WordGeometryRepository.shared(context).wordsForPage(page),verse,samplePair().firstWordIndices,
                QcfAtlasVerseStrips.physicalLines(g,page),QcfAtlasVerseStrips.safeLineCuts(context,page,original));
            assertFalse("The exact Quran crop must be available for 2:63",cropped.isEmpty());
            assertTrue("Exact original Quran paths must be preserved",cropped.contains("ayahPolygon"));
            assertTrue("The audited differing words must be underlined",cropped.contains("exact-word-differences"));
            Rect[] pageBounds=bounds.get();
            assertNotNull(pageBounds);
            AtomicReference<Rect[]> croppedBounds=new AtomicReference<>();
            scenario.onActivity(activity->croppedBounds.set(activity.qcfCropBoundsForTest()));
            Rect[] crop=croppedBounds.get();
            assertNotNull("Both genuine source QCF excerpts need a measured viewport",crop);
            assertEquals(2,crop.length);
            android.util.Log.i("HaafidhAtlasPixelBounds","fullPages="+java.util.Arrays.toString(pageBounds)
                +" excerptHalves="+java.util.Arrays.toString(crop)
);
            // Chromium may report valid SVG geometry one compositing frame
            // before the underlying WebView's pixels appear. Sample *real*
            // screenshots until BOTH exact Quran excerpts are visible together;
            // do not accept a DOM callback or a white placeholder as success.
            Bitmap shot=null;
            for(int paintAttempt=0;paintAttempt<24;paintAttempt++){
                Bitmap frame=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                if(frame!=null&&inkVisible(frame,pageBounds[0])&&inkVisible(frame,pageBounds[1])
                        &&inkVisible(frame,crop[0])&&inkVisible(frame,crop[1])){
                    shot=frame;break;
                }
                if(frame!=null)frame.recycle();
                Thread.sleep(250L);
            }
            if(shot==null)
                saveComparisonScreenshot(context,"qcf-pixel-gate-diagnostic");
            assertNotNull("Both authentic QCF word excerpts must be visibly painted in the SAME actual screenshot",shot);
            assertInkIsVisible(shot,pageBounds[0],"Left KFQC page");
            assertInkIsVisible(shot,pageBounds[1],"Right KFQC page");
            assertInkIsVisible(shot,crop[0],"Left exact QCF comparison strip");
            assertInkIsVisible(shot,crop[1],"Right exact QCF comparison strip");
            // MediaStore retains the audit photo if Gradle uninstalls the test APK.
            // No Android storage permission; only a new image authored by this app.
            ContentValues entry=new ContentValues();
            entry.put(MediaStore.Images.Media.DISPLAY_NAME,"haafidh-atlas-discriminant-portrait.png");
            entry.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
            entry.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/HaafidhAudit");
            Uri target=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,entry);
            assertNotNull("Screenshot must have a MediaStore destination",target);
            try(OutputStream stream=context.getContentResolver().openOutputStream(target)){
                assertNotNull("Screenshot output unavailable",stream);
                assertTrue(shot.compress(Bitmap.CompressFormat.PNG,100,stream));
                stream.flush();
            }
            shot.recycle();
        }
        assertEquals("Atlas comparison must never mutate the main Hifz state",before,prefs.getAll());
    }
    /** Genuine Lecture -> Atlas -> same Lecture Activity; no extra Hifz credit. */
    @Test public void lectureSelectionOpensComparisonAndReturnsWithoutMovingCursor() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences hifz=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        // A fresh reader initializes schema-6 defaults; compare only changes
        // made while the comparison is open, not first-install initialization.
        AtomicReference<Map<String,?>> before=new AtomicReference<>();
        int sourcePage=GeometryRepository.get(context).pageForVerse(samplePair().first);
        Intent start=new Intent(context,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,sourcePage)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,"2:63");
        Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor=instrument.addMonitor(
            AtlasDiscriminantActivity.class.getName(),null,false);
        try {
            try(ActivityScenario<StudyReaderActivity> scene=ActivityScenario.launch(start)) {
                AtomicBoolean ready=new AtomicBoolean(false);
                AtomicReference<StudyReaderActivity> original=new AtomicReference<>();
                for(int i=0;i<40;i++) {
                    Thread.sleep(250L);
                    scene.onActivity(activity->{
                        original.set(activity);
                        ready.set(activity.selectedDiscriminantPageForTest()==sourcePage
                            &&activity.selectedDiscriminantVerseForTest()!=null
                            &&"2:63".equals(activity.selectedDiscriminantVerseForTest().toString())
                            &&activity.discriminantAvailableForTest());
                    });
                    if(ready.get())break;
                }
                assertTrue("Documented 2:63 must expose its contextual comparison",
                    ready.get());
                before.set(new HashMap<>(hifz.getAll()));
                scene.onActivity(activity->{
                    assertTrue(activity.discriminantAvailableForTest());
                    activity.onAtlasDiscriminantTap(new com.quransafeguard.hifz.core.VerseRef(2,63));
                });
                Activity opened=instrument.waitForMonitorWithTimeout(monitor,20000);
                assertNotNull("Lecture must launch the comparison for its selected verse",opened);
                assertTrue(opened instanceof AtlasDiscriminantActivity);
                assertEquals(sourcePage,((AtlasDiscriminantActivity)opened).getCurrentLeftPageForTest());
                instrument.runOnMainSync(opened::finish);
                instrument.waitForIdleSync();
                scene.onActivity(activity->{
                    assertSame("The original reader Activity must survive, not a new session",
                        original.get(),activity);
                    assertEquals(sourcePage,activity.selectedDiscriminantPageForTest());
                    assertEquals("2:63",activity.selectedDiscriminantVerseForTest().toString());
                    assertFalse(activity.freeActiveMaskForTest());
                    assertTrue(activity.discriminantAvailableForTest());
                });
                assertEquals("Atlas visit must not modify initialized Hifz state",
                    before.get(),hifz.getAll());
            }
        } finally {
            instrument.removeMonitor(monitor);
        }
        assertNotNull("Reader baseline must be captured before opening Atlas",before.get());
        assertEquals("Comparison must not change any Hifz progress",before.get(),hifz.getAll());
    }

    /**
     * Every curated pair must survive the same genuine asynchronous QCF decoding.
     * The first pilot's screenshot test remains the stronger pixel/underline check;
     * this guards against later pairs hiding missing pages or invalid verse geometry.
     */
    @Test public void everyPilotPairLoadsBothAuthenticQcfPagesAndStrips() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        // Shards partition exact indices; default invocation still checks the entire catalogue.
        android.os.Bundle args=InstrumentationRegistry.getArguments();
        int shard=Integer.parseInt(args.getString("catalogueShard","0"));
        int shards=Integer.parseInt(args.getString("catalogueShards","1"));
        assertTrue(shards>=1&&shards<=16&&shard>=0&&shard<shards);
        int checked=0;
        for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++){
            if(i%shards!=shard)continue;
            checked++;
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(i);
            Intent intent=pairIntent(context,i);
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
                scenario.onActivity(activity->{
                    View firstLink=findByDescriptionPrefix(activity.getWindow().getDecorView(),
                        "Ouvrir le premier verset en Lecture : ");
                    View secondLink=findByDescriptionPrefix(activity.getWindow().getDecorView(),
                        "Ouvrir le second verset en Lecture : ");
                    assertNotNull("First compact Quran header missing",firstLink);
                    assertNotNull("Second compact Quran header missing",secondLink);
                    assertTrue(firstLink.isClickable()&&secondLink.isClickable());
                    assertTrue(((android.widget.TextView)firstLink).getText().toString().contains(pair.first.toString()));
                    assertTrue(((android.widget.TextView)secondLink).getText().toString().contains(pair.second.toString()));
                    assertFalse("No redundant giant Lecture line",
                        ((android.widget.TextView)firstLink).getText().toString().contains("Lire "));
                    assertTrue("Header must retain a genuine 48dp touch target",
                        firstLink.getLayoutParams().height>=Ui.dp(activity,48));
                    assertNull("Standalone Atlas next-couple arrow must be absent",
                        findByDescription(activity.getWindow().getDecorView(),
                            "Passage suivant du corpus"));
                });
                scenario.onActivity(activity->{
                    GeometryRepository geometry=GeometryRepository.get(activity);
                    assertEquals("Wrong left QCF page for "+pair.first,
                        geometry.pageForVerse(pair.first),activity.getCurrentLeftPageForTest());
                    assertEquals("Exactly two QCF page WebViews required for "+pair.first,
                        2,countViews(activity.findViewById(android.R.id.content),MapQcfThumbnails.class));
                });
                AtomicBoolean ready=new AtomicBoolean(false);
                for(int attempt=0;attempt<40;attempt++){
                    Thread.sleep(500L);
                    scenario.onActivity(activity->{
                        assertEquals("Invalid original QCF page or verse crop in "+pair.first+
                            " / "+pair.second+" : "+activity.qcfFailureDetailForTest(),
                            0,activity.qcfPagesFailedForTest());
                        ready.set(activity.qcfPagesReadyForTest()&&activity.qcfStripsReadyForTest());
                    });
                    if(ready.get())break;
                }
                assertTrue("Both original QCF pages and excerpts must load: "+
                    pair.first+" / "+pair.second,ready.get());
                assertDisplayedQcfClips(scenario,pair);
                assertOriginalPageShapeAndWordMarks(scenario,pair);
                assertExactQcfWords(context,pair.first,pair.firstWordIndices);
                assertExactQcfWords(context,pair.second,pair.secondWordIndices);
                if(pair.auditId==120||pair.auditId==199){
                    AtomicBoolean readyExtra=new AtomicBoolean(false);
                    for(int attempt=0;attempt<60;attempt++){
                        scenario.onActivity(a->readyExtra.set(a.qcfSecondaryReadyForTest()));
                        if(readyExtra.get())break;
                        Thread.sleep(100L);
                    }
                    assertTrue("Two genuine QCF pivot strips must render for case "+pair.auditId,
                        readyExtra.get());
                    String device=context.getResources().getConfiguration().smallestScreenWidthDp>=600?
                        "boox":"phone";
                    saveComparisonScreenshot(context,"case-"+pair.auditId+"-two-pivots-"+device);
                }
                if(pair.auditId==49||pair.auditId==292||pair.auditId==293){
                    AtomicReference<String> surface=new AtomicReference<>();
                    scenario.onActivity(a->surface.set(a.getResources().getConfiguration().smallestScreenWidthDp>=600?"boox":"phone"));
                    saveComparisonScreenshot(context,"case-"+pair.auditId+"-"+surface.get());
                }
            }
        }
        assertEquals("Every index in this shard must render both original pages/crops",
            (AtlasDiscriminantCases.PILOT.size()+shards-1-shard)/shards,checked);
        android.util.Log.i("HaafidhCatalogueAudit","shard="+shard+"/"+shards+" checked="+checked);
        assertEquals("Browsing Atlas must not alter Hifz progress",before,prefs.getAll());
    }


    /** Two compact Quran headers preserve independent exact-verse navigation,
     * then return to the SAME comparison without touching Hifz. */
    @Test public void bothVisibleLectureLinksOpenExactVersesAndReturn() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=pairIntent(context,exactPairIndex(2,63,2,93));
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(
            StudyReaderActivity.class.getName(),null,false);
        try {
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
                for(int side=0;side<2;side++){
                    // Instrumentation.waitForMonitorWithTimeout consumes the
                    // matching monitor after one Activity launch. Re-register
                    // for the second independent Lecture destination.
                    if(side>0){
                        instrumentation.removeMonitor(monitor);
                        monitor=instrumentation.addMonitor(
                            StudyReaderActivity.class.getName(),null,false);
                    }
                    final boolean first=side==0;
                    scenario.onActivity(activity->{
                        View link=findByDescriptionPrefix(activity.getWindow().getDecorView(),
                            first?"Ouvrir le premier verset en Lecture : ":
                                "Ouvrir le second verset en Lecture : ");
                        assertNotNull("Contextual Lecture destination missing",link);
                        assertTrue("Compact Quran header must open the correct verse",link.performClick());
                    });
                    Activity opened=instrumentation.waitForMonitorWithTimeout(monitor,15000);
                    assertNotNull("Tapping explicit Lecture destination must open reader",opened);
                    assertEquals(first?samplePair().first.toString():samplePair().second.toString(),opened.getIntent()
                        .getStringExtra(StudyReaderActivity.EXTRA_JUMP_VERSE));
                    assertEquals(GeometryRepository.get(context).pageForVerse(first?samplePair().first:samplePair().second),opened.getIntent()
                        .getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,-1));
                    instrumentation.runOnMainSync(opened::finish);
                    instrumentation.waitForIdleSync();
                    scenario.onActivity(activity->{
                        assertEquals(GeometryRepository.get(context).pageForVerse(samplePair().first),activity.getCurrentLeftPageForTest());
                        assertTrue("Back must restore the same comparison",
                            activity.qcfPagesFailedForTest()==0);
                    });
                }
            }
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    /**
     * Reader -> exact Atlas pair -> Back must preserve the SAME reader instance,
     * the same selected QCF verse and current page, and all Hifz progress.
     */
    @Test public void lectureContextualLinkRoundTripPreservesQcfVerseAndHifzState() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Application app=(Application)context;
        SharedPreferences hifz=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        SharedPreferences reading=context.getSharedPreferences("hifz_study",Context.MODE_PRIVATE);
        AtomicReference<StudyReaderActivity> readerRef=new AtomicReference<>();
        AtomicReference<AtlasDiscriminantActivity> comparisonRef=new AtomicReference<>();
        AtomicBoolean returned=new AtomicBoolean(false);
        Application.ActivityLifecycleCallbacks callbacks=new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityCreated(Activity activity,Bundle state){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityResumed(Activity activity){
                if(activity instanceof AtlasDiscriminantActivity)
                    comparisonRef.set((AtlasDiscriminantActivity)activity);
                if(activity==readerRef.get() && comparisonRef.get()!=null)returned.set(true);
            }
            @Override public void onActivityPaused(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity,Bundle state){}
            @Override public void onActivityDestroyed(Activity activity){}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        try {
            int qcfPage=GeometryRepository.get(context).pageForVerse(
                new com.quransafeguard.hifz.core.VerseRef(2,63));
            Intent intent=new Intent(context,StudyReaderActivity.class)
                .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,qcfPage)
                .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,"2:63");
            try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)){
                AtomicBoolean ready=new AtomicBoolean(false);
                for(int attempt=0;attempt<40;attempt++){
                    Thread.sleep(250L);
                    scenario.onActivity(activity->{
                        readerRef.set(activity);
                        com.quransafeguard.hifz.core.VerseRef verse=
                            activity.selectedDiscriminantVerseForTest();
                        ready.set(activity.discriminantAvailableForTest() &&
                            verse!=null && verse.getSurah()==2 && verse.getAyah()==63);
                    });
                    if(ready.get())break;
                }
                assertTrue("A documented verse in Lecture must expose its Atlas relation",ready.get());
                // Compare only the detour; reader bootstrapping can initialize defaults.
                Map<String,?> beforeHifz=new HashMap<>(hifz.getAll());
                Map<String,?> beforeReading=new HashMap<>(reading.getAll());
                scenario.onActivity(activity->{
                    assertEquals("Original Lecture page",qcfPage,activity.selectedDiscriminantPageForTest());
                    assertFalse("No comparison while masked",activity.freeActiveMaskForTest());
                    assertTrue("Exact relation must be available",activity.discriminantAvailableForTest());
                    activity.onAtlasDiscriminantTap(new com.quransafeguard.hifz.core.VerseRef(2,63));
                });
                for(int attempt=0;attempt<40 && comparisonRef.get()==null;attempt++)
                    Thread.sleep(250L);
                assertNotNull("Comparison was not opened from the selected verse",comparisonRef.get());
                InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    ()->comparisonRef.get().finish());
                for(int attempt=0;attempt<40 && !returned.get();attempt++)
                    Thread.sleep(250L);
                assertTrue("Back must resume the original Lecture instance",returned.get());
                scenario.onActivity(activity->{
                    assertSame("Must not recreate the Reader on return",readerRef.get(),activity);
                    assertEquals("No page jump after comparison",
                        qcfPage,activity.selectedDiscriminantPageForTest());
                    com.quransafeguard.hifz.core.VerseRef selected=
                        activity.selectedDiscriminantVerseForTest();
                    assertNotNull("Selected verse lost on return",selected);
                    assertEquals(2,selected.getSurah());
                    assertEquals(63,selected.getAyah());
                    assertFalse("Mask must remain unchanged",activity.freeActiveMaskForTest());
                    assertTrue(activity.discriminantAvailableForTest());

                    // Changing the selected verse preserves its Tafsir selection;
                    // verified 2:63 remains on the same QCF page. The margin icon
                    // must stay available without changing the selected Tafsir verse.
                    activity.onVerseTap(new com.quransafeguard.hifz.core.VerseRef(2,64));
                    assertEquals("Selecting a different verse must not change the Reader page",
                        qcfPage,activity.selectedDiscriminantPageForTest());
                    assertEquals("Selecting a nearby verse must preserve its Tafsir selection",
                        new com.quransafeguard.hifz.core.VerseRef(2,64),
                        activity.selectedDiscriminantVerseForTest());
                    assertTrue("The page still contains verified 2:63; keep its link",
                        activity.discriminantAvailableForTest());
                });
                assertEquals("Atlas round-trip must not modify Lecture cursor prefs",
                    beforeReading,reading.getAll());
                assertEquals("Atlas round-trip must not create Hifz progress",
                    beforeHifz,hifz.getAll());
            }
        } finally {
            app.unregisterActivityLifecycleCallbacks(callbacks);
        }
    }

    /** The margin action remains available on verified pages without selecting a verse. */
    @Test public void verifiedPageShowsChainLinkBeforeAnyVerseSelection() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        int page=GeometryRepository.get(context).pageForVerse(
            new com.quransafeguard.hifz.core.VerseRef(2,63));
        Intent start=new Intent(context,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page);
        try(ActivityScenario<StudyReaderActivity> scene=ActivityScenario.launch(start)) {
            AtomicBoolean ready=new AtomicBoolean(false);
            for(int i=0;i<40;i++){
                Thread.sleep(250L);
                scene.onActivity(activity->{
                    ready.set(activity.selectedDiscriminantPageForTest()==page
                        &&activity.discriminantAvailableForTest());
                });
                if(ready.get())break;
            }
            assertTrue("Verified 2:63 must expose the chain-link icon without a tapped verse",ready.get());
            scene.onActivity(activity->{
                assertNull("Contextual icon must not alter the Tafsir verse selection",
                    activity.selectedDiscriminantVerseForTest());
                assertFalse("Regular Lecture must remain visible, never free-mask mode",
                    activity.freeActiveMaskForTest());
            });
        }
    }

}
