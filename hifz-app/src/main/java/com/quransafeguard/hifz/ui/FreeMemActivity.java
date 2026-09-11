package com.quransafeguard.hifz.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.LineGeometryRepository;
import com.quransafeguard.hifz.reader.ReaderSurface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Free, punctual memorization. It never reads or changes structured Hifz cursors. */
public final class FreeMemActivity extends android.app.Activity implements ReaderSurface.Listener {
    private static final String PREFS = "quran_hifz_free_mem_v1";

    private ReaderSurface surface;
    private LineGeometryRepository lines;
    private SharedPreferences prefs;
    private TextView title, selection, counter;
    private int page, count, mask;
    private VerseRef start, end;
    private GeometryRepository.AyahRegion focus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        lines = LineGeometryRepository.get(this);
        page = clampPage(prefs.getInt("page", 1));
        count = Math.max(0, prefs.getInt("count", 0));
        mask = clampMask(prefs.getInt("mask", 0));
        start = parseNullable(prefs.getString("start", null));
        end = parseNullable(prefs.getString("end", null));

        LinearLayout root = Ui.column(this); root.setPadding(0, 0, 0, 0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,8), Ui.dp(this,4), Ui.dp(this,8), Ui.dp(this,4));
        top.addView(Ui.smallButton(this, "‹", v -> finish()));
        title = Ui.text(this, "Mémorisation libre", 15, true); title.setGravity(Gravity.CENTER); Ui.weight(title,1f); top.addView(title); root.addView(top);

        selection = Ui.text(this, "Touchez un verset pour choisir le passage.", 14, false);
        selection.setPadding(Ui.dp(this,12), 2, Ui.dp(this,12), 2); root.addView(selection);

        surface = new ReaderSurface(this); surface.setListener(this);
        root.addView(surface, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        counter = Ui.text(this, "Répétitions : " + count, 15, true); counter.setGravity(Gravity.CENTER); root.addView(counter);
        LinearLayout reps = Ui.row(this);
        Button minus = Ui.smallButton(this, "−1", v -> { if (count > 0) count--; save(); refreshCounter(); });
        Button plus = Ui.smallButton(this, "+1", v -> { count++; save(); refreshCounter(); });
        Button reset = Ui.smallButton(this, "Remise à 0", v -> { count = 0; save(); refreshCounter(); });
        Ui.weight(minus,1); Ui.weight(plus,1); Ui.weight(reset,1); reps.addView(minus); reps.addView(plus); reps.addView(reset); root.addView(reps);

        LinearLayout masks = Ui.row(this);
        for (int value : new int[]{0,25,50,75,100}) {
            Button b = Ui.smallButton(this, value + "%", v -> { mask = value; save(); refreshSelection(); });
            Ui.weight(b,1); masks.addView(b);
        }
        root.addView(masks);

        LinearLayout nav = Ui.row(this);
        Button previous = Ui.smallButton(this, "‹ Page", v -> go(-1));
        Button reveal = Ui.smallButton(this, "Afficher brièvement", null);
        reveal.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) surface.revealTemporarily(true);
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) surface.revealTemporarily(false);
            return true;
        });
        Button next = Ui.smallButton(this, "Page ›", v -> go(+1));
        Ui.weight(previous,1); Ui.weight(reveal,1); Ui.weight(next,1); nav.addView(previous); nav.addView(reveal); nav.addView(next); root.addView(nav);

        setContentView(root);
        surface.showPage(page);
    }

    private void go(int delta) {
        int next = clampPage(page + delta);
        if (next == page) return;
        page = next; start = end = null; focus = null; count = 0;
        save(); surface.clearMemorizationState(); surface.showPage(page); updateSelectionLabel();
    }

    @Override public void onPageChanged(int shown) {
        page = shown; save(); title.setText("Mémorisation libre · " + shown + " / 604");
        refreshSelection();
    }

    @Override public void onVerseTapped(GeometryRepository.AyahRegion region) {
        VerseRef verse = new VerseRef(region.surah, region.ayah);
        focus = region;
        if (start == null) {
            start = end = verse;
        } else if (start.equals(end)) {
            if (ordinal(verse) < ordinal(start)) { end = start; start = verse; } else end = verse;
        } else {
            start = end = verse; count = 0; refreshCounter();
        }
        save(); refreshSelection();
    }

    private void refreshSelection() {
        if (surface == null) return;
        if (start == null || end == null) { surface.clearMemorizationState(); updateSelectionLabel(); return; }
        try {
            int low = Math.min(ordinal(start), ordinal(end));
            int high = Math.max(ordinal(start), ordinal(end));
            List<LineGeometryRepository.PageLine> selected = new ArrayList<>();
            for (LineGeometryRepository.PageLine line : lines.linesForPage(page)) {
                for (VerseRef verse : line.verses) {
                    int o = ordinal(verse);
                    if (o >= low && o <= high) { selected.add(line); break; }
                }
            }
            surface.setMemorizationState(selected, focus, mask);
            updateSelectionLabel();
        } catch (Throwable error) {
            onError(error);
        }
    }

    private void updateSelectionLabel() {
        if (start == null || end == null) selection.setText("Touchez un verset pour choisir le passage.");
        else selection.setText("Passage : " + start + " → " + end + " · masque " + mask + "%");
    }

    private void refreshCounter() { counter.setText("Répétitions : " + count); }

    private void save() {
        prefs.edit().putInt("page", page).putInt("count", count).putInt("mask", mask)
            .putString("start", start == null ? null : start.toString())
            .putString("end", end == null ? null : end.toString()).apply();
    }

    @Override public void onPageSwipe(int delta) { go(delta); }
    @Override public void onError(Throwable error) { Toast.makeText(this, "Mémorisation : " + error.getMessage(), Toast.LENGTH_LONG).show(); }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_PAGE_UP) { go(-1); return true; }
        if (code == KeyEvent.KEYCODE_PAGE_DOWN) { go(+1); return true; }
        return super.onKeyDown(code, event);
    }

    @Override protected void onDestroy() { if (surface != null) surface.close(); super.onDestroy(); }

    private static int ordinal(VerseRef ref) { return QuranCanon.INSTANCE.ordinal(ref); }
    private static int clampPage(int value) { return Math.max(1, Math.min(604, value)); }
    private static int clampMask(int value) { return Math.max(0, Math.min(100, value)); }
    private static VerseRef parseNullable(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 2) return null;
            return new VerseRef(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (RuntimeException invalid) { return null; }
    }
}
