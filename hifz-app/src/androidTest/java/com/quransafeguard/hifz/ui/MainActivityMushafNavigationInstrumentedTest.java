package com.quransafeguard.hifz.ui;

import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;

import com.quransafeguard.hifz.R;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** End-to-end local-reader smoke gate: native page 1 -> page 2 -> page 1. */
public final class MainActivityMushafNavigationInstrumentedTest {
    private static final long TIMEOUT_MS = 10_000L;

    @Test
    public void page001To002AndBackTo001() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitForLabel(scenario, "Page 1 / 604");

            scenario.onActivity(activity -> {
                Button next = activity.findViewById(R.id.next_page);
                assertTrue("Page 2 must be locally bundled in smoke mode", next.isEnabled());
                next.performClick();
            });
            waitForLabel(scenario, "Page 2 / 604");

            scenario.onActivity(activity -> {
                Button previous = activity.findViewById(R.id.previous_page);
                assertTrue("Page 1 must remain locally available", previous.isEnabled());
                previous.performClick();
            });
            waitForLabel(scenario, "Page 1 / 604");
        }
    }

    private static void waitForLabel(ActivityScenario<MainActivity> scenario, String expected)
        throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
        AtomicReference<String> actual = new AtomicReference<>("");

        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                TextView label = activity.findViewById(R.id.page_label);
                actual.set(label.getText().toString());
            });
            if (expected.equals(actual.get())) return;
            Thread.sleep(50L);
        }

        assertEquals("Timed out waiting for native Mushaf navigation", expected, actual.get());
    }
}
