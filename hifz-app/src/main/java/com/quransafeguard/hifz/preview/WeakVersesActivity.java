package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Standing list of every verse currently flagged "difficile" during Révision active, with its
 * clean-recall progress toward the automatic 3-in-a-row clear. Tapping a verse opens Lecture at
 * its page with it highlighted; "Démarquer" removes the flag directly, the same effect as tapping
 * it again in the Mushaf while "Marquer" is armed.
 */
public final class WeakVersesActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private LinearLayout list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);
        try {
            geometry = GeometryRepository.get(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.column(this);
        scroll.addView(root);

        LinearLayout top = Ui.row(this);
        top.setPadding(0, 0, 0, Ui.dp(this, 2));
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Repères faibles", 18, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        top.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        root.addView(top);

        TextView subtitle = Ui.text(this,
            "Versets marqués difficiles en Révision active — s’effacent après "
                + HifzPrefs.WEAK_VERSE_CLEAN_STREAK_TO_CLEAR + " rappels propres.", 12.5f, false);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 14));
        root.addView(subtitle);

        list = Ui.column(this);
        list.setPadding(0, 0, 0, 0);
        root.addView(list);

        setContentView(scroll);
        Ui.respectSystemBars(this, scroll, 0, 0, 0, 0);
    }

    @Override protected void onResume() {
        super.onResume();
        if (geometry != null) refreshList();
    }

    private void refreshList() {
        list.removeAllViews();
        List<VerseRef> weak = new ArrayList<>(prefs.murajaahWeakVerses());
        Collections.sort(weak, Comparator.comparingInt(GeometryRepository::ordinal));
        if (weak.isEmpty()) {
            TextView empty = Ui.text(this, "Aucun repère faible pour l’instant.", 13f, false);
            empty.setTextColor(Ui.MUTED);
            empty.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            list.addView(empty);
            return;
        }
        for (VerseRef verse : weak) {
            list.addView(row(verse));
            list.addView(Ui.divider(this));
        }
    }

    private LinearLayout row(VerseRef verse) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));

        LinearLayout tapArea = Ui.column(this);
        tapArea.setPadding(0, 0, 0, 0);
        Ui.weight(tapArea, 1f);
        TextView label = Ui.text(this, QuranSurahNames.name(verse.getSurah()) + " " + verse.getAyah(), 14.5f, true);
        tapArea.addView(label);
        int streak = prefs.weakVerseStreak(verse);
        TextView progress = Ui.text(this,
            streak + "/" + HifzPrefs.WEAK_VERSE_CLEAN_STREAK_TO_CLEAR + " rappels propres", 12f, false);
        progress.setTextColor(Ui.MUTED);
        tapArea.addView(progress);
        tapArea.setClickable(true);
        tapArea.setFocusable(true);
        tapArea.setOnClickListener(v -> openInMushaf(verse));
        row.addView(tapArea);

        TextView unmark = Ui.text(this, "Démarquer", 12.5f, false);
        unmark.setTextColor(Ui.MUTED);
        unmark.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
        unmark.setClickable(true);
        unmark.setFocusable(true);
        unmark.setOnClickListener(v -> {
            prefs.toggleMurajaahWeakVerse(verse);
            refreshList();
        });
        row.addView(unmark);

        return row;
    }

    private void openInMushaf(VerseRef verse) {
        int page = geometry.pageForVerse(verse);
        Intent intent = new Intent(this, StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, verse.toString());
        startActivity(intent);
    }
}
