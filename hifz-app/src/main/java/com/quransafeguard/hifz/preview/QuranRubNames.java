package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;

import java.util.function.IntConsumer;

/**
 * Shared "jump to a rub' al-hizb" picker, for every screen that navigates the
 * Mushaf by page — alongside QuranSurahNames.showPicker, never replacing it.
 * A flat list of the 240 quarters would be unwieldy to scroll, so this is a
 * two-step picker instead: first the Hizb (1-60, a manageable list, each row
 * naming the surah/verse it starts at like the surah picker does), then which
 * of its 4 quarters — a much shorter, more ergonomic second step.
 */
final class QuranRubNames {
    private QuranRubNames() {}

    private static final String[] POSITION_LABEL = {"Début du Hizb", "¼", "½", "¾"};

    /** Which rub' the given page currently sits in, for a status label — e.g. "Hizb 2 · ¼". */
    static String currentLabel(int page) {
        int[] row = QuranRubBoundaries.currentAt(page);
        return "Hizb " + row[4] + " · " + POSITION_LABEL[row[5]];
    }

    static void showPicker(Activity activity, IntConsumer onPageChosen) {
        String[] items = new String[60];
        for (int hizb = 1; hizb <= 60; hizb++) {
            int[] row = QuranRubBoundaries.rowForIndex((hizb - 1) * 4 + 1);
            items[hizb - 1] = "Hizb " + hizb + " · " + QuranSurahNames.name(row[1]) + " " + row[1] + ":" + row[2];
        }
        new AlertDialog.Builder(activity)
            .setTitle("Aller à un Hizb")
            .setItems(items, (dialog, which) -> showQuarterPicker(activity, which + 1, onPageChosen))
            .show();
    }

    private static void showQuarterPicker(Activity activity, int hizb, IntConsumer onPageChosen) {
        String[] items = new String[4];
        for (int position = 0; position < 4; position++) {
            int[] row = QuranRubBoundaries.rowForIndex((hizb - 1) * 4 + position + 1);
            items[position] = POSITION_LABEL[position] + " · " + row[1] + ":" + row[2];
        }
        new AlertDialog.Builder(activity)
            .setTitle("Hizb " + hizb + " · quel repère ?")
            .setItems(items, (dialog, which) -> onPageChosen.accept(QuranRubBoundaries.pageForRub(hizb, which)))
            .show();
    }
}
