package com.quransafeguard.writingtest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.TrajectoryComparison;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone test screen — deliberately NOT a memory exercise like hifz-app's real screen. This
 * one shows the real verse text up front (a reference panel to copy from, correctly or with a
 * chosen mistake) so a tester can systematically probe what the scoring engine does and doesn't
 * catch, across ANY physical Mushaf line in the whole corpus. Two things this app tries that
 * hifz-app's own (already-shipped) writing exercise does not yet:
 *   1. Auto-detects how many words were actually written instead of requiring an exact manual
 *      chip selection — tries every word count starting from the chosen start word and keeps
 *      whichever length scores best.
 *   2. Line-to-line navigation across the whole 604-page corpus for systematic multi-line testing.
 * Once validated here, either can be folded back into hifz-app's real screen.
 */
public final class WritingTestActivity extends Activity {
    private static final int PAPER = 0xfffaf8f0;
    private static final int SURFACE = 0xfffdfbf6;
    private static final int INK = 0xff121211;
    private static final int MUTED = 0xff6b6a66;
    private static final int LINE_COLOR = 0xffd9d3c6;
    private static final int ACCENT = 0xff2f5d4f;
    private static final int GOOD = 0xff2f7d4f;
    private static final int WARN = 0xffb2790a;
    private static final int BAD = 0xffb3312c;

    private SimpleGeometry geometry;
    private int lineIndex = 0;

    private List<List<double[]>> words;
    private int startWord = 0;
    private Button[] wordChipButtons;

    private TextView lineLabel;
    private TextView subtitle;
    private TextView referenceText;
    private LinearLayout chipsContainer;
    private WritingCanvasView canvas;
    private TextView resultView;
    private LinearLayout resultCard;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(true);

        try {
            geometry = SimpleGeometry.get(this);
        } catch (Throwable error) {
            TextView errorView = new TextView(this);
            errorView.setPadding(dp(24), dp(24), dp(24), dp(24));
            errorView.setText("Géométrie du Mushaf indisponible : " + error);
            setContentView(errorView);
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Exercice d’écriture — Test");
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(INK);
        title.setPadding(0, 0, 0, dp(14));
        root.addView(title);

        LinearLayout navCard = card();
        LinearLayout navRow = row();
        navRow.addView(navButton("‹", v -> moveLine(-1)));
        lineLabel = new TextView(this);
        lineLabel.setGravity(Gravity.CENTER);
        lineLabel.setTextSize(15f);
        lineLabel.setTypeface(Typeface.DEFAULT_BOLD);
        lineLabel.setTextColor(INK);
        weight(lineLabel, 1f);
        navRow.addView(lineLabel);
        navRow.addView(navButton("›", v -> moveLine(1)));
        navCard.addView(navRow);

        subtitle = new TextView(this);
        subtitle.setTextSize(13.5f);
        subtitle.setTextColor(MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(6), 0, 0);
        navCard.addView(subtitle);
        root.addView(navCard, cardParams());

        TextView referenceLabel = new TextView(this);
        referenceLabel.setText("TEXTE DE RÉFÉRENCE — recopie-le, correctement ou avec une faute choisie");
        referenceLabel.setTextSize(11f);
        referenceLabel.setTextColor(MUTED);
        referenceLabel.setTypeface(Typeface.DEFAULT_BOLD);
        referenceLabel.setPadding(dp(2), 0, dp(2), dp(6));
        root.addView(referenceLabel);

        LinearLayout referenceCard = card();
        referenceText = new TextView(this);
        referenceText.setTextSize(24f);
        referenceText.setTextColor(INK);
        referenceText.setGravity(Gravity.END);
        referenceText.setLineSpacing(dp(6), 1.15f);
        referenceCard.addView(referenceText);
        LinearLayout.LayoutParams referenceCardParams = cardParams();
        referenceCardParams.bottomMargin = dp(12);
        root.addView(referenceCard, referenceCardParams);

        TextView chipsLabel = new TextView(this);
        chipsLabel.setText("MOT DE DÉPART (droite → gauche)");
        chipsLabel.setTextSize(11f);
        chipsLabel.setTextColor(MUTED);
        chipsLabel.setTypeface(Typeface.DEFAULT_BOLD);
        chipsLabel.setPadding(dp(2), 0, 0, dp(6));
        root.addView(chipsLabel);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsContainer = row();
        chipsScroll.addView(chipsContainer);
        LinearLayout.LayoutParams chipsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipsParams.bottomMargin = dp(4);
        root.addView(chipsScroll, chipsParams);

        TextView chipsHint = new TextView(this);
        chipsHint.setText("La longueur (combien de mots suivent) est détectée automatiquement — pas besoin de la choisir.");
        chipsHint.setTextSize(11.5f);
        chipsHint.setTextColor(MUTED);
        chipsHint.setPadding(dp(2), 0, 0, dp(12));
        root.addView(chipsHint);

        LinearLayout canvasFrame = new LinearLayout(this);
        canvasFrame.setBackgroundColor(LINE_COLOR);
        canvasFrame.setPadding(dp(2), dp(2), dp(2), dp(2));
        canvas = new WritingCanvasView(this);
        canvasFrame.addView(canvas, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(460)));
        LinearLayout.LayoutParams canvasFrameParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        canvasFrameParams.bottomMargin = dp(12);
        root.addView(canvasFrame, canvasFrameParams);

        LinearLayout buttonRow = row();
        buttonRow.addView(outlineButton("Effacer", v -> {
            canvas.clear();
            showResult(null, null);
        }), buttonWeight());
        buttonRow.addView(primaryButton("Évaluer (auto)", v -> evaluateAuto()), buttonWeight());
        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonRowParams.bottomMargin = dp(16);
        root.addView(buttonRow, buttonRowParams);

