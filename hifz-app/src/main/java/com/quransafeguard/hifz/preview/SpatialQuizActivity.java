package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional adaptive spatial-memory quiz. The Mushaf itself is the memory palace — every
 * question is a real ACQUIRED verse's real page/line, never an invented locus.
 *
 * Strictly UI + scoring-display: never calls a V6 transition, never touches Sabqi/Itqān/
 * Consolidation/Renforcement progression, credits or the daily closure date. SpatialQuizStore
 * persists only this screen's own adaptive history.
 */
public final class SpatialQuizActivity extends Activity implements MushafView.Listener {
    private static final long MAX_DURATION_MS = 15 * 60 * 1000L;
    private static final long FEEDBACK_DELAY_MS = 1100L;

    private MushafView mushaf;
    private TextView promptText;
    private TextView feedbackText;
    private TextView timerText;
    private LinearLayout choicesRow;

    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private SpatialQuizStore store;
    private SpatialQuizEngine engine;
    private SpatialQuizEngine.Question current;
    private long questionShownAtMs;
    private long startedElapsedRealtime;
    private boolean finished;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (finished) return;
            long remaining = MAX_DURATION_MS - (SystemClock.elapsedRealtime() - startedElapsedRealtime);
            if (remaining <= 0) { finishQuiz("Quiz spatial · 15 minutes atteintes"); return; }
            long totalSeconds = remaining / 1000;
            timerText.setText(String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60));
            mainHandler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        store = new SpatialQuizStore(this);
        startedElapsedRealtime = SystemClock.elapsedRealtime();

        LinearLayout root = Ui.column(this);

        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Quiz spatial", 15f, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        timerText = Ui.text(this, "15:00", 13f, true);
        timerText.setPadding(0, 0, Ui.dp(this, 10), 0);
        top.addView(timerText);
        root.addView(top);

        promptText = Ui.bookText(this, "", 20f, true);
        promptText.setGravity(Gravity.CENTER);
        promptText.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 4));
        root.addView(promptText);

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        root.addView(mushaf, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        feedbackText = Ui.text(this, "", 14f, true);
        feedbackText.setGravity(Gravity.CENTER);
        feedbackText.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 2));
        root.addView(feedbackText);

        choicesRow = Ui.row(this);
        choicesRow.setGravity(Gravity.CENTER);
        choicesRow.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 10));
        root.addView(choicesRow);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        mainHandler.post(tick);

        io.execute(() -> {
            try {
                GeometryRepository loadedGeometry = GeometryRepository.get(getApplicationContext());
                VerseText verseText = VerseText.get(getApplicationContext());
                SpatialQuizCorpus.Snapshot snapshot = SpatialQuizCorpus.build(prefs, loadedGeometry, verseText);
                Map<String, SpatialQuizEngine.Stats> history = store.loadAll();
                runOnUiThread(() -> {
                    geometry = loadedGeometry;
                    engine = new SpatialQuizEngine(
                        snapshot.candidates, snapshot.candidates, snapshot.transitions, history, new Random());
                    showNextQuestion();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> Ui.showFatal(this, "Le quiz spatial est indisponible pour le moment."));
            }
        });
    }

    private void showNextQuestion() {
        if (finished) return;
        SpatialQuizEngine.Question q = engine.next();
        if (q == null) {
            promptText.setText("Pas encore assez de contenu acquis pour ce quiz.");
            choicesRow.removeAllViews();
            feedbackText.setText("");
            mushaf.setSpatialTapEnabled(false);
            return;
        }
        current = q;
        questionShownAtMs = SystemClock.elapsedRealtime();
        feedbackText.setText("");
        choicesRow.removeAllViews();
        switch (q.kind) {
            case TEXT_TO_POSITION:
                promptText.setText(q.prompt.arabicSnippet);
                mushaf.setSpatialTapEnabled(true);
                // The page's own ink must stay hidden — showing it here would let the answer be
                // read straight off the page instead of recalled from spatial memory, defeating
                // the question. Landmarks/tap detection are unaffected: masking is purely visual.
                mushaf.show(q.prompt.page, Collections.emptyList(), geometry.lineIdsOnPage(q.prompt.page), 100);
                break;
            case POSITION_TO_TEXT:
            case TRANSITION: {
                promptText.setText(q.kind == SpatialQuizEngine.Kind.TRANSITION
                    ? "Quelle est la suite ?" : "Quel texte correspond à cette position ?");
                mushaf.setSpatialTapEnabled(false);
                mushaf.show(q.prompt.page, Collections.emptyList(),
                    Collections.singletonList(q.prompt.targetLineId), 100, true);
                for (SpatialQuizEngine.Candidate choice : q.choices) {
                    Button button = Ui.button(this, choice.arabicSnippet, v -> onChoiceSelected(choice));
                    choicesRow.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
                break;
            }
        }
    }

    private void onChoiceSelected(SpatialQuizEngine.Candidate choice) {
        if (current == null) return;
        long responseMs = SystemClock.elapsedRealtime() - questionShownAtMs;
        SpatialQuizEngine.Answer answer = engine.scoreChoice(current, choice.verse, responseMs);
        store.recordAnswer(current.id, answer);
        showFeedback(answer.verdict);
        current = null;
        choicesRow.removeAllViews();
        mainHandler.postDelayed(this::showNextQuestion, FEEDBACK_DELAY_MS);
    }

    @Override public void onSpatialLineTap(String lineId) {
        if (current == null || current.kind != SpatialQuizEngine.Kind.TEXT_TO_POSITION) return;
        long responseMs = SystemClock.elapsedRealtime() - questionShownAtMs;
        List<GeometryRepository.LineMeta> pageLines =
            geometry.linesForExactIds(geometry.lineIdsOnPage(current.prompt.page));
        SpatialQuizEngine.Answer answer = engine.scorePosition(current, lineId, responseMs, pageLines);
        store.recordAnswer(current.id, answer);
        showFeedback(answer.verdict);
        current = null;
        mushaf.setSpatialTapEnabled(false);
        mainHandler.postDelayed(this::showNextQuestion, FEEDBACK_DELAY_MS);
    }

    private void showFeedback(SpatialQuizEngine.Verdict verdict) {
        switch (verdict) {
            case EXACT: feedbackText.setText("Exact"); break;
            case ALMOST: feedbackText.setText("Presque"); break;
            default: feedbackText.setText("À revoir"); break;
        }
    }

    private void finishQuiz(String message) {
        finished = true;
        current = null;
        mushaf.setSpatialTapEnabled(false);
        promptText.setText(message);
        choicesRow.removeAllViews();
        feedbackText.setText("");
    }

    @Override public void onVerseTap(VerseRef verse) { /* the spatial quiz never uses whole-verse taps */ }
    @Override public void onReady() { }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onPageShown(int page) { }

    @Override protected void onDestroy() {
        finished = true;
        io.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        if (mushaf != null) mushaf.destroySafely();
        super.onDestroy();
    }
}
