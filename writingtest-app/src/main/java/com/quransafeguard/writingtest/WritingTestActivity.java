package com.quransafeguard.writingtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.ArabicHint;
import com.quransafeguard.hifz.core.TrajectoryComparison;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone test screen: navigate to ANY physical Mushaf line in the whole corpus (not just one
 * hardcoded line) and score a real trace against it. Two things this app tries that hifz-app's own
 * (already-shipped) writing exercise does not yet:
 *   1. Auto-detects how many words were actually written instead of requiring an exact manual
 *      chip selection — tries every word count starting from the chosen start word and keeps
 *      whichever length scores best.
 *   2. Line-to-line navigation across the whole 604-page corpus for systematic multi-line testing.
 * Once validated here, either can be folded back into hifz-app's real screen.
 */
public final class WritingTestActivity extends Activity {
    private SimpleGeometry geometry;
    private int lineIndex = 0;

    private List<List<double[]>> words;
    private int startWord = 0;
    private Button[] wordChipButtons;
    private VerseRef hintVerse;

    private TextView lineLabel;
    private TextView subtitle;
    private LinearLayout chipsContainer;
    private WritingCanvasView canvas;
    private TextView hintText;
    private TextView resultView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            geometry = SimpleGeometry.get(this);
        } catch (Throwable error) {
            TextView errorView = new TextView(this);
            errorView.setPadding(32, 32, 32, 32);
            errorView.setText("Géométrie du Mushaf indisponible : " + error);
            setContentView(errorView);
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        LinearLayout navRow = row();
        navRow.addView(button("◀", v -> moveLine(-1)));
        lineLabel = new TextView(this);
        lineLabel.setGravity(Gravity.CENTER);
        lineLabel.setTextSize(13f);
        weight(lineLabel, 1f);
        navRow.addView(lineLabel);
        navRow.addView(button("▶", v -> moveLine(1)));
        root.addView(navRow);

        subtitle = new TextView(this);
        subtitle.setTextSize(12f);
        subtitle.setPadding(0, dp(4), 0, dp(8));
        root.addView(subtitle);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsContainer = row();
        chipsScroll.addView(chipsContainer);
        LinearLayout.LayoutParams chipsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipsParams.bottomMargin = dp(4);
        root.addView(chipsScroll, chipsParams);

        TextView chipsHint = new TextView(this);
        chipsHint.setText("Mot de départ (droite → gauche). La longueur est détectée automatiquement.");
        chipsHint.setTextSize(11f);
        chipsHint.setPadding(0, 0, 0, dp(8));
        root.addView(chipsHint);

        canvas = new WritingCanvasView(this);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(420));
        canvasParams.bottomMargin = dp(8);
        root.addView(canvas, canvasParams);

        hintText = new TextView(this);
        hintText.setTextSize(24f);
        hintText.setGravity(Gravity.END);
        hintText.setVisibility(View.GONE);
        hintText.setPadding(0, dp(4), 0, dp(8));
        root.addView(hintText);

        LinearLayout buttonRow = row();
        buttonRow.addView(button("Effacer", v -> {
            canvas.clear();
            resultView.setText("");
        }), buttonWeight());
        buttonRow.addView(button("Indice", v -> revealHint()), buttonWeight());
        buttonRow.addView(button("Évaluer (auto)", v -> evaluateAuto()), buttonWeight());
        root.addView(buttonRow);

        resultView = new TextView(this);
        resultView.setTextSize(14f);
        resultView.setPadding(0, dp(10), 0, 0);
        root.addView(resultView);

        setContentView(scroll);
        loadLine();
    }

    private void moveLine(int delta) {
        int next = lineIndex + delta;
        if (next < 0 || next >= geometry.lineCount()) return;
        lineIndex = next;
        loadLine();
    }

    private void loadLine() {
        SimpleGeometry.LineMeta line = geometry.line(lineIndex);
        lineLabel.setText("Ligne " + (lineIndex + 1) + " / " + geometry.lineCount() + "  (id " + line.id + ")");

        StringBuilder verses = new StringBuilder();
        for (VerseRef ref : line.verses) {
            if (verses.length() > 0) verses.append(", ");
            verses.append(ref);
        }
        subtitle.setText("Page " + line.page + " — versets " + verses);

        float[] viewBox;
        float[][] markers;
        try {
            viewBox = geometry.viewBoxForPage(line.page);
            WordShapeRepository wordShapeRepo = new WordShapeRepository(this);
            AyahMarkerRepository markerRepo = new AyahMarkerRepository(this);
            words = LineWritingGeometry.wordsForLine(wordShapeRepo.shapesForPage(line.page)[line.lineIndexOnPage]);
            markers = LineWritingGeometry.markersWithinBand(markerRepo.markersForPage(line.page), line.top, line.bottom);
        } catch (Throwable error) {
            resultView.setText("Géométrie indisponible pour cette ligne : " + error);
            return;
        }

        hintVerse = line.verses.isEmpty() ? null : line.verses.get(0);
        startWord = 0;
        hintText.setVisibility(View.GONE);
        resultView.setText("");

        canvas.configure(viewBox[0], (float) line.top, viewBox[2], (float) (line.bottom - line.top), markers);

        chipsContainer.removeAllViews();
        wordChipButtons = new Button[words.size()];
        for (int i = 0; i < words.size(); i++) {
            int wordIdx = i;
            Button chip = button(String.valueOf(i + 1), v -> {
                startWord = wordIdx;
                refreshChipSelection();
            });
            wordChipButtons[i] = chip;
            chipsContainer.addView(chip);
        }
        refreshChipSelection();
    }

    private void refreshChipSelection() {
        for (int i = 0; i < wordChipButtons.length; i++) wordChipButtons[i].setSelected(i == startWord);
    }

    private void revealHint() {
        if (hintVerse == null) {
            hintText.setText("Aucun verset identifié pour cette ligne.");
            hintText.setVisibility(View.VISIBLE);
            return;
        }
        String text = VerseText.get(this).textFor(hintVerse);
        String hint = text == null ? null : ArabicHint.firstLettersWithTashkil(text, 3);
        hintText.setText(hint == null || hint.isEmpty() ? "Indice indisponible." : hint);
        hintText.setVisibility(View.VISIBLE);
    }

    /**
     * Tries every word count starting from the selected word (1 word, 2 words, ... to the end of
     * the line) against the user's actual trace, and keeps whichever length scores best — so the
     * tester never has to manually match a chip selection to how much they actually wrote.
     */
    private void evaluateAuto() {
        if (canvas.isEmpty()) {
            resultView.setText("Écris d’abord quelque chose avant d’évaluer.");
            return;
        }
        if (words == null || words.isEmpty() || startWord >= words.size()) {
            resultView.setText("Aucun mot de référence pour cette ligne.");
            return;
        }

        List<List<TrajectoryComparison.Pt>> userStrokes = canvas.strokesInPageSpace();
        int bestLength = -1;
        int bestScore = -1;
        List<double[]> reference = new ArrayList<>();
        for (int len = 1; startWord + len <= words.size(); len++) {
            reference.addAll(words.get(startWord + len - 1));
            Integer score = TrajectoryComparison.scoreStrokes(reference, userStrokes);
            if (score != null && score > bestScore) {
                bestScore = score;
                bestLength = len;
            }
        }

        if (bestLength < 0) {
            resultView.setText("Trajectoire (forme) : indisponible.");
            return;
        }
        int lastWord = startWord + bestLength;
        String range = bestLength == 1
            ? "mot " + (startWord + 1)
            : "mots " + (startWord + 1) + "-" + lastWord;
        resultView.setText("Meilleure correspondance : " + range + " — " + bestScore + " %");
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams buttonWeight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int gap = dp(3);
        params.setMargins(gap, gap, gap, gap);
        return params;
    }

    private void weight(View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        view.setLayoutParams(params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
