package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Android runtime gates absent from the previous audit-only CI.
 * Pure opening/navigation smoke tests: never mark a Hifz unit complete. */
public final class HifzAndMapRuntimeSmokeInstrumentedTest {
    @Test public void eachOfficialHifzModeOpensWithoutCrash() {
        Context context=ApplicationProvider.getApplicationContext();
        String[] modes={
            HifzSessionActivity.SABQI,
            HifzSessionActivity.ITQAN,
            HifzSessionActivity.LEARNING_CONSOLIDATION,
            HifzSessionActivity.RECENT_SABQI_REVIEW,
            HifzSessionActivity.LEARNING_FINAL,
            HifzSessionActivity.CONSOLIDATION_FINAL,
            HifzSessionActivity.MURAJAAH_ACTIVE,
            HifzSessionActivity.MURAJAAH
        };
        for(String mode:modes) {
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,mode);
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(intent)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity->{
                    assertFalse("Unexpected close of Hifz "+mode,activity.isFinishing());
                    assertNotNull("Missing Hifz view "+mode,
                        activity.findViewById(android.R.id.content));
                    TextView program=(TextView)reflect(activity,"program");
                    String title=program.getText().toString();
                    assertFalse("Hifz "+mode+" rendered an error instead of a session: "+title,
                        title.contains("état à vérifier"));
                });
                if(HifzSessionActivity.SABQI.equals(mode) || HifzSessionActivity.ITQAN.equals(mode))
                    awaitMushafPage(scenario,"mushaf",mode);
                saveScreenshot("audit-hifz-"+mode.toLowerCase(java.util.Locale.ROOT)+".png");
            }
        }
    }

    @Test public void readingOpensQcfWithNoSemanticPassageList() {
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=new Intent(context,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,518);
        try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)) {
            awaitMushafPage(scenario,"mushaf","Lecture Mushaf without lists");
            scenario.onActivity(activity->{
                assertFalse("Reader must remain open",activity.isFinishing());
                android.widget.LinearLayout actions=(android.widget.LinearLayout)reflect(activity,"readerActions");
                for(int i=0;i<actions.getChildCount();i++) {
                    CharSequence c=actions.getChildAt(i).getContentDescription();
                    assertFalse("Passage-list Carte button still accessible",c!=null&&c.toString().equals("Carte"));
                }
            });
            saveScreenshot("audit-lecture-mushaf.png");
        }
    }

    /** Assert the actual QCF page was rendered, not merely an empty Activity shell.
     * The first audit passed while screenshots captured WebViews before onPageShown. */
    static <T extends android.app.Activity> void awaitMushafPage(
            ActivityScenario<T> scenario,String field,String label) {
        java.util.concurrent.atomic.AtomicBoolean drawn=new java.util.concurrent.atomic.AtomicBoolean();
        for(int attempt=0;attempt<44;attempt++) {
            scenario.onActivity(activity->{
                Object view=reflect(activity,field);
                assertTrue("Expected genuine MushafView in "+label,view instanceof MushafView);
                drawn.set((Boolean)reflect(view,"pageShown"));
            });
            if(drawn.get()) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                android.os.SystemClock.sleep(800);
                return;
            }
            android.os.SystemClock.sleep(250);
        }
        fail("QCF page was NOT rendered in real Android for "+label);
    }

    static void saveScreenshot(String name) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        android.os.SystemClock.sleep(900); // allow WebView/GPU frame to reach the display
        android.graphics.Bitmap image=
            InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull("Cannot capture Android UI screenshot "+name,image);
        Context context=ApplicationProvider.getApplicationContext();
        java.io.File base=context.getExternalFilesDir(null);
        assertNotNull("Android external audit dir unavailable",base);
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(base,name))) {
            assertTrue("PNG encoding failed",image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));
        } catch(java.io.IOException error) {
            throw new AssertionError("Cannot save "+name,error);
        } finally {
            image.recycle();
        }
    }

    @Test public void weeklyProgramCanBeChosenDirectlyWithoutEngineCredit() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                TextView selector=(TextView)reflect(activity,"dashboardTitle");
                assertNotNull(selector);
                assertEquals("Choisir le programme hebdomadaire",
                    selector.getContentDescription().toString());
                try {
                    java.lang.reflect.Method show=MainActivity.class.getDeclaredMethod("showParcours");
                    show.setAccessible(true);
                    show.invoke(activity);
                }catch(ReflectiveOperationException error) {throw new AssertionError(error);}
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            saveScreenshot("audit-programme-choix.png");
            scenario.onActivity(activity->{
                TextView selector=(TextView)reflect(activity,"dashboardTitle");
                assertTrue("Weekly mode selector must be clickable",selector.isClickable());
                selector.performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            saveScreenshot("audit-programme-dialogue.png");
            scenario.onActivity(activity->assertFalse("Schedule selector must not close MainActivity",activity.isFinishing()));
        }
    }

    private static Object reflect(Object owner,String name) {
        try {
            Field f=owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch(Exception x) {throw new AssertionError("Cannot inspect runtime view "+name,x);}
    }
}
