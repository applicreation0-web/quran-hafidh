package com.quransafeguard.hifz.preview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Free self-assessed Quiz. No speech recognition, no writing, no network, no Progression write-back.
 */
public final class QuizActivity extends android.app.Activity implements MushafView.Listener {
    private static final int RECORD_PERMISSION = 2407;
    private static final int QUESTION_COUNT = 10;

    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private WordGeometryRepository words;
    private QuizCorpus corpus;
    private QuizHistory history;
    private QuizCorpus.Mode selectedMode = QuizCorpus.Mode.MIXED;

    private List<QuizQuestion> questions = Collections.emptyList();
    private int questionIndex;
    private int correctCount;
    private int hesitationCount;
    private int reviewCount;
    private int retryCount;
    private boolean recordUsed;
    private boolean verified;

    private MushafView mushaf;
    private TextView instruction;
    private TextView counter;
    private TextView audioStatus;
    private LinearLayout assessmentRow;
    private Button recordButton;
    private Button playButton;
    private Button retryButton;
    private Button verifyButton;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private File recordingFile;
    /** Interface Quiz: fichiers temporaires gardés pendant la série, jamais au-delà du Quiz. */
    private final List<File> sessionRecordings = new ArrayList<>();
    private boolean recording;
    private boolean pendingRecordAfterPermission;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        history = new QuizHistory(this);
        try {
            geometry = GeometryRepository.get(this);
            words = new WordGeometryRepository(this);
            corpus = new QuizCorpus(geometry, words);
        } catch (Throwable error) {
            Ui.showFatal(this, "Le Quiz ne peut pas charger la géométrie du Mushaf.");
            return;
        }
        showSetup();
    }

    private void showSetup() {
        // Interface Quiz — revenir au réglage signifie quitter la série en cours.
        clearQuizAudio();
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 18);
        root.setPadding(side, Ui.dp(this, 8), side, Ui.dp(this, 18));

        LinearLayout header = Ui.row(this);
        Button back = Ui.iconButton(this, "‹", "Retour", v -> finish());
        TextView title = Ui.bookText(this, "Quiz", 21f, true);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        TextView balance = Ui.text(this, "", 1f, false);
        header.addView(back);
        header.addView(title);
        header.addView(balance, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(header);

        TextView corpusLabel = Ui.bookText(this, "Corpus", 14f, true);
        corpusLabel.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 3));
        root.addView(corpusLabel);
        TextView corpusValue = Ui.text(this, "Mémorisé", 13f, false);
        corpusValue.setTextColor(Ui.MUTED);
        root.addView(corpusValue);

        TextView typeLabel = Ui.bookText(this, "Type", 14f, true);
        typeLabel.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        root.addView(typeLabel);

        LinearLayout modes = Ui.row(this);
        modes.setGravity(Gravity.CENTER);
        Button mixed = Ui.smallButton(this, "Mélangé", v -> { selectedMode = QuizCorpus.Mode.MIXED; showSetup(); });
        Button continuation = Ui.smallButton(this, "Continuer", v -> { selectedMode = QuizCorpus.Mode.CONTINUE; showSetup(); });
        Button previous = Ui.smallButton(this, "Précédent", v -> { selectedMode = QuizCorpus.Mode.PREVIOUS; showSetup(); });
        Ui.setChosen(mixed, selectedMode == QuizCorpus.Mode.MIXED);
        Ui.setChosen(continuation, selectedMode == QuizCorpus.Mode.CONTINUE);
        Ui.setChosen(previous, selectedMode == QuizCorpus.Mode.PREVIOUS);
        modes.addView(mixed);
        modes.addView(continuation);
        modes.addView(previous);
        root.addView(modes);

        root.addView(Ui.settingRow(this, "Questions", Integer.toString(QUESTION_COUNT), null));

        TextView note = Ui.text(this, "Auto-évaluation · enregistrement local facultatif · aucune reconnaissance vocale", 11.5f, false);
        note.setTextColor(Ui.MUTED);
        note.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        root.addView(note);

        // Interface Quiz — action principale compacte, icône seule (libellé via accessibilité/tooltip).
        LinearLayout startRow = Ui.row(this);
        startRow.setGravity(Gravity.CENTER);
        startRow.addView(Ui.iconButton(this, "", "Commencer le Quiz", v -> startQuiz()));
        root.addView(startRow);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void startQuiz() {
        clearQuizAudio();
        try {
            questions = corpus.questions(prefs.progressionSnapshotV6(), selectedMode, QUESTION_COUNT);
        } catch (Throwable error) {
            Toast.makeText(this, "Corpus du Quiz indisponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (questions.isEmpty()) {
            Toast.makeText(this, "Pas encore assez de versets entièrement mémorisés pour ce Quiz.", Toast.LENGTH_LONG).show();
            return;
        }
        questionIndex = 0;
        correctCount = hesitationCount = reviewCount = 0;
        buildQuestionScreen();
        showQuestion();
    }

    private void buildQuestionScreen() {
        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Ui.PAPER);
        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);
        holder.addView(root, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        Button back = Ui.iconButton(this, "‹", "Retour", v -> showSetup());
        TextView title = Ui.bookText(this, "Quiz", 19f, true);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        counter = Ui.text(this, "", 12f, false);
        counter.setTextColor(Ui.MUTED);
        counter.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(back);
        header.addView(title);
        header.addView(counter, new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 48)));
        root.addView(header);

        instruction = Ui.text(this, "", 13f, false);
        instruction.setGravity(Gravity.CENTER);
        instruction.setPadding(Ui.dp(this, 12), Ui.dp(this, 2), Ui.dp(this, 12), Ui.dp(this, 5));
        root.addView(instruction);

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        root.addView(mushaf, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        audioStatus = Ui.text(this, "", 11f, false);
        audioStatus.setTextColor(Ui.MUTED);
        audioStatus.setGravity(Gravity.CENTER);
        audioStatus.setMinHeight(Ui.dp(this, 22));
        root.addView(audioStatus);

        LinearLayout actionRow = Ui.row(this);
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setMinimumHeight(Ui.dp(this, 58));
        recordButton = Ui.iconButton(this, "●", "Enregistrer", v -> toggleRecording());
        playButton = Ui.iconButton(this, "▶", "Écouter l’enregistrement", v -> playRecording());
        retryButton = Ui.iconButton(this, "↻", "Réessayer", v -> retryQuestion());
        verifyButton = Ui.iconButton(this, "✓", "Vérifier", v -> verifyAnswer());
        actionRow.addView(recordButton);
        actionRow.addView(playButton);
        actionRow.addView(retryButton);
        actionRow.addView(verifyButton);
        root.addView(actionRow);

        assessmentRow = Ui.row(this);
        assessmentRow.setGravity(Gravity.CENTER);
        assessmentRow.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 6));
        // Interface Quiz — aucune légende sous les icônes : ✓ / hésitation / à revoir sont
        // exposés par contentDescription et tooltip.
        assessmentRow.addView(Ui.iconButton(this, "", "Correct", v -> assess(QuizHistory.Result.CORRECT)));
        assessmentRow.addView(Ui.iconButton(this, "", "Hésitation", v -> assess(QuizHistory.Result.HESITATION)));
        assessmentRow.addView(Ui.iconButton(this, "", "À revoir", v -> assess(QuizHistory.Result.REVIEW)));
        assessmentRow.setVisibility(View.GONE);
        root.addView(assessmentRow);

        setContentView(holder);
        Ui.respectSystemBars(this, holder, 0, 0, 0, 0);
    }

    private QuizQuestion currentQuestion() {
        return questionIndex >= 0 && questionIndex < questions.size() ? questions.get(questionIndex) : null;
    }

    private void showQuestion() {
        QuizQuestion question = currentQuestion();
        if (question == null) {
            showSummary();
            return;
        }
        // Interface Quiz — l'audio de la question précédente reste dans le cache jusqu'à la fin
        // de la série, mais n'est plus la piste active de la nouvelle question.
        releaseAudio(false);
        recordingFile = null;
        retryCount = 0;
        recordUsed = false;
        verified = false;
        counter.setText((questionIndex + 1) + "/" + questions.size());
        instruction.setText(question.typeLabel() + " · " + question.instruction());
        assessmentRow.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        verifyButton.setVisibility(View.VISIBLE);
        recordButton.setEnabled(true);
        recordButton.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);
        playButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        audioStatus.setText("");

        JSONArray visible = promptBoxes(question);
        if (visible.length() == 0) {
            Toast.makeText(this, "Géométrie exacte du mot indisponible pour cette question.", Toast.LENGTH_LONG).show();
            questionIndex++;
            showQuestion();
            return;
        }

        mushaf.setMaskFollowsSelection(false);
        mushaf.setPreserveVerseMarkersOnMask(false);
        mushaf.setSemanticCues(new JSONArray(), true, false);
        mushaf.setPageLandmarkBoxes(visible);
        mushaf.setMaskEntropy("quiz-" + questionIndex + "-" + question.prompt);
        mushaf.show(question.promptPage, Collections.emptyList(),
            geometry.lineIdsOnPage(question.promptPage), 100, false);
    }

    private JSONArray promptBoxes(QuizQuestion question) {
        JSONArray all = words.boxesForVerse(question.promptPage, question.prompt);
        if (question.type == QuizQuestion.Type.PREVIOUS) return all;
        if (all.length() < 3) return new JSONArray();
        JSONArray firstThree = new JSONArray();
        for (int i = 0; i < 3; i++) firstThree.put(all.optJSONArray(i));
        return firstThree;
    }

    private void retryQuestion() {
        if (verified) return;
        retryCount++;
        // Interface Quiz — Réessayer remplace seulement la prise courante; les autres questions
        // restent temporaires jusqu'à la fin de la série.
        releaseAudio(false);
        discardCurrentRecording();
        playButton.setEnabled(false);
        playButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        audioStatus.setText("");
        QuizQuestion question = currentQuestion();
        if (question == null) return;
        mushaf.setPreserveVerseMarkersOnMask(false);
        mushaf.setSemanticCues(new JSONArray(), true, false);
        mushaf.setPageLandmarkBoxes(promptBoxes(question));
        mushaf.setMaskFollowsSelection(false);
        mushaf.show(question.promptPage, Collections.emptyList(),
            geometry.lineIdsOnPage(question.promptPage), 100, false);
    }

    private void verifyAnswer() {
        QuizQuestion question = currentQuestion();
        if (question == null || verified) return;
        if (recording) stopRecording();
        verified = true;
        // Interface Quiz — pendant la correction, seule la réécoute éventuelle et
        // l'auto-évaluation restent utiles.
        verifyButton.setVisibility(View.GONE);
        recordButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        boolean hasRecording = hasCurrentRecording();
        playButton.setEnabled(hasRecording);
        playButton.setVisibility(hasRecording ? View.VISIBLE : View.GONE);
        assessmentRow.setVisibility(View.VISIBLE);
        instruction.setText("Réponse · " + question.expected);
        mushaf.setPreserveVerseMarkersOnMask(true);
        mushaf.setSemanticCues(new JSONArray(), false, false);
        mushaf.clearPageLandmarkBoxes();
        mushaf.setMaskFollowsSelection(true);
        List<String> exact = geometry.lineIdsForVerseRange(question.expected, question.expected);
        mushaf.show(question.expectedPage, Collections.singletonList(question.expected), exact, 0, true);
    }

    private void assess(QuizHistory.Result result) {
        QuizQuestion question = currentQuestion();
        if (question == null || !verified) return;
        history.record(question, result, retryCount, recordUsed);
        if (result == QuizHistory.Result.CORRECT) correctCount++;
        else if (result == QuizHistory.Result.HESITATION) hesitationCount++;
        else reviewCount++;
        questionIndex++;
        showQuestion();
    }

    private void showSummary() {
        // Interface Quiz — la série est terminée : aucun enregistrement ne survit au résumé.
        clearQuizAudio();
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 20);
        root.setPadding(side, Ui.dp(this, 10), side, Ui.dp(this, 18));
        TextView title = Ui.bookText(this, "Quiz", 22f, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(summaryRow("Correct", correctCount));
        root.addView(summaryRow("Hésitation", hesitationCount));
        root.addView(summaryRow("À revoir", reviewCount));
        LinearLayout finishRow = Ui.row(this);
        finishRow.setGravity(Gravity.CENTER);
        finishRow.addView(Ui.iconButton(this, "", "Terminer", v -> finish()));
        root.addView(finishRow);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private View summaryRow(String label, int value) {
        LinearLayout row = Ui.settingRow(this, label, Integer.toString(value), null);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return row;
    }

    private void toggleRecording() {
        if (verified) return;
        if (recording) {
            stopRecording();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingRecordAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION);
            return;
        }
        startRecording();
    }

    @SuppressWarnings("deprecation")
    private void startRecording() {
        releaseAudio(false);
        discardCurrentRecording();
        try {
            recordingFile = new File(getCacheDir(), "quiz-" + System.nanoTime() + ".m4a");
            sessionRecordings.add(recordingFile);
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(recordingFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recording = true;
            recordUsed = true;
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_stop);
            Ui.setIconDescription(recordButton, "Arrêter");
            playButton.setEnabled(false);
            playButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
            audioStatus.setText("Enregistrement…");
        } catch (Exception error) {
            releaseAudio(false);
            discardCurrentRecording();
            Toast.makeText(this, "Enregistrement indisponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!recording || recorder == null) return;
        try {
            recorder.stop();
            audioStatus.setText("Enregistrement prêt.");
        } catch (RuntimeException tooShort) {
            discardCurrentRecording();
            audioStatus.setText("Enregistrement trop court.");
        } finally {
            try { recorder.reset(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
            recording = false;
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_record);
            Ui.setIconDescription(recordButton, "Enregistrer");
            boolean ready = hasCurrentRecording();
            playButton.setEnabled(ready);
            playButton.setVisibility(ready ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(ready && !verified ? View.VISIBLE : View.GONE);
        }
    }

    private void playRecording() {
        if (recording) stopRecording();
        if (recordingFile == null || !recordingFile.isFile() || recordingFile.length() <= 0L) return;
        if (player != null) {
            player.release();
            player = null;
        }
        try {
            player = new MediaPlayer();
            player.setDataSource(recordingFile.getAbsolutePath());
            player.setOnCompletionListener(mp -> {
                mp.release();
                if (player == mp) player = null;
                audioStatus.setText("Lecture terminée.");
            });
            player.prepare();
            player.start();
            audioStatus.setText("Lecture…");
        } catch (Exception error) {
            if (player != null) {
                player.release();
                player = null;
            }
            Toast.makeText(this, "Lecture de l’enregistrement impossible.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasCurrentRecording() {
        return recordingFile != null && recordingFile.isFile() && recordingFile.length() > 0L;
    }

    private void discardCurrentRecording() {
        if (recordingFile == null) return;
        sessionRecordings.remove(recordingFile);
        recordingFile.delete();
        recordingFile = null;
    }

    /** Interface Quiz — purge unique de toutes les prises temporaires de la série. */
    private void clearQuizAudio() {
        releaseAudio(false);
        for (File file : new ArrayList<>(sessionRecordings)) {
            if (file != null) file.delete();
        }
        sessionRecordings.clear();
        recordingFile = null;
    }

    private void releaseAudio(boolean deleteFile) {
        if (recorder != null) {
            try { if (recording) recorder.stop(); } catch (RuntimeException ignored) {}
            try { recorder.reset(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
        }
        recording = false;
        if (player != null) {
            player.release();
            player = null;
        }
        if (deleteFile) discardCurrentRecording();
        if (recordButton != null) {
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_record);
            Ui.setIconDescription(recordButton, "Enregistrer");
        }
        if (playButton != null) playButton.setEnabled(hasCurrentRecording());
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != RECORD_PERMISSION) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted && pendingRecordAfterPermission && currentQuestion() != null && !verified) startRecording();
        else if (!granted) Toast.makeText(this, "Microphone refusé : le Quiz reste utilisable sans enregistrement.", Toast.LENGTH_LONG).show();
        pendingRecordAfterPermission = false;
    }

    @Override protected void onPause() {
        super.onPause();
        if (recording) stopRecording();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override protected void onDestroy() {
        clearQuizAudio();
        super.onDestroy();
    }

    @Override public void onVerseTap(VerseRef verse) {}
    @Override public void onReady() {}
    @Override public void onError(String message) {
        Toast.makeText(this, message == null ? "Erreur Mushaf" : message, Toast.LENGTH_SHORT).show();
    }
    @Override public void onPageShown(int page) {}
}
