package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Etude. This is the only preview screen that exposes Tafsir. */
public final class StudyReaderActivity extends android.app.Activity implements MushafView.Listener {
    private MushafView mushaf;
    private GeometryRepository geometry;
    private int page = 1;
    private VerseRef selected;
    private TextView pageLabel;
    private Button tafsirButton;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        geometry = GeometryRepository.get(this);
        page = getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);

        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        Button back = Ui.smallButton(this, "‹", v -> finish());
        pageLabel = Ui.text(this, "Lecture / Étude · " + page + " / 604", 15, true);
        Ui.weight(pageLabel, 1f); pageLabel.setGravity(Gravity.CENTER);
        top.addView(back); top.addView(pageLabel);
        root.addView(top);

        mushaf = new MushafView(this); mushaf.setListener(this);
        root.addView(mushaf, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = Ui.row(this);
        bottom.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8));
        Button prev = Ui.smallButton(this, "‹ Page", v -> go(-1));
        tafsirButton = Ui.smallButton(this, "Tafsir", v -> openTafsir());
        tafsirButton.setEnabled(false);
        Button next = Ui.smallButton(this, "Page ›", v -> go(1));
        Ui.weight(prev, 1f); Ui.weight(tafsirButton, 1f); Ui.weight(next, 1f);
        bottom.addView(prev); bottom.addView(tafsirButton); bottom.addView(next);
        root.addView(bottom);
        setContentView(root);
    }

    private void go(int delta) {
        int next = Math.max(1, Math.min(604, page + delta));
        if (next == page) return;
        page = next; selected = null; tafsirButton.setEnabled(false);
        getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        pageLabel.setText("Lecture / Étude · " + page + " / 604");
        mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
    }

    @Override public void onVerseTap(VerseRef verse) {
        selected = verse; tafsirButton.setEnabled(true);
        mushaf.setSelection(Collections.singletonList(verse), geometry.lineIdsForVerseRange(verse, verse));
    }

    private void openTafsir() {
        final VerseRef verse = selected;
        if (verse == null) return;
        final Dialog dialog = new Dialog(this);
        LinearLayout shell = Ui.column(this);
        TextView title = Ui.text(this, "Sourate " + verse.getSurah() + " · verset " + verse.getAyah(), 18, true);
        shell.addView(title);
        LinearLayout controls = Ui.row(this);
        TextView body = Ui.text(this, "Chargement…", 16, false); body.setTextIsSelectable(false);
        Button minus = Ui.smallButton(this, "A−", v -> body.setTextSize(Math.max(13f, body.getTextSize() / getResources().getDisplayMetrics().scaledDensity - 1f)));
        Button plus = Ui.smallButton(this, "A+", v -> body.setTextSize(Math.min(30f, body.getTextSize() / getResources().getDisplayMetrics().scaledDensity + 1f)));
        Button close = Ui.smallButton(this, "Fermer", v -> dialog.dismiss());
        controls.addView(minus); controls.addView(plus); controls.addView(close); shell.addView(controls);
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        dialog.setContentView(shell); dialog.setCanceledOnTouchOutside(true); dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(getResources().getDisplayMetrics().heightPixels * .50f));
        }
        io.execute(() -> {
            try {
                TafsirRepository.Entry entry = new TafsirRepository(this).load(verse);
                String text;
                if (entry == null) text = "Aucun commentaire disponible pour ce verset.";
                else {
                    StringBuilder b = new StringBuilder(entry.commentary);
                    for (String note : entry.notes) b.append("\n\n").append(note);
                    text = b.toString();
                }
                final String finalText = text;
                runOnUiThread(() -> body.setText(finalText));
            } catch (Throwable error) {
                runOnUiThread(() -> body.setText("Tafsir indisponible : " + error.getMessage()));
            }
        });
    }

    @Override public void onReady() { mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0); }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onPageShown(int shown) { pageLabel.setText("Lecture / Étude · " + shown + " / 604"); }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { go(1); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() { io.shutdownNow(); if (mushaf != null) mushaf.destroySafely(); super.onDestroy(); }
}
