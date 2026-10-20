package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Canonical Atlas entry point: units-only semantic map with the authentic KFQC Mushaf
 * and the existing Jalalayn commentary. Does not invoke legacy Ibn Kathir groups,
 * offline titles, or any Hifz progress/memorisation write.
 *
 * A build without the private, verified 114-surah catalogue fails closed instead of
 * reverting to the historical Ibn Kathir pilot/list.
 */
public final class AtlasMapActivity extends Activity {
    static final String EXTRA_SURAH = "mapSurah";
    static final String EXTRA_AYAH = "mapAyah";

    private MawdudiMapScreen atlas;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!BuildConfig.AYAH_THEMES_FINAL_PRIVATE) {
            Ui.showFatal(this, "Atlas nécessite son corpus privé vérifié des 114 sourates.");
            return;
        }
        try {
            GeometryRepository qcf = GeometryRepository.get(this);
            MawdudiPassageIndex canonical = MawdudiPassageIndex.load(this);
            atlas = new MawdudiMapScreen(this, qcf, canonical,
                AyahThemesCueIndex.load(this), MawdudiMapData.load(this, canonical),
                getIntent().getIntExtra(EXTRA_SURAH, 1),
                getIntent().getIntExtra(EXTRA_AYAH, 1), state);
            atlas.display();
        } catch (Exception unavailable) {
            if (atlas != null) { atlas.destroy(); atlas = null; }
            Ui.showFatal(this, "Le corpus privé vérifié d’Atlas est indisponible.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (atlas == null || requestCode != 11436 || resultCode != RESULT_OK || data == null) return;
        atlas.returnFromReader(data.getIntExtra("map114Surah", 0),
            data.getIntExtra("map114UnitOrdinal", 0),
            data.getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, 0),
            data.getStringExtra("map114ReturnPassageId"),
            data.getIntExtra("map114ReturnPassageStart", -1),
            data.getIntExtra("map114ReturnPassageEnd", -1));
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if (atlas != null) atlas.save(out);
        super.onSaveInstanceState(out);
    }

    @Override public void onBackPressed() {
        if (atlas != null && atlas.back()) return;
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (atlas != null) { atlas.destroy(); atlas = null; }
        super.onDestroy();
    }
}
