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

    @Test public void fixedNinetyPercentProjectionAndEmptyHistoryLeaveAllFiveBackupStoresUntouched() throws Exception {
        exercise(false);
    }

    @Test public void maintenanceScreenKeepsTheRealAnNasLatch() throws Exception {
        assertTrue(raw.edit().putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, true).commit());
        exercise(true);
    }

    private void exercise(boolean postNas) throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                screen.onActivity(activity -> ready[0] =
                    findContains(activity.getWindow().getDecorView(), "Prévision · 90 %") != null
                        && find(activity.getWindow().getDecorView(), "Prévision · 90 % d’assiduité") != null);
                if (ready[0]) break;
                SystemClock.sleep(100);
            }
            org.json.JSONObject before = HifzBackup.export(context);
            screen.onActivity(activity -> {
                View decor = activity.getWindow().getDecorView();
                assertNotNull(find(decor, "Cap Hifdh"));
                assertNotNull(findContains(decor, "Prévision · 90 %"));
                assertNull(find(decor, "Trajectoire"));
                assertNull(find(decor, "Historique"));
                assertNotNull(findGraph(decor));
                // A real post-Nās restore may have no remaining Acquisition
                // lesson configured. Do not invent a selectable future milestone.
                View initialCheckpoint = findDescription(decor,
                    "Ouvrir le Mushaf depuis Cap Hifdh :");
                if (!postNas) {
                    assertNotNull("A dated milestone is selected inline, not in a popup",
                        initialCheckpoint);
                }
                // Exercise the real Android buttons, not merely the source:
                // one step moves the selected QCF checkpoint, two chevrons
                // change the year without changing any backed-up Hifz store.
                View prevStep = findDescription(decor, "Jalon prévisionnel précédent");
                View nextStep = findDescription(decor, "Jalon prévisionnel suivant");
                View prevYear = findDescription(decor, "Prévision : année précédente");
                View nextYear = findDescription(decor, "Prévision : année suivante");
                assertNotNull(prevStep);
                assertNotNull(nextStep);
                assertNotNull(prevYear);
                assertNotNull(nextYear);
                TextView currentCheckpoint = (TextView) findDescription(
                    decor, "Ouvrir le Mushaf depuis Cap Hifdh :");
                if (currentCheckpoint != null && nextStep.isEnabled()) {
                    String beforeCheckpoint = currentCheckpoint.getText().toString();
                    assertTrue(nextStep.performClick());
                    TextView afterCheckpoint = (TextView) findDescription(
                        decor, "Ouvrir le Mushaf depuis Cap Hifdh :");
                    assertNotNull(afterCheckpoint);
                    assertNotEquals("Step arrow must actually change projection",
                        beforeCheckpoint, afterCheckpoint.getText().toString());
                }
                // The preceding click may have reconstructed the row.
                nextYear = findDescription(decor, "Prévision : année suivante");
                if (nextYear.isEnabled()) {
                    String beforeYear = String.valueOf(findGraph(decor).getContentDescription());
                    assertTrue(nextYear.performClick());
                    String afterYear = String.valueOf(findGraph(decor).getContentDescription());
                    assertNotEquals("Double chevron must change the forecast year",
                        beforeYear, afterYear);
                    View backYear = findDescription(decor, "Prévision : année précédente");
                    assertTrue(backYear.isEnabled());
                    assertTrue(backYear.performClick());
                }
                TextView history = find(decor, "Acquis ›");
                assertNotNull("Cap Hifdh history must remain visible on the home", history);
                assertTrue(history.performClick());
                assertNotNull("Acquired lines use the same fixed row, not a modal",
                    findContains(decor, "Acquis · revenir à la projection"));
                assertTrue(history.performClick());
                if (!postNas)
                    assertNotNull("Returning to the forecast must restore the QCF marker",
                        findDescription(decor, "Ouvrir le Mushaf depuis Cap Hifdh :"));
                assertNull(findContains(decor, "30 Juz acquis attestés"));
                TextView target = find(decor, "Prévision · 90 % d’assiduité");
                assertNotNull("A single non-interactive 90% target must be visible", target);
                assertFalse("The fixed projection is not a toggle", target.isClickable());
                assertNull(find(decor, "100 %"));
                assertNull(find(decor, "80 %"));
                assertNull(find(decor, "90 %"));
                CapHifzTimelineView graph = findGraph(decor);
                assertNotNull(graph);
                assertFalse("The Cap Hifdh second frise is removed after An-Nās; no Entretien ghost",
                    String.valueOf(graph.getContentDescription())
                        .contains("Entretien : cycle sans échéance"));
                assertTrue("Acquisition remains readable in either phase",
                    String.valueOf(graph.getContentDescription()).contains("Acquisition,"));
                assertNotNull(find(decor, "Acquis ›"));
                if (activity.getResources().getConfiguration().smallestScreenWidthDp >= 600) {
                    assertFalse(hasScrollView(decor));
                }
            });
            org.json.JSONObject after = HifzBackup.export(context);
            for (String store : new String[]{"values", "dashboard", "j10", "legacy_gates", "free_mem"}) {
                assertEquals(store, before.getJSONObject(store).toString(), after.getJSONObject(store).toString());
            }
        }
    }

    private static View findDescription(View view, String prefix) {
        CharSequence spoken = view.getContentDescription();
        if (spoken != null && spoken.toString().startsWith(prefix)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findDescription(group.getChildAt(i), prefix);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasScrollView(View view) {
        if (view instanceof android.widget.ScrollView) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasScrollView(group.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static CapHifzTimelineView findGraph(View view) {
        if (view instanceof CapHifzTimelineView) return (CapHifzTimelineView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                CapHifzTimelineView found = findGraph(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static TextView findContains(View view, String label) {
        if (view instanceof TextView &&
                ((TextView) view).getText().toString().contains(label)) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findContains(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
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
