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
                    androidx.core.content.ContextCompat.getDrawable(context,resource);
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
                androidx.core.content.ContextCompat.getDrawable(context,resource);
            assertNotNull("Failed to inflate icon "+context.getResources().getResourceEntryName(resource),d);
            d.setBounds(0,0,48,48);
            android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(48,48,android.graphics.Bitmap.Config.ARGB_8888);
            d.draw(new android.graphics.Canvas(pixels));
            pixels.recycle();
        }
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
        if(description.contentEquals(value))return view;
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
