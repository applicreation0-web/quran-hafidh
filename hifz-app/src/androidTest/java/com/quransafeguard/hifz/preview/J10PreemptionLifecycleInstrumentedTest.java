package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Behavioral lifecycle proof for C3: immediate close must never latch future preemption. */
public final class J10PreemptionLifecycleInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";
    private static final String HOST_PREFS = "quran_hifz_j10_host_v1";

    private Context context;
    private LocalDate today;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        today = HifzClock.today();
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(HOST_PREFS, Context.MODE_PRIVATE).edit().clear().commit();

        GeometryRepository geometry = GeometryRepository.get(context);
        List<String> due = geometry.lineIdsForVerseRange(new VerseRef(2, 1), new VerseRef(2, 1));
        assertTrue(new J10ReviewStore(context).acquireLines(due, today.minusDays(10)));
    }

    @After public void tearDown() {
        closeResumedJ10();
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(HOST_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void immediateJ10CloseDoesNotDisableTheNextPreemption() {
        Intent hostIntent = new Intent(context, HifzSessionActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.MURAJAAH);

        try (ActivityScenario<HifzSessionActivity> host = ActivityScenario.launch(hostIntent)) {
            J10ReviewActivity first = awaitResumedJ10();
            assertNotNull("first priority review must open", first);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(first::finish);
            awaitNoResumedJ10();

            // The automatic host resume consumes only the one-shot suppression. A later resume
            // must evaluate priority again and open a new J10 review for the still-due line.
            host.moveToState(Lifecycle.State.CREATED);
            host.moveToState(Lifecycle.State.RESUMED);

            J10ReviewActivity second = awaitResumedJ10();
            assertNotNull("a later host resume must preempt again", second);
            assertNotSame("the second preemption must be a new review Activity", first, second);
        }
    }

    private J10ReviewActivity awaitResumedJ10() {
        long deadline = SystemClock.elapsedRealtime() + 5_000L;
        while (SystemClock.elapsedRealtime() < deadline) {
            J10ReviewActivity current = resumedJ10();
            if (current != null) return current;
            SystemClock.sleep(25L);
        }
        return resumedJ10();
    }

    private void awaitNoResumedJ10() {
        long deadline = SystemClock.elapsedRealtime() + 3_000L;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (resumedJ10() == null) return;
            SystemClock.sleep(25L);
        }
        assertNull("the first J10 review must actually be closed", resumedJ10());
    }

    private void closeResumedJ10() {
        J10ReviewActivity current = resumedJ10();
        if (current != null) InstrumentationRegistry.getInstrumentation().runOnMainSync(current::finish);
    }

    private J10ReviewActivity resumedJ10() {
        AtomicReference<J10ReviewActivity> found = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED);
            for (Activity activity : resumed) {
                if (activity instanceof J10ReviewActivity) {
                    found.set((J10ReviewActivity) activity);
                    return;
                }
            }
        });
        return found.get();
    }
}
