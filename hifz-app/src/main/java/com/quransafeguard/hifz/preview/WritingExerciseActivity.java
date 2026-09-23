package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.core.ArabicHint;
import com.quransafeguard.hifz.core.TrajectoryComparison;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The real writing-exercise screen (Step 4): a physical Mushaf line's worth of blank canvas —
 * write from memory, the letterform is never shown — with the real ayah-end markers kept visible
 * at their exact printed positions, a hidden-by-default hint (first three letters WITH tashkil),
 * and two separate, honest scores: Palier 3 (trajectory shape, always available offline) and, only
 * when the user has opted in via Settings, ML Kit content verification (never voyels — see
 * InkContentVerifier's own docs for why).
 */
public final class WritingExerciseActivity extends android.app.Activity {
    public static final String EXTRA_LINE_ID = "com.quransafeguard.hifz.preview.LINE_ID";

    private GeometryRepository.LineMeta line;
    /** This line's words (cells), in real reading order — each a group of reference subpaths. */
    private List<List<double[]>> words;
    private boolean[] wordSelected;
    private Button[] wordChipButtons;
    private VerseRef hintVerse;
    private WritingCanvasView canvas;
    private TextView hintText;
    private TextView trajectoryScoreView;
    private TextView contentScoreView;
    private HifzPrefs prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new HifzPrefs(this);

        String lineId = getIntent().getStringExtra(EXTRA_LINE_ID);
        if (lineId == null) {
            Ui.showFatal(this, "Aucune ligne indiquée pour l’exercice d’écriture.");
            return;
        }

        float[] viewBox;
        float[][] markersOnThisLine;
        try {
            GeometryRepository geometry = GeometryRepository.get(this);
            line = geometry.linesForExactIds(Collections.singletonList(lineId)).get(0);
            viewBox = geometry.viewBoxForPage(line.page);
            AyahMarkerRepository markerRepo = new AyahMarkerRepository(this);
            WordShapeRepository wordShapeRepo = new WordShapeRepository(this);
            words = LineWritingGeometry.wordsForLine(
                wordShapeRepo.shapesForPage(line.page)[line.lineIndexOnPage]);
            markersOnThisLine = LineWritingGeometry.markersWithinBand(
                markerRepo.markersForPage(line.page), line.top, line.bottom);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible pour cette ligne.");
            return;
        }
        hintVerse = line.verses.isEmpty() ? null : line.verses.get(0);
        // Default to just the first word: testing a small group at a time (and writing it
        // wrong on purpose sometimes) is the whole point — the full line is opt-in via chips.
        wordSelected = new boolean[words.size()];
        if (wordSelected.length > 0) wordSelected[0] = true;

        LinearLayout root = Ui.column(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = Ui.row(this);
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Exercice d’écriture", 17f, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        TextView balance = Ui.text(this, "", 1f, false);
        balance.setMinWidth(Ui.dp(this, 44));
        top.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        root.addView(top);

        TextView subtitle = Ui.text(this, verseLabel(), 12.5f, false);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        // Pins the overall paragraph to LTR as a belt-and-suspenders measure; verseLabel() itself
        // does the real work of keeping the Arabic surah name and the ayah range from reordering
        // into each other (see its own comment — this alone was NOT enough, confirmed on device).
        subtitle.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);
        root.addView(subtitle);

        if (!words.isEmpty()) {
            HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
            chipsScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout chipsRow = Ui.row(this);
            wordChipButtons = new Button[words.size()];
            for (int i = 0; i < words.size(); i++) {
                int wordIndex = i;
                Button chip = Ui.smallButton(this, String.valueOf(i + 1), v -> {
                    wordSelected[wordIndex] = !wordSelected[wordIndex];
                    Ui.setChosen(wordChipButtons[wordIndex], wordSelected[wordIndex]);
                });
                Ui.setChosen(chip, wordSelected[i]);
                wordChipButtons[i] = chip;
                chipsRow.addView(chip);
            }
            chipsScroll.addView(chipsRow);
            LinearLayout.LayoutParams chipsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipsParams.bottomMargin = Ui.dp(this, 8);
            root.addView(chipsScroll, chipsParams);
            TextView chipsHint = Ui.text(this, "Mots à écrire (droite → gauche) : touchez pour choisir un petit groupe.", 11f, false);
            chipsHint.setTextColor(Ui.MUTED);
            chipsHint.setPadding(0, 0, 0, Ui.dp(this, 8));
            root.addView(chipsHint);
        }

        canvas = new WritingCanvasView(this);
        canvas.configure(viewBox[0], (float) line.top, viewBox[2], (float) (line.bottom - line.top), markersOnThisLine);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        canvasParams.setMargins(0, 0, 0, Ui.dp(this, 8));
        root.addView(canvas, canvasParams);

        hintText = Ui.text(this, "", 26f, false);
        hintText.setGravity(Gravity.END);
        hintText.setVisibility(android.view.View.GONE);
        hintText.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        root.addView(hintText);

        LinearLayout buttonRow = Ui.row(this);
        buttonRow.addView(Ui.button(this, "Effacer", v -> {
            canvas.clear();
            trajectoryScoreView.setText("");
            contentScoreView.setText("");
        }), buttonWeight());
        buttonRow.addView(Ui.button(this, "Indice", v -> revealHint()), buttonWeight());
        buttonRow.addView(Ui.button(this, "Évaluer", v -> evaluate()), buttonWeight());
        root.addView(buttonRow);

        trajectoryScoreView = Ui.text(this, "", 14f, true);
        trajectoryScoreView.setPadding(0, Ui.dp(this, 10), 0, 0);
        root.addView(trajectoryScoreView);

        contentScoreView = Ui.text(this, "", 13f, false);
        contentScoreView.setTextColor(Ui.MUTED);
        contentScoreView.setPadding(0, Ui.dp(this, 4), 0, 0);
        root.addView(contentScoreView);

        setContentView(root);
        Ui.respectSystemBars(this, root, Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
    }

    private LinearLayout.LayoutParams buttonWeight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int gap = Ui.dp(this, 3);
        params.setMargins(gap, gap, gap, gap);
        return params;
    }

    // Unicode directional isolates (LRI/RLI/PDI): setTextDirection alone only fixes the overall
    // paragraph's base direction — it does NOT stop the bidi algorithm from sweeping the Arabic
    // surah name and the plain Latin ayah-number range that follows it into one reordered RTL
    // island (confirmed on a real device: still showed "6 → 5" after that first attempt). Wrapping
    // each run in its own isolate pins its internal order and stops it merging with its neighbor.
    private static final String LRI = "⁦";
    private static final String RLI = "⁧";
    private static final String PDI = "⁩";

    private String verseLabel() {
        return verseLabel(line.page, line.verses);
    }

    static String verseLabel(int page, List<VerseRef> verses) {
        if (verses.isEmpty()) return "Page " + page;
        VerseRef first = verses.get(0);
        VerseRef last = verses.get(verses.size() - 1);
        String ayahRange = first.equals(last)
            ? String.valueOf(first.getAyah())
            : (first.getAyah() + " → " + last.getAyah());
        String surahName = RLI + QuranSurahNames.name(first.getSurah()) + PDI;
        return "Page " + page + " — " + surahName + " " + LRI + ayahRange + PDI;
    }

    private void revealHint() {
        if (hintVerse == null) {
            hintText.setText("Aucun verset identifié pour cette ligne.");
            hintText.setVisibility(android.view.View.VISIBLE);
            return;
        }
        String text = VerseText.get(this).textFor(hintVerse);
        String hint = text == null ? null : ArabicHint.firstLettersWithTashkil(text, 3);
        hintText.setText(hint == null || hint.isEmpty() ? "Indice indisponible pour ce verset." : hint);
        hintText.setVisibility(android.view.View.VISIBLE);
    }

    private void evaluate() {
        if (canvas.isEmpty()) {
            trajectoryScoreView.setText("Écris d’abord le(s) mot(s) choisi(s) avant d’évaluer.");
            contentScoreView.setText("");
            return;
        }

        List<double[]> selectedReference = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            if (wordSelected[i]) selectedReference.addAll(words.get(i));
        }
        if (selectedReference.isEmpty()) {
            trajectoryScoreView.setText("Choisis au moins un mot (chiffres ci-dessus) avant d’évaluer.");
            contentScoreView.setText("");
            return;
        }

        Integer score = TrajectoryComparison.scoreStrokes(selectedReference, canvas.strokesInPageSpace());
        trajectoryScoreView.setText(score == null
            ? "Trajectoire (forme) : indisponible pour ce(s) mot(s)."
            : "Trajectoire (forme) : " + score + " %");

        if (!prefs.advancedWritingVerificationEnabled()) {
            contentScoreView.setText("Vérification de contenu désactivée (Réglages).");
            return;
        }
        if (hintVerse == null) {
            contentScoreView.setText("Vérification de contenu indisponible pour cette ligne.");
            return;
        }
        contentScoreView.setText("Vérification de contenu (" + hintVerse + ") en cours…");
        InkContentVerifier.verify(this, canvas.rawStrokesFlatXYT(), hintVerse, new InkContentVerifier.Callback() {
            @Override public void onResult(List<String> candidates, Boolean matches) {
                runOnUiThread(() -> {
                    String verdict = matches == null ? "texte de référence indisponible"
                        : matches ? "contenu reconnu ✓" : "contenu non reconnu";
                    contentScoreView.setText("Contenu (" + hintVerse + ") : " + verdict
                        + (candidates.isEmpty() ? "" : " — " + candidates.get(0)));
                });
            }

            @Override public void onDisabled() {
                runOnUiThread(() -> contentScoreView.setText("Vérification de contenu désactivée (Réglages)."));
            }

            @Override public void onError(String message) {
                runOnUiThread(() -> contentScoreView.setText("Vérification de contenu : " + message));
            }
        });
    }

    /** Launches the exercise for a specific physical Mushaf line id (e.g. "12:3"). */
    public static Intent intentFor(android.content.Context context, String lineId) {
        Intent intent = new Intent(context, WritingExerciseActivity.class);
        intent.putExtra(EXTRA_LINE_ID, lineId);
        return intent;
    }
}
