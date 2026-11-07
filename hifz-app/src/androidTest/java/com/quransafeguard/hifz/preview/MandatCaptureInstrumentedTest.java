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
        String path="/data/local/tmp/haafidh-"+name+".png";
        // The QCF SVG may be ready in the WebView DOM before its pixels have
        // reached the BOOX-emulated display surface. Capture actual SCREEN
        // frames until ink is rendered; a persistent white page MUST still fail.
        long deadline=android.os.SystemClock.uptimeMillis()+25000;
        long light=0,ink=0,total=0,folioInk=0,folioSamples=0;
        int frames=0;
        boolean comparison=name.contains("comparaison-p")||name.contains("navigation-p")
            ||name.startsWith("frise-");
        long leftInk=0,rightInk=0,leftSamples=0,rightSamples=0;
        do{
            ins.waitForIdleSync();
            try(InputStream out=new ParcelFileDescriptor.AutoCloseInputStream(
                    ins.getUiAutomation().executeShellCommand("screencap -p "+path))){
                while(out.read()!=-1){}
            }
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(
                    ins.getUiAutomation().executeShellCommand("cat "+path))){
                android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(in);
                assertNotNull("Actual emulator screenshot missing: "+name,bitmap);
                try{
                    light=0;ink=0;total=0;folioInk=0;folioSamples=0;
                    leftInk=0;rightInk=0;leftSamples=0;rightSamples=0;
                    // Sample the central folio/content region separately from
                    // navigation, status bars and English Tafsir in the footer.
                    // A "Chargement du Mushaf QCF..." placeholder must NEVER
                    // count as proof that an authentic QCF page is painted.
                    final int top=bitmap.getHeight()*16/100;
                    final int bottom=bitmap.getHeight()*68/100;
                    final int left=bitmap.getWidth()*8/100;
                    final int right=bitmap.getWidth()*92/100;
                    for(int y=bitmap.getHeight()/10;y<bitmap.getHeight()*9/10;y+=4)
                        for(int x=4;x<bitmap.getWidth()-4;x+=4){
                            int c=bitmap.getPixel(x,y);
                            int gray=(android.graphics.Color.red(c)+android.graphics.Color.green(c)
                                +android.graphics.Color.blue(c))/3;
                            total++;if(gray>210)light++;if(gray<100)ink++;
                            if(y>=top&&y<bottom&&x>=left&&x<right){
                                folioSamples++;if(gray<100)folioInk++;
                                if(x<bitmap.getWidth()/2){
                                    leftSamples++;if(gray<100)leftInk++;
                                }else{
                                    rightSamples++;if(gray<100)rightInk++;
                                }
                            }
                        }
                }finally{bitmap.recycle();}
            }
            frames++;
            // For the Mushaf and comparison screenshots, require real ink
            // in the folio viewport, not only in static headers or Tafsir.
            boolean qcfScreenshot=name.contains("lecture-p")
                ||comparison;
            boolean actualFolio=!qcfScreenshot
                ||(folioSamples>0&&folioInk>folioSamples/50);
            boolean bothPanes=!comparison||(leftSamples>0&&rightSamples>0
                &&leftInk>leftSamples/50&&rightInk>rightSamples/50);
            if(total>0&&light>total/4&&ink>total/500&&actualFolio&&bothPanes)return;
            if(android.os.SystemClock.uptimeMillis()>=deadline)break;
            Thread.sleep(350);
        }while(true);
        // Never manufacture image content or accept a blank capture.
        assertTrue("Capture lacks real light paper after "+frames+" frames: "+name
            +" light="+light+"/"+total,total>0&&light>total/4);
        fail("Capture has no painted QCF folio (or is blank) after "+frames
            +" frames: "+name+" allInk="+ink+"/"+total
            +" folioInk="+folioInk+"/"+folioSamples
            +" (required >2% central folio pixels)");
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
