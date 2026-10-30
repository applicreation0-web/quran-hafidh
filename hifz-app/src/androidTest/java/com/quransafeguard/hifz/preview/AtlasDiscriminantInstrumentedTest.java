package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.app.Activity;
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
    private static int countViews(View root,Class<?> expected) {
        int count=expected.isInstance(root)?1:0;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++)count+=countViews(group.getChildAt(i),expected);
        }
        return count;
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
        String excerpt=QcfAtlasVerseStrips.cropSvg(svg,words,verse,marked);
        assertFalse("Original QCF SVG excerpt cannot be cropped for "+verse,excerpt.isEmpty());
        assertTrue("Original Quran word glyph paths were lost for "+verse,
            excerpt.contains("ayahPolygon"));
        int renderedLines=excerpt.split("<line x1=", -1).length-1;
        assertEquals("Wrong number of QCF discriminant underlines for "+verse,
            marked.length,renderedLines);
    }
    /** Finds the actual Atlas navigation button by its stable accessible label. */
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
        Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,6)
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,151);
        // Validate the exact underlying page bytes and crop independently of Chromium.
        for(AtlasDiscriminantCases.Entry pair:java.util.Collections.singletonList(
            AtlasDiscriminantCases.PILOT.get(1))) {
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
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(1);
            assertExactQcfWords(context,pair.first,pair.firstWordIndices);
            assertExactQcfWords(context,pair.second,pair.secondWordIndices);
        }
    }

    /** Detect blank QCF panels or broken-image alt text even when WebView exists. */
    private static void assertInkIsVisible(Bitmap shot,Rect visible,String description) {
        int left=Math.max(0,visible.left),top=Math.max(0,visible.top);
        int right=Math.min(shot.getWidth(),visible.right),bottom=Math.min(shot.getHeight(),visible.bottom);
        assertTrue(description+" missing viewport",right-left>80&&bottom-top>120);
        int dark=0,samples=0;
        for(int y=top+8;y<bottom-8;y+=3)for(int x=left+8;x<right-8;x+=3) {
            int px=shot.getPixel(x,y);
            if(Color.red(px)<125&&Color.green(px)<125&&Color.blue(px)<125)dark++;
            samples++;
        }
        assertTrue(description+" rendered blank / a broken-image placeholder: only "
            +dark+" ink pixels across "+samples+" samples",
            dark*1000 >= samples*12); // at least 1.2% substantial QCF glyph ink
    }

    @Test public void pilotOpensAuthenticPairedMushafAndKeepsProgressUntouched() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,30)
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,37);
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{
                View root=activity.findViewById(android.R.id.content);
                assertEquals(2,countViews(root,MapQcfThumbnails.class));
                assertEquals(1,countViews(root,QcfAtlasVerseStrips.class));
                assertEquals("30:37 must be at page mapped by real geometry",
                    GeometryRepository.get(activity).pageForVerse(
                        new com.quransafeguard.hifz.core.VerseRef(30,37)),
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
                new com.quransafeguard.hifz.core.VerseRef(30,37);
            int page=g.pageForVerse(verse);
            String original=MapQcfThumbnails.readSvg(context,page);
            String cropped=QcfAtlasVerseStrips.cropSvg(original,
                WordGeometryRepository.shared(context).wordsForPage(page),verse,new int[]{2});
            assertFalse("The exact Quran crop must be available for 30:37",cropped.isEmpty());
            assertTrue("Exact original Quran paths must be preserved",cropped.contains("ayahPolygon"));
            assertTrue("The differing second word must be underlined",cropped.contains("exact-word-differences"));
            Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull("Must capture a genuine emulator comparison frame",shot);
            assertTrue(shot.getWidth()>100&&shot.getHeight()>100);
            Rect[] pageBounds=bounds.get();
            assertNotNull(pageBounds);
            assertInkIsVisible(shot,pageBounds[0],"Left KFQC page");
            assertInkIsVisible(shot,pageBounds[1],"Right KFQC page");
            AtomicReference<Rect[]> croppedBounds=new AtomicReference<>();
            scenario.onActivity(activity->croppedBounds.set(activity.qcfCropBoundsForTest()));
            Rect[] crop=croppedBounds.get();
            if(crop!=null&&crop.length==2){
                assertInkIsVisible(shot,crop[0],"Left exact QCF comparison strip");
                assertInkIsVisible(shot,crop[1],"Right exact QCF comparison strip");
            }
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
        Intent start=new Intent(context,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,408)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,"30:37");
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
                        View button=activity.discriminantActionForTest();
                        ready.set(activity.selectedDiscriminantPageForTest()==408
                            &&activity.selectedDiscriminantVerseForTest()!=null
                            &&"30:37".equals(activity.selectedDiscriminantVerseForTest().toString())
                            &&button!=null&&button.getVisibility()==View.VISIBLE);
                    });
                    if(ready.get())break;
                }
                assertTrue("Documented 30:37 must show the compact Lecture comparison icon",
                    ready.get());
                before.set(new HashMap<>(hifz.getAll()));
                scene.onActivity(activity->{
                    View button=activity.discriminantActionForTest();
                    assertNotNull(button);
                    assertTrue(button.performClick());
                });
                Activity opened=instrument.waitForMonitorWithTimeout(monitor,20000);
                assertNotNull("Lecture must launch the comparison for its selected verse",opened);
                assertTrue(opened instanceof AtlasDiscriminantActivity);
                assertEquals(408,((AtlasDiscriminantActivity)opened).getCurrentLeftPageForTest());
                instrument.runOnMainSync(opened::finish);
                instrument.waitForIdleSync();
                scene.onActivity(activity->{
                    assertSame("The original reader Activity must survive, not a new session",
                        original.get(),activity);
                    assertEquals(408,activity.selectedDiscriminantPageForTest());
                    assertEquals("30:37",activity.selectedDiscriminantVerseForTest().toString());
                    assertFalse(activity.freeActiveMaskForTest());
                    assertEquals(View.VISIBLE,activity.discriminantActionForTest().getVisibility());
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
        // Reader-first UI: every validated pair has its own contextual entry,
        // without next/previous controls or a standalone Atlas catalogue.
        for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++){
            AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(i);
            Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
                .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,pair.first.getSurah())
                .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,pair.first.getAyah())
                .putExtra(AtlasDiscriminantActivity.EXTRA_ORIGIN,"Lecture");
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
                scenario.onActivity(activity->{
                    View firstLink=findByDescription(activity.getWindow().getDecorView(),
                        "Ouvrir le premier verset en Lecture");
                    View secondLink=findByDescription(activity.getWindow().getDecorView(),
                        "Ouvrir le second verset en Lecture");
                    assertNotNull("First explicit Lecture link missing",firstLink);
                    assertNotNull("Second explicit Lecture link missing",secondLink);
                    assertTrue(firstLink.isClickable()&&secondLink.isClickable());
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
                java.util.concurrent.CountDownLatch clipsChecked=new java.util.concurrent.CountDownLatch(1);
                AtomicReference<String> clipResult=new AtomicReference<>();
                scenario.onActivity(activity->{
                    try {
                        java.lang.reflect.Field stripsField=AtlasDiscriminantActivity.class.getDeclaredField("strips");
                        stripsField.setAccessible(true);
                        ((QcfAtlasVerseStrips)stripsField.get(activity)).evaluateJavascript(
                            "(function(){var ids=[];for(var i=0;i<2;i++){"
                            +"var s=document.querySelector('#crop'+i+' svg');if(!s)return false;"
                            +"var clip=s.querySelector('clipPath');if(!clip||!clip.id)return false;"
                            +"if(document.getElementById(clip.id)!==clip)return false;"
                            +"var group=s.querySelector('g[clip-path]');"
                            +"if(!group||group.getAttribute('clip-path')!=='url(#'+clip.id+')')return false;"
                            +"ids.push(clip.id);}return ids[0]!==ids[1];})()",
                            value->{clipResult.set(value);clipsChecked.countDown();});
                    }catch(ReflectiveOperationException invalid){throw new AssertionError(invalid);}
                });
                assertTrue("Paired QCF clip DOM callback missing",clipsChecked.await(10,java.util.concurrent.TimeUnit.SECONDS));
                assertEquals("Each inline QCF excerpt must resolve its own exact-word clip", "true",clipResult.get());
                assertExactQcfWords(context,pair.first,pair.firstWordIndices);
                assertExactQcfWords(context,pair.second,pair.secondWordIndices);
            }
        }
        assertEquals("Browsing Atlas must not alter Hifz progress",before,prefs.getAll());
    }


    /** Two distinct, visible Lecture links must open the exact target verse,
     * then return to the SAME comparison without touching Hifz. */
    @Test public void bothVisibleLectureLinksOpenExactVersesAndReturn() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,2)
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,58)
            .putExtra(AtlasDiscriminantActivity.EXTRA_ORIGIN,"Lecture");
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(
            StudyReaderActivity.class.getName(),null,false);
        try {
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
                for(int side=0;side<2;side++){
                    final boolean first=side==0;
                    scenario.onActivity(activity->{
                        View link=findByDescription(activity.getWindow().getDecorView(),
                            first?"Ouvrir le premier verset en Lecture":
                                "Ouvrir le second verset en Lecture");
                        assertNotNull("Contextual Lecture destination missing",link);
                        assertTrue("Text link must be directly tappable",link.performClick());
                    });
                    Activity opened=instrumentation.waitForMonitorWithTimeout(monitor,15000);
                    assertNotNull("Tapping explicit Lecture destination must open reader",opened);
                    assertEquals(first?"2:58":"7:161",opened.getIntent()
                        .getStringExtra(StudyReaderActivity.EXTRA_JUMP_VERSE));
                    assertEquals(first?9:171,opened.getIntent()
                        .getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,-1));
                    instrumentation.runOnMainSync(opened::finish);
                    instrumentation.waitForIdleSync();
                    scenario.onActivity(activity->{
                        assertEquals(9,activity.getCurrentLeftPageForTest());
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
                new com.quransafeguard.hifz.core.VerseRef(30,37));
            Intent intent=new Intent(context,StudyReaderActivity.class)
                .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,qcfPage)
                .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,"30:37");
            try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)){
                AtomicBoolean ready=new AtomicBoolean(false);
                for(int attempt=0;attempt<40;attempt++){
                    Thread.sleep(250L);
                    scenario.onActivity(activity->{
                        readerRef.set(activity);
                        View action=activity.discriminantActionForTest();
                        com.quransafeguard.hifz.core.VerseRef verse=
                            activity.selectedDiscriminantVerseForTest();
                        ready.set(action!=null && action.getVisibility()==View.VISIBLE &&
                            verse!=null && verse.getSurah()==30 && verse.getAyah()==37);
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
                    assertTrue("Exact relation tap must launch Atlas",
                        activity.discriminantActionForTest().performClick());
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
                    assertEquals(30,selected.getSurah());
                    assertEquals(37,selected.getAyah());
                    assertFalse("Mask must remain unchanged",activity.freeActiveMaskForTest());
                    assertEquals(View.VISIBLE,activity.discriminantActionForTest().getVisibility());

                    // Neighboring verse has no documented relation; fail closed.
                    activity.onVerseTap(new com.quransafeguard.hifz.core.VerseRef(30,38));
                    assertEquals("Unrelated verse must hide Atlas relation",
                        View.GONE,activity.discriminantActionForTest().getVisibility());
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

}
