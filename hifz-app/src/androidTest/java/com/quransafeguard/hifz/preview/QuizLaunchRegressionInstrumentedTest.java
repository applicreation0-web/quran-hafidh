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
