package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.res.Configuration;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Locale;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class HaafidhLanguagePersistenceInstrumentedTest {
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
