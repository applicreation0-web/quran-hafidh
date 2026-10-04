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
    private Button verifyButton;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private File recordingFile;
    private boolean recording;
    private boolean pendingRecordAfterPermission;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        history = new QuizHistory(this);
        try {
            geometry = GeometryRepository.get(this);
            words = WordGeometryRepository.shared(this);
            corpus = new QuizCorpus(geometry, words);
        } catch (Throwable error) {
            Ui.showFatal(this, "Le Quiz ne peut pas charger la géométrie du Mushaf.");
            return;
        }
        showSetup();
    }

    private void showSetup() {
        releaseAudio(true);
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

        // What the Quiz is for, said once in plain words: it checks memory outside the sessions
        // and never touches Progression.
        TextView purpose = Ui.text(this,
            "Vérifiez ce que vous avez déjà mémorisé, hors séance : un indice s’affiche, vous "
                + "récitez de mémoire, puis vous comparez avec le Mushaf et vous vous notez. "
                + "Votre progression n’est jamais modifiée.", 13f, false);
        purpose.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 12));
        purpose.setLineSpacing(0f, 1.15f);
        root.addView(purpose);

        TextView typeLabel = Ui.bookText(this, "Exercice", 14f, true);
        typeLabel.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), 0, Ui.dp(this, 2));
        root.addView(typeLabel);
        root.addView(Ui.divider(this));
        root.addView(modeChoice(QuizCorpus.Mode.CONTINUE, "Suite du verset",
            "Les 3 premiers mots d’un verset s’affichent : récitez la suite."));
        root.addView(Ui.divider(this));
        root.addView(modeChoice(QuizCorpus.Mode.PREVIOUS, "Verset précédent",
            "Un verset s’affiche : récitez celui qui le précède."));
        root.addView(Ui.divider(this));
        root.addView(modeChoice(QuizCorpus.Mode.MIXED, "Les deux, au hasard",
            "Alterne les deux exercices."));
        root.addView(Ui.divider(this));

        TextView note = Ui.text(this, QUESTION_COUNT + " questions · versets entièrement mémorisés · "
            + "enregistrement de votre voix facultatif, sur l’appareil", 11.5f, false);
        note.setTextColor(Ui.MUTED);
        note.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12));
        root.addView(note);

        root.addView(Ui.button(this, "Commencer", v -> startQuiz()));
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    /** One plain selectable line (no boxed button): title, one-line explanation, check when chosen. */
    private View modeChoice(QuizCorpus.Mode mode, String title, String explanation) {
        boolean chosen = selectedMode == mode;
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 4), Ui.dp(this, 9), Ui.dp(this, 4), Ui.dp(this, 9));
        row.setMinimumHeight(Ui.dp(this, 56));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ". " + explanation + (chosen ? " Choisi." : ""));
        row.setOnClickListener(v -> { selectedMode = mode; showSetup(); });
        LinearLayout texts = Ui.column(this);
        texts.setPadding(0, 0, 0, 0);
        TextView name = Ui.bookText(this, title, 14.5f, chosen);
        name.setTextColor(chosen ? Ui.INK : Ui.MUTED);
        texts.addView(name);
        TextView detail = Ui.text(this, explanation, 12f, false);
        detail.setTextColor(Ui.MUTED);
        texts.addView(detail);
        Ui.weight(texts, 1f);
        row.addView(texts);
        TextView mark = Ui.text(this, "", 1f, false);
        if (chosen) {
            mark.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ui_validate, 0, 0, 0);
            mark.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.INK));
        }
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        return row;
    }

    /** Flat text action for the self-assessment: no box, ink fill only while pressed. */
    private Button flatChoice(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14f);
        button.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        button.setStateListAnimator(null);
        button.setElevation(0f);
        android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
        android.graphics.drawable.GradientDrawable pressed = new android.graphics.drawable.GradientDrawable();
        pressed.setColor(Ui.INK);
        pressed.setCornerRadius(Ui.dp(this, 8));
        background.addState(new int[]{android.R.attr.state_pressed}, pressed);
        background.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        button.setBackground(background);
        button.setTextColor(new android.content.res.ColorStateList(
            new int[][]{{android.R.attr.state_pressed}, {}}, new int[]{Ui.PAPER, Ui.INK}));
        button.setMinHeight(Ui.dp(this, 48));
        button.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        button.setLayoutParams(params);
        return button;
    }

    private void startQuiz() {
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
        Button retry = Ui.iconButton(this, "↻", "Réessayer", v -> retryQuestion());
        verifyButton = Ui.iconButton(this, "✓", "Vérifier", v -> verifyAnswer());
        actionRow.addView(recordButton);
        actionRow.addView(playButton);
        actionRow.addView(retry);
        actionRow.addView(verifyButton);
        root.addView(actionRow);

        assessmentRow = Ui.row(this);
        assessmentRow.setGravity(Gravity.CENTER);
        assessmentRow.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 6));
        assessmentRow.addView(flatChoice("Juste", v -> assess(QuizHistory.Result.CORRECT)));
        assessmentRow.addView(flatChoice("Hésitant", v -> assess(QuizHistory.Result.HESITATION)));
        assessmentRow.addView(flatChoice("À revoir", v -> assess(QuizHistory.Result.REVIEW)));
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
        releaseAudio(true);
        retryCount = 0;
        recordUsed = false;
        verified = false;
        counter.setText((questionIndex + 1) + "/" + questions.size());
        instruction.setText(question.instruction());
        assessmentRow.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        recordButton.setEnabled(true);
        playButton.setEnabled(false);
        audioStatus.setText("Récitez de mémoire, puis touchez ✓ pour voir la réponse.");

        JSONArray visible = promptBoxes(question);
        if (visible.length() == 0) {
            Toast.makeText(this, "Géométrie exacte du mot indisponible pour cette question.", Toast.LENGTH_LONG).show();
            questionIndex++;
            showQuestion();
            return;
        }

        mushaf.setMaskFollowsSelection(false);
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
        releaseAudio(true);
        audioStatus.setText("");
        QuizQuestion question = currentQuestion();
        if (question == null) return;
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
        verifyButton.setEnabled(false);
        recordButton.setEnabled(false);
        assessmentRow.setVisibility(View.VISIBLE);
        instruction.setText("Réponse : " + question.expected + " — comment était votre récitation ?");
        audioStatus.setText("");
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
        releaseAudio(true);
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 20);
        root.setPadding(side, Ui.dp(this, 10), side, Ui.dp(this, 18));
        TextView title = Ui.bookText(this, "Quiz", 22f, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(summaryRow("Juste", correctCount));
        root.addView(summaryRow("Hésitant", hesitationCount));
        root.addView(summaryRow("À revoir", reviewCount));
        root.addView(Ui.button(this, "Terminer", v -> finish()));
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
        releaseAudio(true);
        try {
            recordingFile = new File(getCacheDir(), "quiz-" + System.nanoTime() + ".m4a");
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
            audioStatus.setText("Enregistrement…");
        } catch (Exception error) {
            releaseAudio(true);
            Toast.makeText(this, "Enregistrement indisponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!recording || recorder == null) return;
        try {
            recorder.stop();
            audioStatus.setText("Enregistrement prêt.");
        } catch (RuntimeException tooShort) {
            if (recordingFile != null) recordingFile.delete();
            recordingFile = null;
            audioStatus.setText("Enregistrement trop court.");
        } finally {
            try { recorder.reset(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
            recording = false;
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_record);
            Ui.setIconDescription(recordButton, "Enregistrer");
            playButton.setEnabled(recordingFile != null && recordingFile.isFile() && recordingFile.length() > 0L);
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
        if (deleteFile && recordingFile != null) {
            recordingFile.delete();
            recordingFile = null;
        }
        if (recordButton != null) {
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_record);
            Ui.setIconDescription(recordButton, "Enregistrer");
        }
        if (playButton != null) playButton.setEnabled(recordingFile != null && recordingFile.isFile());
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
        releaseAudio(true);
        super.onDestroy();
    }

    @Override public void onVerseTap(VerseRef verse) {}
    @Override public void onReady() {}
    @Override public void onError(String message) {
        Toast.makeText(this, message == null ? "Erreur Mushaf" : message, Toast.LENGTH_SHORT).show();
    }
    @Override public void onPageShown(int page) {}
}
