package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lecture/Etude. This is the only Hifz screen that exposes Tafsir. */
public final class StudyReaderActivity extends android.app.Activity implements MushafView.Listener {
    private static final float TAFSIR_MIN_SP = 16f;
    private static final float TAFSIR_MAX_SP = 26f;
    private static final float TAFSIR_DEFAULT_SP = 18f;
    private static final String TAFSIR_PREFS = "hifz_tafsir_reading";
    private static final String TAFSIR_FONT_KEY = "commentary_font_size_sp";

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
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(overlayWidth(720), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) pageLabel.setText("Lecture / Étude · " + (progress + 1) + " / 604");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { showControls(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { setPage(seekBar.getProgress() + 1); }
        });
        bottomControls.addView(pageSeek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(overlayWidth(720), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bottomLp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 6));
        root.addView(bottomControls, bottomLp);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        scheduleAutoHide();
    }

    private int overlayWidth(int maxDp) {
        int screen = getResources().getDisplayMetrics().widthPixels;
        return Math.max(Ui.dp(this, 280), Math.min(screen - Ui.dp(this, 16), Ui.dp(this, maxDp)));
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
        shell.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,10));

        TextView title = Ui.text(this, TafsirRepository.EDITION_NAME + " · " + verse.getSurah() + ":" + verse.getAyah(), 17, true);
        shell.addView(title);
        TextView source = Ui.text(this, TafsirRepository.SOURCE_TITLE, 12, false);
        source.setTextColor(Ui.MUTED);
        source.setPadding(0,0,0,Ui.dp(this,4));
        shell.addView(source);

        SharedPreferences readingPrefs = getSharedPreferences(TAFSIR_PREFS, MODE_PRIVATE);
        final float[] fontSize = {Math.max(TAFSIR_MIN_SP, Math.min(TAFSIR_MAX_SP, readingPrefs.getFloat(TAFSIR_FONT_KEY, TAFSIR_DEFAULT_SP)))};
        final TafsirRepository.Entry[] loaded = {null};
        final LinearLayout textColumn = Ui.column(this);
        textColumn.setPadding(0,0,0,0);

        LinearLayout controls = Ui.row(this);
        Button minus = Ui.smallButton(this, "A−", v -> {
            fontSize[0] = Math.max(TAFSIR_MIN_SP, fontSize[0] - 2f);
            readingPrefs.edit().putFloat(TAFSIR_FONT_KEY, fontSize[0]).apply();
            if (loaded[0] != null) renderTafsir(textColumn, loaded[0], fontSize[0]);
        });
        Button plus = Ui.smallButton(this, "A+", v -> {
            fontSize[0] = Math.min(TAFSIR_MAX_SP, fontSize[0] + 2f);
            readingPrefs.edit().putFloat(TAFSIR_FONT_KEY, fontSize[0]).apply();
            if (loaded[0] != null) renderTafsir(textColumn, loaded[0], fontSize[0]);
        });
        Button close = Ui.smallButton(this, "Fermer", v -> dialog.dismiss());
        controls.addView(minus); controls.addView(plus); controls.addView(close); shell.addView(controls);

        ScrollView scroll = new ScrollView(this);
        TextView loading = tafsirText("Chargement…", fontSize[0], false);
        textColumn.addView(loading);
        scroll.addView(textColumn);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dialog.setContentView(shell); dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> hideControls());
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            w.setWindowAnimations(0);
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            View content = findViewById(android.R.id.content);
            int availableHeight = content == null ? getResources().getDisplayMetrics().heightPixels : content.getHeight();
            int panelWidth = overlayWidth(760);
            w.setLayout(panelWidth, Math.max(Ui.dp(this,220), Math.round(availableHeight * .50f)));
        }
        io.execute(() -> {
            try {
                TafsirRepository.Entry entry = new TafsirRepository(this).load(verse);
                runOnUiThread(() -> {
                    textColumn.removeAllViews();
                    if (entry == null) {
                        textColumn.addView(tafsirText("Aucun commentaire vérifié n’est disponible pour ce verset.", fontSize[0], false));
                    } else {
                        loaded[0] = entry;
                        renderTafsir(textColumn, entry, fontSize[0]);
                    }
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    textColumn.removeAllViews();
                    textColumn.addView(tafsirText("Tafsir indisponible : " + error.getMessage(), fontSize[0], false));
                });
            }
        });
    }

    private void renderTafsir(LinearLayout target, TafsirRepository.Entry entry, float fontSp) {
        target.removeAllViews();
        addRunBlocks(target, entry.commentaryRuns, fontSp, false);
        if (!entry.notes.isEmpty()) {
            Button notesToggle = Ui.smallButton(this, "Notes (" + entry.notes.size() + ")", null);
            LinearLayout notes = Ui.column(this); notes.setPadding(0,0,0,0); notes.setVisibility(View.GONE);
            notesToggle.setOnClickListener(v -> notes.setVisibility(notes.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            target.addView(notesToggle);
            for (TafsirRepository.Note note : entry.notes) {
                TextView label = tafsirText(Integer.toString(note.number) + ".", fontSp, true);
                label.setPadding(0,Ui.dp(this,6),0,0); notes.addView(label);
                addRunBlocks(notes, note.runs, Math.max(TAFSIR_MIN_SP, fontSp - 1f), true);
            }
            target.addView(notes);
        }
    }

    private void addRunBlocks(LinearLayout target, List<TafsirRepository.Run> runs, float fontSp, boolean note) {
        ArrayList<TafsirRepository.Run> block = new ArrayList<>();
        Boolean poetry = null;
        for (TafsirRepository.Run run : runs) {
            boolean isPoetry = run.style == TafsirRepository.RunStyle.POETRY;
            if (poetry != null && poetry != isPoetry) {
                addRunBlock(target, block, fontSp, note, poetry);
                block.clear();
            }
            poetry = isPoetry;
            block.add(run);
        }
        if (!block.isEmpty()) addRunBlock(target, block, fontSp, note, Boolean.TRUE.equals(poetry));
    }

    private void addRunBlock(LinearLayout target, List<TafsirRepository.Run> runs, float fontSp, boolean note, boolean poetry) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (TafsirRepository.Run run : runs) {
            String value = poetry ? run.text : run.text.replaceAll("\\n{2,}", "\n");
            int start = text.length();
            text.append(value);
            int end = text.length();
            if (end <= start) continue;
            switch (run.style) {
                case ITALIC:
                case TRANSLITERATION:
                case POETRY:
                    text.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); break;
                case BOLD:
                case TECHNICAL_TERM:
                    text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); break;
                case BOLD_ITALIC:
                    text.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); break;
                case NOTE_REF:
                    text.setSpan(new SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setSpan(new RelativeSizeSpan(.78f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); break;
                case REGULAR:
                default: break;
            }
        }
        TextView view = tafsirText(text, fontSp, false);
        view.setLineSpacing(0f, poetry ? 1.55f : (note ? 1.45f : 1.50f));
        if (poetry) {
            view.setGravity(Gravity.START);
            view.setPadding(Ui.dp(this,12),0,Ui.dp(this,4),Ui.dp(this,4));
        } else if (Build.VERSION.SDK_INT >= 26) {
            view.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        target.addView(view);
    }

    private TextView tafsirText(CharSequence value, float sp, boolean bold) {
        TextView text = Ui.text(this, value.toString(), sp, bold);
        text.setText(value);
        text.setTextIsSelectable(false);
        return text;
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