        resultCard = card();
        resultCard.setVisibility(View.GONE);
        resultView = new TextView(this);
        resultView.setTextSize(20f);
        resultView.setTypeface(Typeface.DEFAULT_BOLD);
        resultView.setGravity(Gravity.CENTER);
        resultCard.addView(resultView);
        root.addView(resultCard, cardParams());

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
        lineLabel.setText("Ligne " + (lineIndex + 1) + " / " + geometry.lineCount());

        StringBuilder verses = new StringBuilder();
        for (VerseRef ref : line.verses) {
            if (verses.length() > 0) verses.append(", ");
            verses.append(ref);
        }
        subtitle.setText("Page " + line.page + "  ·  ligne id " + line.id + "  ·  versets " + verses);

        float[] viewBox;
        float[][] markers;
        try {
            viewBox = geometry.viewBoxForPage(line.page);
            WordShapeRepository wordShapeRepo = new WordShapeRepository(this);
            AyahMarkerRepository markerRepo = new AyahMarkerRepository(this);
            words = LineWritingGeometry.wordsForLine(wordShapeRepo.shapesForPage(line.page)[line.lineIndexOnPage]);
            markers = LineWritingGeometry.markersWithinBand(markerRepo.markersForPage(line.page), line.top, line.bottom);
        } catch (Throwable error) {
            showResult("Géométrie indisponible pour cette ligne : " + error, BAD);
            return;
        }

        startWord = 0;
        showResult(null, null);

        StringBuilder text = new StringBuilder();
        VerseText verseText = VerseText.get(this);
        for (VerseRef ref : line.verses) {
            String verse = verseText.textFor(ref);
            if (verse == null) continue;
            if (text.length() > 0) text.append("  ·  ");
            text.append(verse);
        }
        referenceText.setText(text.length() > 0 ? text.toString() : "(texte de référence indisponible pour cette ligne)");

        canvas.configure(viewBox[0], (float) line.top, viewBox[2], (float) (line.bottom - line.top), markers);

        chipsContainer.removeAllViews();
        wordChipButtons = new Button[words.size()];
        for (int i = 0; i < words.size(); i++) {
            int wordIdx = i;
            Button chip = chipButton(String.valueOf(i + 1), v -> {
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

    /**
     * Tries every word count starting from the selected word (1 word, 2 words, ... to the end of
     * the line) against the user's actual trace, and keeps whichever length scores best — so the
     * tester never has to manually match a chip selection to how much they actually wrote.
     */
    private void evaluateAuto() {
        if (canvas.isEmpty()) {
            showResult("Écris d’abord quelque chose avant d’évaluer.", WARN);
            return;
        }
        if (words == null || words.isEmpty() || startWord >= words.size()) {
            showResult("Aucun mot de référence pour cette ligne.", WARN);
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
            showResult("Trajectoire (forme) : indisponible.", WARN);
            return;
        }
        int lastWord = startWord + bestLength;
        String range = bestLength == 1
            ? "mot " + (startWord + 1)
            : "mots " + (startWord + 1) + "-" + lastWord;
        int color = bestScore >= 70 ? GOOD : bestScore >= 40 ? WARN : BAD;
        showResult(range + "  —  " + bestScore + " %", color);
    }

    private void showResult(String text, Integer color) {
        if (text == null) {
            resultCard.setVisibility(View.GONE);
            return;
        }
        resultCard.setVisibility(View.VISIBLE);
        resultView.setText(text);
        int c = color == null ? INK : color;
        resultView.setTextColor(c);
        resultCard.setBackground(rounded(SURFACE, c, dp(1), dp(12)));
    }

    // ---- small UI helpers (no XML resources; this test app stays fully code-built) ----

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(SURFACE, LINE_COLOR, dp(1), dp(12)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(16);
        return params;
    }

    private Button navButton(String symbol, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(symbol);
        button.setTextSize(22f);
        button.setTextColor(INK);
        button.setBackground(rounded(PAPER, LINE_COLOR, dp(1), dp(10)));
        button.setMinWidth(dp(52));
        button.setMinHeight(dp(52));
        button.setOnClickListener(listener);
        return button;
    }

    private Button chipButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(15f);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_selected}, rounded(INK, INK, 0, dp(10)));
        bg.addState(new int[]{}, rounded(PAPER, LINE_COLOR, dp(1), dp(10)));
        button.setBackground(bg);
        setChipTextColorStates(button);
        button.setMinWidth(dp(46));
        button.setMinHeight(dp(46));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int gap = dp(4);
        params.setMargins(gap, gap, gap, gap);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    /** Selected chip text turns paper-colored (readable on the ink-filled background); unselected stays ink. */
    private void setChipTextColorStates(Button button) {
        int[][] states = new int[][]{{android.R.attr.state_selected}, {}};
        int[] colors = new int[]{PAPER, INK};
        button.setTextColor(new ColorStateList(states, colors));
    }

    private Button outlineButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14f);
        button.setTextColor(INK);
        button.setBackground(rounded(PAPER, LINE_COLOR, dp(1), dp(10)));
        button.setMinHeight(dp(48));
        button.setOnClickListener(listener);
        return button;
    }

    private Button primaryButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(Color.WHITE);
        button.setBackground(rounded(ACCENT, ACCENT, 0, dp(10)));
        button.setMinHeight(dp(48));
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams buttonWeight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int gap = dp(4);
        params.setMargins(gap, gap, gap, gap);
        return params;
    }

    private void weight(View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        view.setLayoutParams(params);
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        if (strokeWidth > 0) shape.setStroke(strokeWidth, stroke);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
