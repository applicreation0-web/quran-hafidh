package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Deterministic crash-boundary recovery contract for the schema 5 -> 6 migration. */
public final class HifzV6MigrationRecoveryInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private SharedPreferences legacyJ10;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        legacyJ10 = context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE);
        clearFaultIfPresent();
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
    }

    @After public void tearDown() {
        clearFaultIfPresent();
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
    }

    @Test public void interruptionBeforeMainCommitLeavesSchemaFiveReplayableAndLegacyUntouched() throws Exception {
        seedSchemaFive();
        long exactDate = LocalDate.of(2026, 9, 4).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:2:0", exactDate).commit());
        Map<String, ?> mainBefore = snapshot(main);
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        setFaultPoint("BEFORE_MAIN_COMMIT");
        expectInjectedInterruption("BEFORE_MAIN_COMMIT");
        clearFaultPoint();

        assertEquals("pre-commit interruption must leave schema 5 authoritative", 5, main.getInt("schema", -1));
        assertEquals("pre-commit interruption must not partially mutate main prefs", mainBefore, snapshot(main));
        assertEquals("pre-commit interruption must not mutate retained J10", legacyBefore, snapshot(legacyJ10));

        HifzPrefs recovered = new HifzPrefs(context);
        assertEquals(6, recovered.schema());
        assertEquals(legacyBefore, snapshot(legacyJ10));
        assertTrue(main.contains("v6AcquiredCreditLineIds"));
        assertTrue(main.contains("v6ActiveJ10LastReviewed"));
    }

    @Test public void interruptionAfterMainCommitLeavesSchemaSixAuthoritativeAndReopenIsIdempotent() throws Exception {
        seedSchemaFive();
        long exactDate = LocalDate.of(2026, 9, 5).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:2:0", exactDate).commit());
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        setFaultPoint("AFTER_MAIN_COMMIT");
        expectInjectedInterruption("AFTER_MAIN_COMMIT");
        clearFaultPoint();

        assertEquals("post-commit interruption must leave schema 6 authoritative", 6, main.getInt("schema", -1));
        assertTrue(main.contains("v6AcquiredCreditLineIds"));
        assertTrue(main.contains("v6ActiveJ10LastReviewed"));
        assertEquals("post-commit interruption must not mutate retained J10", legacyBefore, snapshot(legacyJ10));

        Map<String, ?> mainAfterInterruptedCommit = snapshot(main);
        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(6, reopened.schema());
        assertEquals("schema 6 reopen must be idempotent", mainAfterInterruptedCommit, snapshot(main));
        assertEquals(legacyBefore, snapshot(legacyJ10));
    }

    private void expectInjectedInterruption(String point) {
        try {
            new HifzPrefs(context);
            fail("expected simulated process interruption at " + point);
        } catch (RuntimeException expected) {
            assertTrue("unexpected interruption: " + expected,
                expected.getMessage() != null && expected.getMessage().contains(point));
        }
    }

    private static void setFaultPoint(String point) throws Exception {
        Method method = HifzPrefs.class.getDeclaredMethod("setMigrationFaultPointForTest", String.class);
        method.setAccessible(true);
        try {
            method.invoke(null, point);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw error;
        }
    }

    private static void clearFaultPoint() throws Exception {
        setFaultPoint(null);
    }

    private static void clearFaultIfPresent() {
        try {
            setFaultPoint(null);
        } catch (Exception ignored) {
            // RED phase: the production seam intentionally does not exist yet.
        }
    }

    private void seedSchemaFive() {
        boolean ok = main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:74")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", "[]")
            .putString("unconsolidatedPromotedRanges", "[]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[]")
            .putInt("itqanBlockIndex", 0)
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "2:1")
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putString("recentSabqi", "[]")
            .putString("consolidationAttendanceDates", "[]")
            .putFloat("murajaahSecPerLine", 9.0f)
            .putBoolean("murajaahSpeedCalibrated", false)
            .putInt("murajaahSpeedSamples", 0)
            .putString("lastItqanCreditStart", "")
            .putString("lastItqanCreditEnd", "")
            .putInt("lastItqanCreditBlockIndex", -1)
            .putString("lastMurajaahCreditStart", "")
            .putString("lastMurajaahCreditEnd", "")
            .commit();
        assertTrue(ok);
    }

    private static Map<String, ?> snapshot(SharedPreferences prefs) {
        return new LinkedHashMap<>(prefs.getAll());
    }
}
