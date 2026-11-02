package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Actual disposable-emulator surfaces. Opening an empty state is not a completed Hifz test.
 * Private Atlas screens are captured as unavailable when the original private corpus is absent.
 */
public final class Independent11710ScreensInstrumentedTest {
    private static void call(Object o, String method) {
        try { Method m=o.getClass().getDeclaredMethod(method);m.setAccessible(true);m.invoke(o); }
        catch (Exception e) { throw new AssertionError(method,e); }
    }
    private static Object field(Object o,String name) {
        try { Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o); }
        catch (Exception e) {throw new AssertionError(name,e);}
    }
    private static View findText(View root,String text) {
        if(root instanceof TextView && text.contentEquals(((TextView)root).getText()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++) {
            View v=findText(((ViewGroup)root).getChildAt(i),text);if(v!=null)return v;
        }
        return null;
    }
    private static void open(Activity a,String label) {
        View v=findText(a.findViewById(android.R.id.content),label);
        assertNotNull("Missing actual navigation: "+label,v);
        while(v!=null && !v.isClickable())v=v.getParent() instanceof View?(View)v.getParent():null;
        assertNotNull("No clickable navigation: "+label,v);assertTrue(v.performClick());
    }
    static void capture(String name) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        android.os.SystemClock.sleep(900L);
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        Context c=ApplicationProvider.getApplicationContext();
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"independent-"+name+".png");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/HaafidhAudit");
        android.net.Uri uri=c.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(uri);
        try(java.io.OutputStream stream=c.getContentResolver().openOutputStream(uri)) {
            assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream));
        }catch(Exception e){throw new AssertionError("Cannot save actual Android screenshot",e);}
        finally{image.recycle();}
    }
    @Test public void homeCalendarAndAllSettingsPages() {
        try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)) {
            capture("home");
            s.onActivity(a->call(a,"showParcours"));capture("parcours-top");
            s.onActivity(a->{ScrollView v=(ScrollView)field(a,"parcoursScroll");v.fullScroll(View.FOCUS_DOWN);});
            capture("parcours-calendar-bottom");
            s.onActivity(a->call(a,"chooseWeeklyProgram"));capture("weekly-program-dialog");
        }
        String[] labels={"Parcours","Plages & corpus","Révision","Audio & affichage","Sauvegarde","Avancé"};
        for(int i=0;i<labels.length;i++) {
            final String label=labels[i];
            try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)) {
                if(i==0)capture("settings-root");
                s.onActivity(a->open(a,label));capture("settings-"+i+"-top");
                s.onActivity(a->{View content=a.findViewById(android.R.id.content);
                    if(content instanceof ViewGroup) {
                        View first=((ViewGroup)content).getChildAt(0);
                        if(first instanceof ScrollView)((ScrollView)first).fullScroll(View.FOCUS_DOWN);
                    }});
                capture("settings-"+i+"-bottom");
                if("Sauvegarde".equals(label)) {
                    s.onActivity(a->call(a,"confirmImportBackup"));capture("import-confirmation");
                }
                if("Avancé".equals(label)) {
                    s.onActivity(a->call(a,"showDiagnostic"));capture("diagnostic-dialog");
                }
            }
        }
        try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)) {
            s.onActivity(a->open(a,"Avancé"));
            s.onActivity(a->open(a,"À propos du parcours"));
            capture("settings-about-top");
            s.onActivity(a->{View first=((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);
                if(first instanceof ScrollView)((ScrollView)first).fullScroll(View.FOCUS_DOWN);});
            capture("settings-about-bottom");
        }
    }
    @Test public void realReadingProgressAndUnavailablePrivateStates() {
        try(ActivityScenario<StudyReaderActivity> s=ActivityScenario.launch(StudyReaderActivity.class)) {
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","independent reader");
            capture("reader");
            s.onActivity(a->call(a,"hideControls"));capture("reader-fullscreen");
        }
        try(ActivityScenario<FreeMemActivity> s=ActivityScenario.launch(FreeMemActivity.class)) {
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","independent free memorization");
            capture("free-memory");
        }
        try(ActivityScenario<ProgressMapActivity> s=ActivityScenario.launch(ProgressMapActivity.class)) {capture("progress-map");}
        try(ActivityScenario<AtlasMapActivity> s=ActivityScenario.launch(AtlasMapActivity.class)) {capture("atlas-private-corpus-state");}
        try(ActivityScenario<AtlasSourcePdfActivity> s=ActivityScenario.launch(AtlasSourcePdfActivity.class)) {capture("atlas-private-pdf-state");}
    }
    @Test public void allStructuredModeEntryStates() {
        Context c=ApplicationProvider.getApplicationContext();
        String[] modes={HifzSessionActivity.SABQI,HifzSessionActivity.SABQI_TODAY_REVIEW,
            HifzSessionActivity.ITQAN,HifzSessionActivity.RECENT_SABQI_REVIEW,
            HifzSessionActivity.LEARNING_CONSOLIDATION,HifzSessionActivity.CONSOLIDATION_FINAL,
            HifzSessionActivity.LEARNING_FINAL,HifzSessionActivity.POST_FINAL_RECALL,
            HifzSessionActivity.MURAJAAH,HifzSessionActivity.MURAJAAH_ACTIVE};
        for(String mode:modes) {
            Intent intent=new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
            try(ActivityScenario<HifzSessionActivity> s=ActivityScenario.launch(intent)) {
                s.onActivity(a->{assertFalse(a.isFinishing());assertNotNull(a.findViewById(android.R.id.content));});
                capture("mode-"+mode.toLowerCase(java.util.Locale.ROOT));
            }
        }
    }
}
