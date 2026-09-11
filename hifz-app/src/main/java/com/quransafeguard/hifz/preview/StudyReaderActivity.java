package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Etude. This is the only Hifz screen that exposes Tafsir. */
public final class StudyReaderActivity extends android.app.Activity implements MushafView.Listener {
    private MushafView mushaf;
    private int page = 1;
    private VerseRef selected;
    private TextView pageLabel;
    private Button tafsirButton;
    private LinearLayout topControls, bottomControls;
    private SeekBar pageSeek;
    private boolean controlsVisible = true;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable autoHide = this::hideControls;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        page = getSharedPreferences("hifz_study", MODE_PRIVATE).getInt("page", 1);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.PAPER);

        mushaf = new MushafView(this); mushaf.setListener(this);
        root.addView(mushaf, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        topControls = Ui.row(this);
        topControls.setBackgroundColor(Ui.PAPER);
        topControls.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        Button back = Ui.smallButton(this, "‹", v -> finish());
        pageLabel = Ui.text(this, "Lecture / Étude · " + page + " / 604", 15, true);
        Ui.weight(pageLabel, 1f); pageLabel.setGravity(Gravity.CENTER);
        topControls.addView(back); topControls.addView(pageLabel);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        topLp.setMargins(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), 0);
        root.addView(topControls, topLp);

        bottomControls = Ui.column(this);
        bottomControls.setBackgroundColor(Ui.PAPER);
        bottomControls.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 6));
        LinearLayout buttons = Ui.row(this);
        Button prev = Ui.smallButton(this, "‹ Page", v -> go(-1));
        tafsirButton = Ui.smallButton(this, "Tafsir", v -> openTafsir());
        tafsirButton.setEnabled(false);
        Button next = Ui.smallButton(this, "Page ›", v -> go(1));
        Ui.weight(prev, 1f); Ui.weight(tafsirButton, 1f); Ui.weight(next, 1f);
        buttons.addView(prev); buttons.addView(tafsirButton); buttons.addView(next);
        bottomControls.addView(buttons);

        pageSeek = new SeekBar(this);
        pageSeek.setMax(603);
        pageSeek.setProgress(page - 1);
        pageSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) { showControls(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { setPage(seekBar.getProgress() + 1); }
        });
        bottomControls.addView(pageSeek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        bottomLp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 6));
        root.addView(bottomControls, bottomLp);

        setContentView(root);
        scheduleAutoHide();
    }

    private void setPage(int requested) {
        int next = Math.max(1, Math.min(604, requested));
        if (next == page) { showControls(); return; }
        page = next; selected = null; tafsirButton.setEnabled(false);
        getSharedPreferences("hifz_study", MODE_PRIVATE).edit().putInt("page", page).apply();
        pageLabel.setText("Lecture / Étude · " + page + " / 604");
        pageSeek.setProgress(page - 1);
        mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);
        showControls();
    }

    private void go(int delta) { setPage(page + delta); }

    @Override public void onVerseTap(VerseRef verse) {
        selected = verse;
        tafsirButton.setEnabled(true);
        mushaf.setSelection(Collections.singletonList(verse), Collections.emptyList());
        showControls();
    }

    @Override public void onSurfaceTap() {
        if (controlsVisible) hideControls(); else showControls();
    }

    private void showControls() {
        controlsVisible = true;
        topControls.setVisibility(View.VISIBLE);
        bottomControls.setVisibility(View.VISIBLE);
        scheduleAutoHide();
    }

    private void hideControls() {
        if (topControls == null || bottomControls == null) return;
        controlsVisible = false;
        topControls.setVisibility(View.GONE);
        bottomControls.setVisibility(View.GONE);
        if (mushaf != null) mushaf.removeCallbacks(autoHide);
    }

    private void scheduleAutoHide() {
        if (mushaf == null) return;
        mushaf.removeCallbacks(autoHide);
        mushaf.postDelayed(autoHide, 3200L);
    }

    private void openTafsir() {
        final VerseRef verse = selected;
        if (verse == null) return;
        hideControls();
        final Dialog dialog = new Dialog(this);
        LinearLayout shell = Ui.column(this);
        shell.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));
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
        dialog.setContentView(shell); dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> hideControls());
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.BOTTOM);
            w.setWindowAnimations(0);
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            View content = findViewById(android.R.id.content);
            int availableHeight = content == null ? getResources().getDisplayMetrics().heightPixels : content.getHeight();
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(Ui.dp(this,220), Math.round(availableHeight * .50f)));
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
    @Override public void onPageShown(int shown) { page=shown; pageLabel.setText("Lecture / Étude · " + shown + " / 604"); pageSeek.setProgress(shown - 1); }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { go(1); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() { io.shutdownNow(); if (mushaf != null) { mushaf.removeCallbacks(autoHide); mushaf.destroySafely(); } super.onDestroy(); }
}
