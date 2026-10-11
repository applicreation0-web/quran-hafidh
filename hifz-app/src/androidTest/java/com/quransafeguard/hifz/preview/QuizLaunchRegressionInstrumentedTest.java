package com.quransafeguard.hifz.preview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Reproduces the user's 2026-10-11 report on a real Android runtime:
 * tapping Quiz immediately exits the app. JVM/source audits never launched it.
 * Never touches memorization progress or attempts to seed fake Quran data.
 */
public final class QuizLaunchRegressionInstrumentedTest {
    @Test public void quizMustStayOpenAfterRealActivityLaunchAndEmptyCorpusStart() {
        try(ActivityScenario<QuizActivity> scenario=ActivityScenario.launch(QuizActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View root=activity.findViewById(android.R.id.content);
                assertNotNull("Quiz activity window missing",root);
                assertTrue("Quiz setup must actually render",containsExactText(root,"Quiz"));
                View start=findDescription(root,"Commencer");
                assertNotNull("Quiz start command not rendered",start);
                assertTrue("Quiz start command must be enabled",start.isEnabled());
                start.performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View root=activity.findViewById(android.R.id.content);
                assertNotNull(root);
                assertFalse("Quiz must not finish just because its learned corpus is empty",
                    activity.isFinishing());
            });
        }
    }

    @Test public void everyContractualIconInflatesOnAndroid() {
        android.content.Context context =
            androidx.test.core.app.ApplicationProvider.getApplicationContext();
        // Source SHA and XML presence are not enough. The Quiz crash came from
        // a vector with malformed pathData that aapt accepted but Android rejected.
        int[] assets=new int[]{
            R.drawable.ic_ui_home,R.drawable.ic_ui_reading,
            R.drawable.ic_ui_semantic_map,R.drawable.ic_ui_recall_key,
            R.drawable.ic_ui_tafsir,R.drawable.ic_ui_focus,
            R.drawable.ic_ui_revision,R.drawable.ic_ui_difficulty,
            R.drawable.ic_ui_edit,R.drawable.ic_ui_quiz,
            R.drawable.ic_ui_progress_map,R.drawable.ic_ui_resume,
            R.drawable.ic_ui_back,R.drawable.ic_ui_validate,
            R.drawable.ic_ui_close,R.drawable.ic_ui_undo,
            R.drawable.ic_ui_annotation_erase,R.drawable.ic_ui_info,
            R.drawable.ic_ui_reveal,R.drawable.ic_ui_record,
            R.drawable.ic_ui_play,R.drawable.ic_ui_pause,R.drawable.ic_ui_more
        };
        assertEquals("Contractual icon count",23,assets.length);
        // Broad UI gate: the 23 contracted glyphs are NOT the only icons used
        // in Hifz, settings and annotations. Inflate every packaged ic_* icon.
        int all=0;
        for(java.lang.reflect.Field field:R.drawable.class.getFields()) {
            if(!field.getName().startsWith("ic_"))continue;
            try {
                int resource=field.getInt(null);
                android.graphics.drawable.Drawable drawable=
                    context.getResources().getDrawable(resource,context.getTheme());
                assertNotNull("Null packaged icon "+field.getName(),drawable);
                drawable.setBounds(0,0,48,48);
                android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(
                    48,48,android.graphics.Bitmap.Config.ARGB_8888);
                drawable.draw(new android.graphics.Canvas(image));
                image.recycle();
                all++;
            } catch(ReflectiveOperationException error) {
                throw new AssertionError("Broken drawable identifier "+field.getName(),error);
            } catch(RuntimeException error) {
                throw new AssertionError("Android cannot render "+field.getName(),error);
            }
        }
        assertTrue("Expected the full Hifz icon set, not only the 23",all>=23);
        for(int resource:assets) {
            android.graphics.drawable.Drawable d=
                context.getResources().getDrawable(resource,context.getTheme());
            assertNotNull("Failed to inflate icon "+context.getResources().getResourceEntryName(resource),d);
            d.setBounds(0,0,48,48);
            android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888);
            d.draw(new android.graphics.Canvas(pixels));
            pixels.recycle();
        }
    }


    @Test public void learnedRealQuranPageMakesAQuizQuestionScreenNotCrash() {
        android.content.Context context=androidx.test.core.app.ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(context);
        HifzPrefs prefs=new HifzPrefs(context);
        java.util.List<String> realLines=geometry.lineIdsOnPage(523);
        assertTrue("Mushaf QCF page 523 lines must exist",realLines.size()>5);
        // Emulator-only synthetic LEARNING completion. No user-device data is read or changed.
        prefs.transitionV6Lines(realLines,HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        QuizCorpus check=new QuizCorpus(geometry,WordGeometryRepository.shared(context));
        assertFalse("Newly Appris corpus must immediately enter the official 1.17.1 Quiz",
            check.questions(prefs.progressionSnapshotV6(),QuizCorpus.Mode.MIXED,10).isEmpty());
        try(ActivityScenario<QuizActivity> scenario=ActivityScenario.launch(QuizActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-quiz-accueil.png");
            scenario.onActivity(activity->{
                View start=findDescription(activity.findViewById(android.R.id.content),"Commencer");
                assertNotNull(start);
                start.performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                assertFalse("Quiz closed during real corpus question",activity.isFinishing());
                assertNotNull("Mushaf question reader missing",
                    reflect(activity,"mushaf"));
                assertTrue("First real question not shown",
                    activity.findViewById(android.R.id.content)!=null);
            });
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Quiz real learned Quran question");
            // Pixel-level adversarial audit. The preceding green run proved an Activity
            // was alive but its screenshot contained ZERO Quran ink in the reader body.
            // pageShown alone only confirms JS boot, not a usable visible question.
            scenario.onActivity(activity -> {
                android.webkit.WebView view=(android.webkit.WebView)reflect(activity,"mushaf");
                android.graphics.Rect rect=new android.graphics.Rect();
                assertTrue("Quiz Mushaf is not on screen",view.getGlobalVisibleRect(rect));
                assertTrue("Quiz Mushaf has no height: "+rect,rect.height()>150);
                assertTrue("Quiz Mushaf has no width: "+rect,rect.width()>200);
                android.util.Log.i("QuizQCFProbe","Mushaf layout = "+rect+
                    ", webview="+view.getWidth()+"x"+view.getHeight());
            });
            HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-quiz-question.png");
            final int[] initialInk={0};
            android.graphics.Bitmap bmp=InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().takeScreenshot();
            assertNotNull("Cannot inspect rendered Quiz body",bmp);
            try {
                // The region between header and footer must contain actual Quran ink;
                // a completely blank WebView fails even when Android reports pageShown.
                int w=bmp.getWidth(),h=bmp.getHeight(),dark=0;
                for(int y=(int)(h*0.20f);y<(int)(h*0.80f);y+=2)
                    for(int x=(int)(w*0.20f);x<(int)(w*0.80f);x+=2) {
                        int col=bmp.getPixel(x,y);
                        int rgb=(android.graphics.Color.red(col)+
                            android.graphics.Color.green(col)+
                            android.graphics.Color.blue(col))/3;
                        if(rgb<180)dark++;
                    }
                initialInk[0]=dark;
            } finally {bmp.recycle();}
            if(initialInk[0]<=15) {
                // Diagnostic only: show the SAME real Quran page without an eraser.
                // If ink returns, the bug is the Quiz mask/focus adapter, not QCF assets.
                scenario.onActivity(activity -> {
                    MushafView v=(MushafView)reflect(activity,"mushaf");
                    v.setMask(0);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                android.os.SystemClock.sleep(1100);
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-quiz-unmasked-diagnostic.png");
                android.graphics.Bitmap unmasked=InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().takeScreenshot();
                int extra=0;
                try {
                    int w=unmasked.getWidth(),h=unmasked.getHeight();
                    for(int y=(int)(h*0.20f);y<(int)(h*0.80f);y+=2)
                        for(int x=(int)(w*0.20f);x<(int)(w*0.80f);x+=2) {
                            int color=unmasked.getPixel(x,y);
                            int grey=(android.graphics.Color.red(color)+
                                android.graphics.Color.green(color)+
                                android.graphics.Color.blue(color))/3;
                            if(grey<180)extra++;
                        }
                } finally {unmasked.recycle();}
                assertTrue("QUIZ BLANK: original masked="+initialInk[0]+
                    " ink; unmasked diagnostic="+extra+
                    " ink. Do not publish a Quiz question without visible Quran",false);
            }
        }
    }

    private static Object reflect(Object owner,String name) {
        try {
            java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        }catch(Exception ex){throw new AssertionError("Cannot inspect "+name,ex);}
    }

    private static boolean containsExactText(View view,String text) {
        if(view instanceof TextView && text.contentEquals(((TextView)view).getText()))return true;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)
                if(containsExactText(group.getChildAt(i),text))return true;
        }
        return false;
    }

    private static View findDescription(View view,String description) {
        CharSequence value=view.getContentDescription();
        if(value!=null && description.contentEquals(value))return view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                View found=findDescription(group.getChildAt(i),description);
                if(found!=null)return found;
            }
        }
        return null;
    }
}
