package com.quransafeguard.hifz.preview;

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
        if(description.contentEquals(root.getContentDescription()))return root;
        if(root instanceof ViewGroup) {
            ViewGroup parent=(ViewGroup)root;
            for(int i=0;i<parent.getChildCount();i++) {
                View found=findByDescription(parent.getChildAt(i),description);
                if(found!=null)return found;
            }
        }
        return null;
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
    /**
     * Every curated pair must survive the same genuine asynchronous QCF decoding.
     * The first pilot's screenshot test remains the stronger pixel/underline check;
     * this guards against later pairs hiding missing pages or invalid verse geometry.
     */
    @Test public void everyPilotPairLoadsBothAuthenticQcfPagesAndStrips() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        AtlasDiscriminantCases.Entry first=AtlasDiscriminantCases.PILOT.get(0);
        Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,first.first.getSurah())
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,first.first.getAyah());
        // Test the user's actual next-arrow navigation in one screen; repeatedly
        // destroying three heavy WebViews is not the production navigation path.
        try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
            for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++){
                AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(i);
                if(i>0)scenario.onActivity(activity->{
                    View next=findByDescription(activity.getWindow().getDecorView(),
                        "Passage suivant du corpus");
                    assertNotNull("Atlas next-couple button missing",next);
                    assertTrue("Atlas next-couple button not clickable",next.performClick());
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
                            " / "+pair.second,0,activity.qcfPagesFailedForTest());
                        ready.set(activity.qcfPagesReadyForTest()&&activity.qcfStripsReadyForTest());
                    });
                    if(ready.get())break;
                }
                assertTrue("Both original QCF pages and excerpts must load: "+
                    pair.first+" / "+pair.second,ready.get());
                assertExactQcfWords(context,pair.first,pair.firstWordIndices);
                assertExactQcfWords(context,pair.second,pair.secondWordIndices);
            }
        }
        assertEquals("Browsing Atlas must not alter Hifz progress",before,prefs.getAll());
    }

}
