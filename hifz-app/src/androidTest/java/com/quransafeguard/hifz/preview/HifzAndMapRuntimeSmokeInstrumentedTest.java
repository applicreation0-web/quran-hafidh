package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test public void readingAndGraphicalMapCompleteTheQcfRoundTrip() {
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

        try(ActivityScenario<IbnKathirMapActivity> shortMap=ActivityScenario.launch(
                new Intent(context,IbnKathirMapActivity.class)
                    .putExtra(IbnKathirMapActivity.EXTRA_SURAH,1)
                    .putExtra(IbnKathirMapActivity.EXTRA_AYAH,1))) {
            awaitMushafPage(shortMap,"pagePreview","Carte courte Al-Fatiha");
            shortMap.onActivity(activity->{
                assertEquals(1,((Integer)reflect(activity,"surah")).intValue());
                assertFalse("Graphical map must expose nodes",
                    ((List<?>)reflect(activity,"graphicalNodes")).isEmpty());
            });
            saveScreenshot("audit-cp03-carte-courte.png");
        }

        GeometryRepository geometry=GeometryRepository.get(context);
        IbnKathirGroupIndex.Group multipage=null;
        outer: for(int surah=1;surah<=114;surah++) {
            for(IbnKathirGroupIndex.Group group:IbnKathirGroupIndex.shared().groupsForSurah(surah)) {
                if(IbnKathirMapModel.pageSpan(group,geometry).last>
                        IbnKathirMapModel.pageSpan(group,geometry).first) {
                    multipage=group;
                    break outer;
                }
            }
        }
        assertNotNull("The verified corpus must contain a multipage group",multipage);
        final IbnKathirGroupIndex.Group selectedMultipage=multipage;
        try(ActivityScenario<IbnKathirMapActivity> map=ActivityScenario.launch(
                new Intent(context,IbnKathirMapActivity.class)
                    .putExtra(IbnKathirMapActivity.EXTRA_SURAH,multipage.surah)
                    .putExtra(IbnKathirMapActivity.EXTRA_AYAH,multipage.startAyah))) {
            awaitMushafPage(map,"pagePreview","Carte multipage Ibn Kathir");
            AtomicInteger selectedIndex=new AtomicInteger();
            AtomicInteger selectedPage=new AtomicInteger();
            map.onActivity(activity->{
                selectedIndex.set((Integer)reflect(activity,"selectedGroupIndex"));
                selectedPage.set((Integer)reflect(activity,"previewPage"));
                assertEquals(IbnKathirMapModel.pageSpan(selectedMultipage,geometry).first,
                    selectedPage.get());
            });
            saveScreenshot("audit-cp03-carte-multipage.png");

            map.onActivity(activity->invoke(activity,"openPreviewInReader"));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(1200);
            assertTrue("Touching the QCF preview must open Lecture",
                resumedActivityName().endsWith("StudyReaderActivity"));
            saveScreenshot("audit-cp03-carte-lecture.png");
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(500);
            map.onActivity(activity->{
                assertEquals(selectedMultipage.surah,
                    ((Integer)reflect(activity,"surah")).intValue());
                assertEquals(selectedIndex.get(),
                    ((Integer)reflect(activity,"selectedGroupIndex")).intValue());
                assertEquals(selectedPage.get(),
                    ((Integer)reflect(activity,"previewPage")).intValue());
            });
            saveScreenshot("audit-cp03-carte-retour.png");
        }

        try(ActivityScenario<IbnKathirMapActivity> largeMap=ActivityScenario.launch(
                new Intent(context,IbnKathirMapActivity.class)
                    .putExtra(IbnKathirMapActivity.EXTRA_SURAH,2)
                    .putExtra(IbnKathirMapActivity.EXTRA_AYAH,286))) {
            awaitMushafPage(largeMap,"pagePreview","Grande sourate Al-Baqara");
            largeMap.onActivity(activity->{
                List<?> nodes=(List<?>)reflect(activity,"graphicalNodes");
                int selected=(Integer)reflect(activity,"selectedGroupIndex");
                assertTrue("Al-Baqara must exercise a large graphical map",nodes.size()>50);
                View node=(View)nodes.get(selected);
                ((ScrollView)reflect(activity,"mapScroll")).scrollTo(0,node.getTop());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            saveScreenshot("audit-cp03-carte-grande-sourate.png");
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

    private static void invoke(Object owner,String name) {
        try {
            Method method=owner.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(owner);
        } catch(Exception x) {throw new AssertionError("Cannot invoke runtime action "+name,x);}
    }

    private static String resumedActivityName() {
        java.util.concurrent.atomic.AtomicReference<String> name=
            new java.util.concurrent.atomic.AtomicReference<>("");
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            java.util.Collection<android.app.Activity> resumed=
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
            if(!resumed.isEmpty()) name.set(resumed.iterator().next().getClass().getName());
        });
        return name.get();
    }
}
