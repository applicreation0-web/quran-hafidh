package com.quransafeguard.hifz.ui;

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
import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.TafsirRepository;
import com.quransafeguard.hifz.reader.ReaderSurface;
import com.quransafeguard.hifz.storage.ReaderStateStore;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Étude. This is the only screen that exposes Tafsir. */
public final class StudyReaderActivity extends android.app.Activity implements ReaderSurface.Listener {
    private ReaderSurface surface;
    private ReaderStateStore stateStore;
    private TextView pageLabel;
    private Button tafsirButton;
    private GeometryRepository.AyahRegion selected;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quran-hifz-tafsir"); t.setDaemon(true); return t;
    });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        stateStore = new ReaderStateStore(this);

        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        top.addView(Ui.smallButton(this, "‹", v -> finish()));
        pageLabel = Ui.text(this, "Lecture / Étude", 15, true);
        pageLabel.setGravity(Gravity.CENTER);
        Ui.weight(pageLabel, 1f);
        top.addView(pageLabel);
        root.addView(top);

        surface = new ReaderSurface(this);
        surface.setListener(this);
        root.addView(surface, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = Ui.row(this);
        bottom.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8));
        Button previous = Ui.smallButton(this, "‹ Page", v -> go(-1));
        tafsirButton = Ui.smallButton(this, "Tafsir", v -> openTafsir());
        tafsirButton.setEnabled(false);
        Button next = Ui.smallButton(this, "Page ›", v -> go(+1));
        Ui.weight(previous, 1f); Ui.weight(tafsirButton, 1f); Ui.weight(next, 1f);
        bottom.addView(previous); bottom.addView(tafsirButton); bottom.addView(next);
        root.addView(bottom);

        setContentView(root);
        surface.showPage(stateStore.lastPage());
    }

    private void go(int delta) {
        int current = surface.getCurrentPage();
        int page = Math.max(1, Math.min(604, current + delta));
        if (page == current) return;
        selected = null;
        tafsirButton.setEnabled(false);
        surface.clearMemorizationState();
        surface.showPage(page);
    }

    @Override public void onPageChanged(int page) {
        stateStore.saveLastPage(page);
        pageLabel.setText("Lecture / Étude · " + page + " / 604");
    }

    @Override public void onVerseTapped(GeometryRepository.AyahRegion verse) {
        selected = verse;
        tafsirButton.setEnabled(true);
        surface.setMemorizationState(Collections.emptyList(), verse, 0);
    }

    @Override public void onPageSwipe(int delta) { go(delta); }

    @Override public void onError(Throwable error) {
        Toast.makeText(this, "Lecture indisponible : " + error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void openTafsir() {
        final GeometryRepository.AyahRegion region = selected;
        if (region == null) return;
        final VerseRef verse = new VerseRef(region.surah, region.ayah);
        final Dialog dialog = new Dialog(this);

        LinearLayout shell = Ui.column(this);
        shell.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        TextView title = Ui.text(this, "Sourate " + region.surah + " · verset " + region.ayah, 18, true);
        shell.addView(title);

        TextView body = Ui.text(this, "Chargement…", stateStore.tafsirTextSp(), false);
        body.setTextIsSelectable(false);
        LinearLayout controls = Ui.row(this);
        Button minus = Ui.smallButton(this, "A−", v -> changeTafsirSize(body, -1f));
        Button plus = Ui.smallButton(this, "A+", v -> changeTafsirSize(body, +1f));
        Button close = Ui.smallButton(this, "Fermer", v -> dialog.dismiss());
        controls.addView(minus); controls.addView(plus); controls.addView(close);
        shell.addView(controls);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(shell);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.50f));
        }

        io.execute(() -> {
            try {
                TafsirRepository.Entry entry = new TafsirRepository(this).load(verse);
                final String text;
                if (entry == null) {
                    text = "Aucun commentaire disponible pour ce verset.";
                } else {
                    StringBuilder b = new StringBuilder(entry.commentary);
                    for (String note : entry.notes) b.append("\n\n").append(note);
                    text = b.toString();
                }
                runOnUiThread(() -> body.setText(text));
            } catch (Throwable error) {
                runOnUiThread(() -> body.setText("Tafsir indisponible."));
            }
        });
    }

    private void changeTafsirSize(TextView body, float delta) {
        float current = body.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        float next = Math.max(14f, Math.min(30f, current + delta));
        body.setTextSize(next);
        stateStore.saveTafsirTextSp(next);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { go(+1); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        if (surface != null) surface.close();
        super.onDestroy();
    }
}
