package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.res.Configuration;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Locale;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
        String[] days = {
            "Lundi Mardi Mercredi Jeudi Vendredi Samedi Dimanche",
            "Monday Tuesday Wednesday Thursday Friday Saturday Sunday",
            "Altine Talaata Àllarba Alxames Àjjuma Gaawu Dibéer"
        };
        String[] months = {
            "janvier février mars avril mai juin juillet août septembre octobre novembre décembre",
            "January February March April May June July August September October November December",
            "Saŋwiyee Féewaryee Màrs Awril Me Suweŋ Sulet Ut Sàttumbar Oktoobar Nowàmbar Desàmbar"
        };
        try {
            for (int i = 0; i < 120; i++) {
                int language = cycle[i % cycle.length];
                assertTrue(HaafidhActivity.setLanguage(context, language));
                assertEquals(language, HaafidhActivity.languageIndex(context));
                Configuration config = new Configuration(context.getResources().getConfiguration());
                config.setLocale(Locale.forLanguageTag(tags[language]));
                Context display = context.createConfigurationContext(config);
                assertEquals(labels[language], display.getString(R.string.fragility_hide));
                String[] expectedDays = days[language].split(" ");
                for (DayOfWeek day : DayOfWeek.values())
                    assertEquals(expectedDays[day.getValue()-1], CalendarText.weekday(display,day,false));
                String[] expectedMonths = months[language].split(" ");
                for (int month = 1; month <= 12; month++)
                    assertEquals("1 "+expectedMonths[month-1]+" 2028", CalendarText.date(display,LocalDate.of(2028,month,1)));
                LocalDate leapDay = LocalDate.of(2028,2,29);
                assertEquals(expectedDays[leapDay.getDayOfWeek().getValue()-1]+" 29 "+expectedMonths[1]+" 2028",
                    CalendarText.dateWithWeekday(display,leapDay,true));
                assertEquals(6,display.getResources().getStringArray(R.array.setup_weekdays).length);
                assertEquals(7,display.getResources().getStringArray(R.array.calendar_weekdays_short).length);
                JSONObject after = HifzBackup.export(context);
                after.remove("exportedAtEpochMs");
                assertEquals("Hifz backup changed while switching language", before.toString(), after.toString());
            }
        } finally {
            assertTrue(HaafidhActivity.setLanguage(context, originalLanguage));
        }
    }
}
