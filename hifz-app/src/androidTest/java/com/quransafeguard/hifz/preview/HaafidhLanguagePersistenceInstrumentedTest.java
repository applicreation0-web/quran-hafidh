package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.res.Configuration;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Locale;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class HaafidhLanguagePersistenceInstrumentedTest {
    @Test public void newUserRetainsDraftThroughRecreationAndOnlyDeclaresAcquisOnConfirm() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        new HifzPrefs(context);
        JSONObject original = HifzBackup.export(context);
        int originalLanguage = HaafidhActivity.languageIndex(context);
        try {
            assertTrue(context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).edit().clear().commit());
            assertTrue(HaafidhActivity.setLanguage(context,0));
            try (ActivityScenario<MainActivity> user = ActivityScenario.launch(MainActivity.class)) {
                user.onActivity(activity -> {
                    var prefs = new HifzPrefs(activity);
                    assertTrue(prefs.universalSetupPending());
                    assertTrue(prefs.itqanRanges().isEmpty());
                    assertTrue(activity.findViewById(R.id.setup_next).performClick());
                    EditText declared=activity.findViewById(R.id.setup_declared);
                    android.view.ViewGroup panel=(android.view.ViewGroup)declared.getParent();
                    assertTrue(panel.getChildAt(panel.indexOfChild(declared)+1).performClick());
                    android.app.AlertDialog picker=(android.app.AlertDialog)HifzAndMapRuntimeSmokeInstrumentedTest.reflect(activity,"setupRangePicker");
                    picker.<android.widget.NumberPicker>findViewById(R.id.setup_range_last_ayah).setValue(7);
                    assertTrue(picker.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());
                    assertEquals("1:1-1:7",declared.getText().toString());
                    assertTrue(activity.findViewById(R.id.setup_next).performClick());
                    activity.<EditText>findViewById(R.id.setup_learning).setText("114:1-114:6");
                    assertTrue(prefs.itqanRanges().isEmpty());
                });
                assertTrue(HaafidhActivity.setLanguage(context,1));
                user.recreate();
                user.onActivity(activity -> {
                    assertEquals("en",activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
                    assertEquals("114:1-114:6",activity.<EditText>findViewById(R.id.setup_learning).getText().toString());
                    assertEquals("1:1-1:7",activity.<EditText>findViewById(R.id.setup_declared).getText().toString());
                    for (int step = 0; step < 2; step++)
                        assertTrue(activity.findViewById(R.id.setup_next).performClick());
                    activity.<android.widget.Spinner>findViewById(R.id.setup_volume).setSelection(1);
                    assertTrue(activity.findViewById(R.id.setup_next).performClick());
                    assertTrue(activity.findViewById(R.id.setup_confirm).isShown());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("setup-summary-en.png");
                user.onActivity(activity -> assertTrue(activity.findViewById(R.id.setup_confirm).performClick()));
                var prefs = new HifzPrefs(context);
                assertFalse(prefs.universalSetupPending());
                assertEquals(2,prefs.learningLinesPerSession());
                assertEquals(1,prefs.itqanRanges().size());
                assertEquals(1,prefs.configuredUniversalLearningRanges().size());
                assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
                assertTrue(prefs.progressionSnapshotV6().learned.isEmpty());
            }
        } finally {
            HifzBackup.restore(context,original);
            assertTrue(HaafidhActivity.setLanguage(context,originalLanguage));
        }
    }

    @Test public void roundTripsAllLanguagePairsAndKeepsAllBackupStores() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int originalLanguage = HaafidhActivity.languageIndex(context);
        JSONObject before = HifzBackup.export(context);
        before.remove("exportedAtEpochMs");
        int[] cycle = {0, 1, 2, 0, 2, 1};
        String[] tags = {"fr", "en", "wo"};
        String[] labels = {"Masquer la page", "Hide the page", "Nëbb xët wi"};
        try {
            for (int i = 0; i < 120; i++) {
                int language = cycle[i % cycle.length];
                assertTrue(HaafidhActivity.setLanguage(context, language));
                assertEquals(language, HaafidhActivity.languageIndex(context));
                Configuration config = new Configuration(context.getResources().getConfiguration());
                config.setLocale(Locale.forLanguageTag(tags[language]));
                Context display = context.createConfigurationContext(config);
                assertEquals(labels[language], display.getString(R.string.fragility_hide));
                JSONObject after = HifzBackup.export(context);
                after.remove("exportedAtEpochMs");
                assertEquals("Hifz backup changed while switching language", before.toString(), after.toString());
            }
        } finally {
            assertTrue(HaafidhActivity.setLanguage(context, originalLanguage));
        }
    }
}
