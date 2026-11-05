package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

/** Same capture code is compiled on the pinned BEFORE and AFTER sources. No fabricated image. */
public final class MandatCaptureInstrumentedTest {
    static void capture(String name)throws Exception{
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        EmulatorLauncherAnr.requireApplicationForeground(ins);
        ins.waitForIdleSync();Thread.sleep(500);
        String path="/data/local/tmp/haafidh-"+name+".png";
        try(InputStream out=new ParcelFileDescriptor.AutoCloseInputStream(ins.getUiAutomation().executeShellCommand("screencap -p "+path))){while(out.read()!=-1){}}
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(ins.getUiAutomation().executeShellCommand("cat "+path))){
            android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(in);assertNotNull(bitmap);
            try{
                long light=0,ink=0,total=0;
                for(int y=bitmap.getHeight()/10;y<bitmap.getHeight()*9/10;y+=4)
                    for(int x=4;x<bitmap.getWidth()-4;x+=4){int c=bitmap.getPixel(x,y);int gray=(android.graphics.Color.red(c)+android.graphics.Color.green(c)+android.graphics.Color.blue(c))/3;total++;if(gray>210)light++;if(gray<100)ink++;}
                assertTrue("Capture must contain actual light paper",light>total/4);
                assertTrue("Capture must contain actual dark text",ink>total/500);
            }finally{bitmap.recycle();}
        }
    }
    @Test public void beforeAfterOriginalReaderFoliosAndSources()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        String phase=InstrumentationRegistry.getArguments().getString("mandatPhase","apres");
        for(int page:new int[]{11,14,532,235,529,293}){
            try(ActivityScenario<StudyReaderActivity> s=ActivityScenario.launch(new Intent(c,StudyReaderActivity.class)
                .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page))){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","mandat "+phase+" p."+page);
                capture(phase+"-lecture-p"+page);
            }
        }
        try(ActivityScenario<CorrespondenceComparisonActivity> s=ActivityScenario.launch(new Intent(c,CorrespondenceComparisonActivity.class)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_SURAH,12)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_AYAH,3)
            .putExtra(CorrespondenceComparisonActivity.EXTRA_ID,"R008"))){
            long until=System.currentTimeMillis()+45000;AtomicBoolean ready=new AtomicBoolean();
            while(System.currentTimeMillis()<until){s.onActivity(a->ready.set(a.bothAuthenticQcfPanesReadyForTest()&&a.visibleTafsirReadyForTest()));if(ready.get())break;Thread.sleep(100);}
            assertTrue(ready.get());capture(phase+"-comparaison-p235");
            s.onActivity(a->{try{java.lang.reflect.Method show=a.getClass().getDeclaredMethod("showSources");show.setAccessible(true);show.invoke(a);}catch(Exception e){throw new AssertionError(e);}});
            capture(phase+"-sources");
        }
    }
}
