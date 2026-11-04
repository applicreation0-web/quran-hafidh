package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual KFQC paired SVG and window layout, phone + BOOX portrait emulator. */
public final class CorrespondenceContextInstrumentedTest {
    @Test public void authenticDoubleQcfAndResponsiveTafsir()throws Exception {
        assertTrue("Isolated workflow must explicitly enable editorial pilot",
            BuildConfig.CORRESPONDENCES_EDITORIAL_PREVIEW);
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=new Intent(context,CorrespondenceComparisonActivity.class)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_SURAH,12)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_AYAH,4)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_ID,"R037")
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
                if(rendered.get())return;
                Thread.sleep(250L);
            }
            fail("Both authentic KFQC pages failed to render");
        }
    }
    @Test public void originalVerseAnchorsAreNotGuessed(){
        Context c=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(c);
        int page=geometry.pageForVerse(new VerseRef(12,4));
        assertTrue(CorrespondenceCases.versesOnPage(page,geometry).contains(new VerseRef(12,4)));
        assertFalse(CorrespondenceCases.forVerse(new VerseRef(12,5)).size()>0);
    }
}