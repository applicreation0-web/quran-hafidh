package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

/** Uses a locale-specific context without mutating process defaults or persisted engine state. */
public abstract class HaafidhActivity extends Activity {
    private static final String PREFERENCES = "haafidh_display";
    private static final String LANGUAGE = "language";

    static final String[] LANGUAGE_LABELS = {"Français", "English", "Wolof"};
    private static final String[] LANGUAGE_TAGS = {"fr", "en", "wo"};

    static int languageIndex(Context context) {
        String tag = context.getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(LANGUAGE, "fr");
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) if (LANGUAGE_TAGS[i].equals(tag)) return i;
        return 0;
    }

    static boolean setLanguage(Context context, int index) {
        if (index < 0 || index >= LANGUAGE_TAGS.length) throw new IllegalArgumentException("Invalid language");
        return context.getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putString(LANGUAGE, LANGUAGE_TAGS[index]).commit();
    }

    private int displayedLanguage;

    @Override protected void attachBaseContext(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        displayedLanguage = languageIndex(base);
        config.setLocale(Locale.forLanguageTag(LANGUAGE_TAGS[displayedLanguage]));
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override protected void onResume() {
        super.onResume();
        if (displayedLanguage != languageIndex(this)) recreate();
    }

    final android.app.AlertDialog chooseLanguage() {
        return new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.app_language)
            .setSingleChoiceItems(LANGUAGE_LABELS, languageIndex(this), (dialog, index) -> {
                dialog.dismiss();
                if (index == languageIndex(this)) return;
                if (setLanguage(this, index)) recreate();
                else android.widget.Toast.makeText(this, R.string.setup_save_error, android.widget.Toast.LENGTH_LONG).show();
            })
            .setNegativeButton(R.string.ui_close, null)
            .show();
    }
}
