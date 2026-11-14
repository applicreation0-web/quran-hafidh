package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CapHifzReadOnlyInstrumentedTest {
    private Context context;
    private SharedPreferences raw;
    private SharedPreferences ledger;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        raw = context.getSharedPreferences("quran_hifz_preview_v1", Context.MODE_PRIVATE);
        ledger = context.getSharedPreferences("quran_hifz_dashboard_ledger_v1", Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        assertTrue(ledger.edit().clear().commit());
        new HifzPrefs(context);
    }

    @After public void tearDown() {
        raw.edit().clear().commit();
        ledger.edit().clear().commit();
    }

    @Test public void simulationsAndEmptyHistoryLeaveAllFiveBackupStoresUntouched() throws Exception {
        exercise(false);
    }

    @Test public void maintenanceScreenKeepsTheRealAnNasLatch() throws Exception {
        assertTrue(raw.edit().putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, true).commit());
        exercise(true);
    }

    private void exercise(boolean postNas) throws Exception {
        org.json.JSONObject before = HifzBackup.export(context);
        try (ActivityScenario<CapHifzActivity> screen = ActivityScenario.launch(CapHifzActivity.class)) {
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                screen.onActivity(activity -> ready[0] = find(activity.getWindow().getDecorView(), "Simulations") != null);
                if (ready[0]) break;
                SystemClock.sleep(100);
            }
            screen.onActivity(activity -> {
                View decor = activity.getWindow().getDecorView();
                TextView chosen = find(decor, "90 %");
                assertNotNull("Expected the simulation controls", chosen);
                assertTrue(chosen.isSelected());
                assertEquals(postNas, find(decor, "Maintien après An-Nās") != null);
                assertTrue(find(decor, "100 %").performClick());
                assertTrue(find(decor, "100 %").isSelected());
                assertTrue(find(decor, "Historique des acquis ›").performClick());
            });
        }
        org.json.JSONObject after = HifzBackup.export(context);
        before.remove("exportedAtEpochMs");
        after.remove("exportedAtEpochMs");
        for (String store : new String[]{"values", "dashboard", "j10", "legacy_gates", "free_mem"}) {
            assertEquals(store, before.getJSONObject(store).toString(), after.getJSONObject(store).toString());
        }
    }

    private static TextView find(View view, String label) {
        if (view instanceof TextView && label.contentEquals(((TextView) view).getText())) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = find(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
