package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
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
            Thread.sleep(3500L); // allow two WebViews to decode exact KFQC SVG on emulated Android
            Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull("Must capture a genuine emulator comparison frame",shot);
            assertTrue(shot.getWidth()>100&&shot.getHeight()>100);
            File out=new File(context.getExternalFilesDir(null),"haafidh-atlas-discriminant-portrait.png");
            try(FileOutputStream stream=new FileOutputStream(out)){
                assertTrue(shot.compress(Bitmap.CompressFormat.PNG,100,stream));
                stream.flush();
            }
            shot.recycle();
        }
        assertEquals("Atlas comparison must never mutate the main Hifz state",before,prefs.getAll());
    }
}
