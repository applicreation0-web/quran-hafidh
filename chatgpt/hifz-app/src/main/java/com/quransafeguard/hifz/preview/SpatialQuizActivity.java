package com.quransafeguard.hifz.preview;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Optional spatial recall quiz over the real acquired corpus.
 *
 * It is deliberately read-only: answers update only counters owned by this Activity and never
 * HifzPrefs progression. The 15-minute ceiling starts only after at least one valid question exists.
 */
public final class SpatialQuizActivity extends android.app.Activity implements MushafView.Listener {
    static final long QUIZ_LIMIT_MS = 15L * 60L * 1000L;

    private enum Kind {
        TEXT_TO_POSITION,
        POSITION_TO_TEXT,
        TRANSITION
    }

    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private MushafView mushaf;
    private TextView prompt;
    private TextView feedback;
    private TextView timerText;
    private TextView scoreText;
    private Button revealButton;
    private Button exactButton;
    private Button almostButton;
    private Button reviewButton;

    private final Random random = new Random();
    private final ArrayList<GeometryRepository.LineMeta> acquiredLines = new ArrayList<>();
    private final ArrayList<GeometryRepository.LineMeta> transitionTargets = new ArrayList<>();
    private final ArrayList<Kind> availableKinds = new ArrayList<>();

    private GeometryRepository.LineMeta currentTarget;
    private GeometryRepository.LineMeta currentPrevious;
    private Kind currentKind;
    private String previousQuestionLineId;
    private boolean answerUnlocked;
    private boolean placementStage;
    private int questionCount;
    private int exactCount;
    private int almostCount;
    private int reviewCount;
    private CountDownTimer timer;
    private boolean timerStarted;
    private boolean finished;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        try {
            geometry = GeometryRepository.get(this);
        } catch (Throwable error) {
            showUnavailable("La géométrie exacte du Mushaf est indisponible.");
            return;
        }
        try {
            buildQuestionCorpus();
        } catch (RuntimeException unavailableProgression) {
            showUnavailable("Quiz indisponible : la progression Acquis doit d’abord être initialisée.");
            return;
        }
        if (acquiredLines.isEmpty()) {
            showUnavailable("Quiz indisponible : aucune ligne n’est encore au statut Acquis.");
            return;
        }
        buildUi();
    }

    private void buildQuestionCorpus() {
        HifzPrefs.ProgressionSnapshot snapshot = prefs.progressionSnapshotV6();
        Set<String> acquired = new HashSet<>(snapshot.acquired);
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (acquired.contains(line.id)) acquiredLines.add(line);
        }

        for (GeometryRepository.LineMeta line : acquiredLines) {
            if (line.globalIndex <= 0) continue;
            GeometryRepository.LineMeta previous = geometry.line(line.globalIndex - 1);
            if (previous.page == line.page && acquired.contains(previous.id)) transitionTargets.add(line);
        }

        availableKinds.add(Kind.TEXT_TO_POSITION);
        availableKinds.add(Kind.POSITION_TO_TEXT);
        if (!transitionTargets.isEmpty()) availableKinds.add(Kind.TRANSITION);
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 8));

        LinearLayout top = Ui.row(this);
        top.addView(Ui.iconButton(this, "‹", "Retour", v -> finish()));
        TextView title = Ui.bookText(this, "Quiz · Acquis", 17f, true);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        top.addView(title);
        timerText = Ui.text(this, "15:00", 12.5f, true);
        timerText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        timerText.setMinWidth(Ui.dp(this, 58));
        top.addView(timerText);
        root.addView(top);

        prompt = Ui.text(this, "", 13.5f, true);
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(Ui.dp(this, 8), Ui.dp(this, 5), Ui.dp(this, 8), Ui.dp(this, 5));
        root.addView(prompt);

        FrameLayout readerPane = new FrameLayout(this);
        mushaf = new MushafView(this);
        mushaf.setListener(this);
        mushaf.setMaskEntropy("spatial_quiz");
        readerPane.addView(mushaf, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams readerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        readerParams.topMargin = Ui.dp(this, 2);
        readerParams.bottomMargin = Ui.dp(this, 2);
        root.addView(readerPane, readerParams);

        feedback = Ui.text(this, "", 12f, false);
        feedback.setGravity(Gravity.CENTER);
        feedback.setTextColor(Ui.MUTED);
        feedback.setMinHeight(Ui.dp(this, 28));
        root.addView(feedback);

        LinearLayout controls = Ui.row(this);
        controls.setGravity(Gravity.CENTER);
        revealButton = Ui.smallButton(this, "Révéler", v -> revealAnswer());
        exactButton = Ui.smallButton(this, "Exact", v -> score(2));
        almostButton = Ui.smallButton(this, "Presque", v -> score(1));
        reviewButton = Ui.smallButton(this, "À revoir", v -> score(0));
        controls.addView(revealButton);
        controls.addView(exactButton);
        controls.addView(almostButton);
        controls.addView(reviewButton);
        root.addView(controls);

        scoreText = Ui.text(this, "", 11.5f, false);
        scoreText.setTextColor(Ui.MUTED);
        scoreText.setGravity(Gravity.CENTER);
        root.addView(scoreText);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
        setScoreButtonsEnabled(false);
        updateScore();
    }

    @Override public void onReady() {
        if (finished || mushaf == null) return;
        if (nextQuestion()) startTimerAfterFirstValidQuestion();
    }

    private boolean nextQuestion() {
        if (finished || availableKinds.isEmpty()) return false;
        answerUnlocked = false;
        placementStage = false;
        feedback.setText("");
        setScoreButtonsEnabled(false);
        revealButton.setEnabled(true);
        revealButton.setVisibility(View.VISIBLE);

        currentKind = availableKinds.get(questionCount % availableKinds.size());
        currentTarget = null;
        currentPrevious = null;

        if (currentKind == Kind.TRANSITION && transitionTargets.isEmpty()) currentKind = Kind.POSITION_TO_TEXT;

        if (currentKind == Kind.TEXT_TO_POSITION) {
            currentTarget = chooseLine(acquiredLines);
            if (currentTarget == null) return false;
            prompt.setText("Mémorisez cette ligne, puis retrouvez sa position.");
            revealButton.setText("Voir la page");
            revealButton.setContentDescription("Voir la page");
            mushaf.clearSemanticCues();
            mushaf.prepareQuizIsolatedLine(currentTarget.id);
            mushaf.show(currentTarget.page, Collections.emptyList(), Collections.emptyList(), 0);
        } else if (currentKind == Kind.POSITION_TO_TEXT) {
            currentTarget = chooseLine(acquiredLines);
            if (currentTarget == null) return false;
            prompt.setText("Récitez la ligne indiquée.");
            revealButton.setText("Révéler");
            revealButton.setContentDescription("Révéler");
            showMaskedQuestion(currentTarget, Collections.emptyList(), true);
        } else {
            currentTarget = chooseLine(transitionTargets);
            if (currentTarget == null || currentTarget.globalIndex <= 0) return false;
            currentPrevious = geometry.line(currentTarget.globalIndex - 1);
            prompt.setText("Continuez après la ligne visible.");
            revealButton.setText("Révéler");
            revealButton.setContentDescription("Révéler");
            showMaskedQuestion(currentTarget, Collections.singletonList(currentPrevious.id), true);
        }
        return true;
    }

    private GeometryRepository.LineMeta chooseLine(List<GeometryRepository.LineMeta> pool) {
        if (pool == null || pool.isEmpty()) return null;
        GeometryRepository.LineMeta chosen = pool.get(random.nextInt(pool.size()));
        if (pool.size() > 1 && chosen.id.equals(previousQuestionLineId)) {
            int start = pool.indexOf(chosen);
            chosen = pool.get((start + 1 + random.nextInt(pool.size() - 1)) % pool.size());
        }
        previousQuestionLineId = chosen.id;
        return chosen;
    }

    private void showMaskedQuestion(GeometryRepository.LineMeta target, List<String> visibleLines, boolean guideTarget) {
        mushaf.clearSemanticCues();
        mushaf.setQuizGuide(guideTarget ? target.id : null, visibleLines);
        mushaf.show(target.page, Collections.emptyList(), geometry.lineIdsOnPage(target.page), 100);
    }

    private void revealAnswer() {
        if (currentTarget == null || finished) return;

        if (currentKind == Kind.TEXT_TO_POSITION && !placementStage) {
            placementStage = true;
            prompt.setText("Touchez l’emplacement exact.");
            feedback.setText("");
            revealButton.setText("Révéler");
            revealButton.setContentDescription("Révéler");
            mushaf.prepareQuizPlacement(currentTarget.id);
            mushaf.show(currentTarget.page, Collections.emptyList(), geometry.lineIdsOnPage(currentTarget.page), 100);
            return;
        }

        mushaf.setQuizGuide(currentTarget.id, Collections.emptyList());
        mushaf.setMask(0);
        answerUnlocked = true;
        setScoreButtonsEnabled(true);
        revealButton.setEnabled(false);
        revealButton.setVisibility(View.GONE);
        if (feedback.getText().length() == 0) feedback.setText("Réponse affichée · évaluez votre rappel.");
    }

    @Override public void onVerseTap(VerseRef verse) {
        // Spatial placement is scored from the exact physical line tap, not a whole-verse polygon.
    }

    @Override public void onQuizLineTap(String lineId) {
        if (finished || currentKind != Kind.TEXT_TO_POSITION || !placementStage
                || currentTarget == null || answerUnlocked) return;
        boolean correct = currentTarget.id.equals(lineId);
        feedback.setText(correct ? "Position correcte." : "Autre position · la ligne exacte est maintenant indiquée.");
        mushaf.setQuizGuide(currentTarget.id, Collections.emptyList());
        mushaf.setMask(0);
        answerUnlocked = true;
        setScoreButtonsEnabled(true);
        revealButton.setEnabled(false);
        revealButton.setVisibility(View.GONE);
    }

    private void score(int grade) {
        if (!answerUnlocked || finished) return;
        if (grade >= 2) exactCount++;
        else if (grade == 1) almostCount++;
        else reviewCount++;
        questionCount++;
        updateScore();
        nextQuestion();
    }

    private void setScoreButtonsEnabled(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        if (exactButton != null) { exactButton.setEnabled(enabled); exactButton.setVisibility(visibility); }
        if (almostButton != null) { almostButton.setEnabled(enabled); almostButton.setVisibility(visibility); }
        if (reviewButton != null) { reviewButton.setEnabled(enabled); reviewButton.setVisibility(visibility); }
    }

    private void updateScore() {
        if (scoreText == null) return;
        scoreText.setText("Questions " + questionCount
            + " · exact " + exactCount + " · presque " + almostCount + " · à revoir " + reviewCount);
    }

    private void startTimerAfterFirstValidQuestion() {
        if (timerStarted || finished) return;
        timerStarted = true;
        timer = new CountDownTimer(QUIZ_LIMIT_MS, 1000L) {
            @Override public void onTick(long millisUntilFinished) {
                long seconds = Math.max(0L, millisUntilFinished / 1000L);
                timerText.setText(String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L));
            }
            @Override public void onFinish() {
                timerText.setText("00:00");
                finishQuiz("15 minutes terminées");
            }
        }.start();
    }

    private void finishQuiz(String reason) {
        if (finished) return;
        finished = true;
        if (timer != null) timer.cancel();
        if (mushaf != null) {
            mushaf.setMask(0);
            mushaf.clearQuizGuide();
        }
        prompt.setText(reason);
        feedback.setText("Quiz facultatif · aucun changement de progression.");
        revealButton.setEnabled(false);
        revealButton.setVisibility(View.GONE);
        setScoreButtonsEnabled(false);
        updateScore();
    }

    @Override public void onPageSwipe(int delta) {
        // A question owns its physical page; swiping must not silently change the answer surface.
    }

    @Override public void onSurfaceTap() {}
    @Override public void onSemanticCueTap(String passageId) {}
    @Override public void onPageShown(int page) {}
    @Override public void onError(String message) {
        if (!finished) feedback.setText(message == null ? "Affichage indisponible." : message);
    }

    private void showUnavailable(String message) {
        LinearLayout root = Ui.column(this);
        root.setGravity(Gravity.CENTER);
        TextView title = Ui.bookText(this, "Quiz", 18f, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView body = Ui.text(this, message, 13f, false);
        body.setGravity(Gravity.CENTER);
        body.setTextColor(Ui.MUTED);
        body.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        root.addView(body);
        root.addView(Ui.button(this, "Retour", v -> finish()));
        setContentView(root);
        Ui.respectSystemBars(this, root, Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        if (mushaf != null) mushaf.destroySafely();
        super.onDestroy();
    }
}
