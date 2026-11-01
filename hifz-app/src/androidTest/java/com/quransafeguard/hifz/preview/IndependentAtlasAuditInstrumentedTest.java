package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Synthetic emulator state only. Invoke the real polygon callback boundary;
 * existing tests separately exercise the actual WebView touch dispatch. */
public final class IndependentAtlasAuditInstrumentedTest {
    private static void call(AtlasDiscriminantActivity a,String name,Class<?>[] types,Object... args){
        try {Method m=AtlasDiscriminantActivity.class.getDeclaredMethod(name,types);
            m.setAccessible(true);m.invoke(a,args);}
        catch(Exception e){throw new AssertionError(e);}
    }
    private static void await(ActivityScenario<AtlasDiscriminantActivity> scene,VerseRef expected)throws Exception{
        AtomicBoolean loaded=new AtomicBoolean();
        long deadline=android.os.SystemClock.uptimeMillis()+20000;
        while(android.os.SystemClock.uptimeMillis()<deadline){
            scene.onActivity(a->{
                CharSequence[] text=a.tafsirTextsForTest();
                loaded.set(expected.equals(a.selectedTafsirVerseForTest())
                    &&a.tafsirHeadingTextForTest().equals("Tafsīr al-Jalālayn · "+expected)
                    &&text[a.selectedTafsirForTest()].length()>60
                    &&!text[a.selectedTafsirForTest()].toString().startsWith("Chargement"));
            });
            if(loaded.get())return;
            Thread.sleep(100);
        }
        fail("Exact clicked Tafsir did not load: "+expected);
    }
    @Test public void fullPageNeighborTafsirSurvivesRecreationAndReturnsToPairWithoutHifzWrites()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        new HifzPrefs(c);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        Intent intent=new Intent(c,AtlasDiscriminantActivity.class)
            .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,2)
            .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,63);
        try(ActivityScenario<AtlasDiscriminantActivity> s=ActivityScenario.launch(intent)){
            VerseRef neighbor=new VerseRef(2,64);
            s.onActivity(a->{
                call(a,"selectExactTafsir",new Class<?>[]{int.class,int.class,VerseRef.class},0,10,neighbor);
                assertEquals("A full-page neighbor tap must open its own Tafsir",neighbor,a.selectedTafsirVerseForTest());
                assertEquals("Tafsīr al-Jalālayn · 2:64",a.tafsirHeadingTextForTest());
                // Keep canonical pair tabs, so either compared verse remains one click away.
                assertEquals("2:63",a.tafsirTabLabelForTest(0));
                assertFalse("A 2:63 pivot must not masquerade as a 2:64 excerpt",a.tafsirPivotEmbeddedForTest());
            });
            await(s,neighbor);
            CharSequence expected=BaqaraPilotScreen.renderExistingJalalayn(new TafsirRepository(c).load(neighbor));
            s.onActivity(a->{
                String actual=a.tafsirTextsForTest()[0].toString();
                assertTrue("Displayed commentary must come from the clicked ayah",expected.toString().endsWith(actual));
                call(a,"selectExactTafsir",new Class<?>[]{int.class,int.class,VerseRef.class},0,10,new VerseRef(3,1));
                assertEquals("Reject an ayah outside the displayed QCF page",neighbor,a.selectedTafsirVerseForTest());
            });
            s.recreate();await(s,neighbor);
            s.onActivity(a->a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            await(s,neighbor);
            s.onActivity(a->call(a,"selectTafsir",new Class<?>[]{int.class},0));
            await(s,new VerseRef(2,63));
            s.onActivity(a->assertTrue(a.tafsirPairLockedForTest()));
            // Race two independent disk reads: latest requested reference wins.
            s.onActivity(a->{
                call(a,"selectExactTafsir",new Class<?>[]{int.class,int.class,VerseRef.class},1,14,new VerseRef(2,92));
                call(a,"selectExactTafsir",new Class<?>[]{int.class,int.class,VerseRef.class},1,14,new VerseRef(2,90));
            });
            await(s,new VerseRef(2,90));
            s.onActivity(a->a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        }
        assertEquals("Tafsir clicks/recreation/rotation must preserve every Hifz preference",before,prefs.getAll());
    }
}
