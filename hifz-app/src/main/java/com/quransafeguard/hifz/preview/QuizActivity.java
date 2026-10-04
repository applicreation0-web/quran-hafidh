package com.quransafeguard.hifz.preview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
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
    /** Verses the learner rated "À revoir" in this series (the verse they had to recite). */
    private final java.util.LinkedHashSet<VerseRef> reviewVerses = new java.util.LinkedHashSet<>();
    private int retryCount;
    private boolean recordUsed;
    private boolean verified;

    private MushafView mushaf;
    private TextView instruction;
    private TextView counter;
    private TextView audioStatus;
    private LinearLayout assessmentRow;
    private LinearLayout actionRow;
    private Button retryButton;
    private Button assessPlayButton;
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
        purgeQuizRecordings();
        actionRow = null;
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

        // Spec UI pass 2 (23.9): one segmented type selector, one info line, one start icon.
        LinearLayout modes = Ui.row(this);
        modes.setGravity(Gravity.CENTER);
        modes.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 4));
        addSegment(modes, QuizCorpus.Mode.MIXED, "Mélangé");
        addSegmentSeparator(modes);
        addSegment(modes, QuizCorpus.Mode.CONTINUE, "Continuer");
        addSegmentSeparator(modes);
        addSegment(modes, QuizCorpus.Mode.PREVIOUS, "Précédent");
        root.addView(modes);

        TextView hint = Ui.text(this, selectedMode == QuizCorpus.Mode.CONTINUE
            ? "Récitez la suite du verset."
            : selectedMode == QuizCorpus.Mode.PREVIOUS
                ? "Récitez le verset qui précède."
                : "Suite du verset ou verset précédent.", 12f, false);
        hint.setTextColor(Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);

        TextView info = Ui.text(this, QUESTION_COUNT + " questions · corpus mémorisé", 12.5f, false);
        info.setTextColor(Ui.MUTED);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        root.addView(info);

        LinearLayout startRow = Ui.row(this);
        startRow.setGravity(Gravity.CENTER);
        startRow.addView(Ui.iconButton(this, "▶", "Commencer", v -> startQuiz()));
        root.addView(startRow);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    /** Flat segmented choice: plain words, the chosen one in ink and bold, no box. */
    private void addSegment(LinearLayout row, QuizCorpus.Mode mode, String label) {
        boolean chosen = selectedMode == mode;
        TextView item = Ui.bookText(this, label, 15f, chosen);
        item.setTextColor(chosen ? Ui.INK : Ui.MUTED);
        item.setGravity(Gravity.CENTER);
        item.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        item.setMinHeight(Ui.dp(this, 48));
        item.setClickable(true);
        item.setFocusable(true);
        item.setSelected(chosen);
        item.setContentDescription(label + (chosen ? ", choisi" : ""));
        item.setOnClickListener(v -> { selectedMode = mode; showSetup(); });
        if (chosen) item.setPaintFlags(item.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        row.addView(item);
    }

    private void addSegmentSeparator(LinearLayout row) {
        TextView bar = Ui.text(this, "|", 15f, false);
        bar.setTextColor(Ui.LINE);
        row.addView(bar);
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
        reviewVerses.clear();
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
        // Slim two-line header (spec 23.10): "Quiz 4/10" then the one short instruction.
        LinearLayout titles = Ui.column(this);
        titles.setPadding(0, 0, 0, 0);
        titles.setGravity(Gravity.CENTER);
        Ui.weight(titles, 1f);
        counter = Ui.bookText(this, "Quiz", 15f, true);
        counter.setGravity(Gravity.CENTER);
        titles.addView(counter);
        instruction = Ui.text(this, "", 12f, false);
        instruction.setTextColor(Ui.MUTED);
        instruction.setGravity(Gravity.CENTER);
        instruction.setSingleLine(true);
        titles.addView(instruction);
        header.addView(back);
        header.addView(titles);
        header.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(header);

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        root.addView(mushaf, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Recording status only takes a line while there is something to say.
        audioStatus = Ui.text(this, "", 11f, false);
        audioStatus.setTextColor(Ui.MUTED);
        audioStatus.setGravity(Gravity.CENTER);
        audioStatus.setVisibility(View.GONE);
        root.addView(audioStatus);

        // One footer row whose icons follow the state (spec 13): before a take [●][✓], after a
        // take [▶][↻][✓], at correction [▶ if a take exists][✓][!][↻]. No captions.
        actionRow = Ui.row(this);
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setMinimumHeight(Ui.dp(this, 48));
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
        assessmentRow.setMinimumHeight(Ui.dp(this, 48));
        assessPlayButton = Ui.iconButton(this, "▶", "Écouter l’enregistrement", v -> playRecording());
        assessmentRow.addView(assessPlayButton);
        assessmentRow.addView(Ui.iconButton(this, "✓", "Correct", v -> assess(QuizHistory.Result.CORRECT)));
        assessmentRow.addView(Ui.iconButton(this, "!", "Hésitation", v -> assess(QuizHistory.Result.HESITATION)));
        assessmentRow.addView(Ui.iconButton(this, "↻", "À revoir", v -> assess(QuizHistory.Result.REVIEW)));
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
        counter.setText("Quiz " + (questionIndex + 1) + "/" + questions.size());
        instruction.setText(question.instruction());
        setAudioStatus("");
        updateQuestionActions();

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

    /**
     * Visible words of a question: the prompt (first three words of the verse, or the whole verse
     * for "Précédent") plus the page's real first three and last three words (spec 13 — page
     * bounds, not verse bounds), all from exact quran-ws boxes. Missing geometry = no question.
     */
    private JSONArray promptBoxes(QuizQuestion question) {
        JSONArray pageBounds = words.pageLandmarkBoxes(question.promptPage);
        if (pageBounds.length() != 6) return new JSONArray();
        JSONArray all = words.boxesForVerse(question.promptPage, question.prompt);
        JSONArray out = new JSONArray();
        if (question.type == QuizQuestion.Type.PREVIOUS) {
            if (all.length() == 0) return new JSONArray();
            for (int i = 0; i < all.length(); i++) out.put(all.optJSONArray(i));
        } else {
            if (all.length() < 3) return new JSONArray();
            JSONArray firstThree = new JSONArray();
            for (int i = 0; i < 3; i++) firstThree.put(all.optJSONArray(i));
            for (int i = 0; i < firstThree.length(); i++) out.put(firstThree.optJSONArray(i));
        }
        for (int i = 0; i < pageBounds.length(); i++) out.put(pageBounds.optJSONArray(i));
        return out;
    }

    private boolean hasTake() {
        return recordingFile != null && recordingFile.isFile() && recordingFile.length() > 0L;
    }

    /** Shows only the actions useful in the current state. */
    private void updateQuestionActions() {
        if (actionRow == null) return;
        boolean take = hasTake();
        actionRow.setVisibility(verified ? View.GONE : View.VISIBLE);
        assessmentRow.setVisibility(verified ? View.VISIBLE : View.GONE);
        recordButton.setVisibility(!verified && (recording || !take) ? View.VISIBLE : View.GONE);
        playButton.setVisibility(!verified && take && !recording ? View.VISIBLE : View.GONE);
        retryButton.setVisibility(!verified && take && !recording ? View.VISIBLE : View.GONE);
        verifyButton.setVisibility(verified ? View.GONE : View.VISIBLE);
        assessPlayButton.setVisibility(verified && take ? View.VISIBLE : View.GONE);
    }

    private void setAudioStatus(String text) {
        if (audioStatus == null) return;
        audioStatus.setText(text == null ? "" : text);
        audioStatus.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void retryQuestion() {
        if (verified) return;
        retryCount++;
        releaseAudio(true);
        setAudioStatus("");
        updateQuestionActions();
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
        updateQuestionActions();
        instruction.setText("Correction · " + question.expected);
        setAudioStatus("");
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
        else {
            reviewCount++;
            reviewVerses.add(question.expected);
        }
        questionIndex++;
        showQuestion();
    }

    private void showSummary() {
        releaseAudio(true);
        purgeQuizRecordings();
        actionRow = null;
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 20);
        root.setPadding(side, Ui.dp(this, 10), side, Ui.dp(this, 18));
        TextView title = Ui.bookText(this, "Quiz", 19f, true);
        title.setGravity(Gravity.CENTER);
        title.setMinHeight(Ui.dp(this, 48));
        root.addView(title);
        root.addView(Ui.divider(this));
        root.addView(summaryRow("Correct", correctCount));
        root.addView(summaryRow("Hésitation", hesitationCount));
        root.addView(summaryRow("À revoir", reviewCount));
        root.addView(Ui.divider(this));
        if (!reviewVerses.isEmpty()) {
            // Each verse to review opens in Lecture; adding them to Repères faibles is offered,
            // never automatic (explicit confirmation, add-only, Progression untouched).
            for (VerseRef verse : reviewVerses) {
                root.addView(Ui.settingRow(this, QuranSurahNames.name(verse.getSurah()) + " " + verse.getAyah(),
                    verse.toString(), v -> openInLecture(verse)));
                root.addView(Ui.divider(this));
            }
            LinearLayout addRow = Ui.settingRow(this, "Ajouter aux Repères faibles",
                reviewVerses.size() + " verset" + (reviewVerses.size() > 1 ? "s" : ""), null);
            addRow.setClickable(true);
            addRow.setFocusable(true);
            addRow.setOnClickListener(v -> confirmAddToWeakSpots(addRow));
            root.addView(addRow);
            root.addView(Ui.divider(this));
        }
        LinearLayout finishRow = Ui.row(this);
        finishRow.setGravity(Gravity.CENTER);
        finishRow.addView(Ui.iconButton(this, "✓", "Terminer", v -> finish()));
        root.addView(finishRow);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void openInLecture(VerseRef verse) {
        Intent intent = new Intent(this, StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, geometry.pageForVerse(verse));
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, verse.toString());
        startActivity(intent);
    }

    private void confirmAddToWeakSpots(LinearLayout addRow) {
        int count = reviewVerses.size();
        new android.app.AlertDialog.Builder(this)
            .setTitle("Repères faibles")
            .setMessage("Ajouter " + count + " verset" + (count > 1 ? "s" : "") + " à revoir aux Repères faibles ? "
                + "Ils seront proposés en Révision active. Votre progression n’est pas modifiée.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Ajouter", (dialog, which) -> {
                int added = prefs.addMurajaahWeakVerses(new ArrayList<>(reviewVerses));
                TextView value = Ui.settingValue(addRow);
                if (value != null) value.setText(added > 0 ? "Ajoutés" : "Déjà marqués");
                addRow.setEnabled(false);
                addRow.setAlpha(0.6f);
            })
            .show();
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
            setAudioStatus("Enregistrement…");
            updateQuestionActions();
        } catch (Exception error) {
            releaseAudio(true);
            Toast.makeText(this, "Enregistrement indisponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!recording || recorder == null) return;
        try {
            recorder.stop();
            setAudioStatus("");
        } catch (RuntimeException tooShort) {
            if (recordingFile != null) recordingFile.delete();
            recordingFile = null;
            setAudioStatus("Enregistrement trop court.");
        } finally {
            try { recorder.reset(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
            recording = false;
            Ui.setButtonIcon(recordButton, R.drawable.ic_ui_record);
            Ui.setIconDescription(recordButton, "Enregistrer");
            updateQuestionActions();
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
                setAudioStatus("");
            });
            player.prepare();
            player.start();
            setAudioStatus("Lecture…");
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
        updateQuestionActions();
    }

    /** Every take is temporary: wipe any quiz-*.m4a left in the cache (spec 29.5). */
    private void purgeQuizRecordings() {
        File[] stale = getCacheDir().listFiles((dir, name) -> name.startsWith("quiz-") && name.endsWith(".m4a"));
        if (stale == null) return;
        for (File file : stale) {
            if (file.equals(recordingFile)) continue;
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
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
        purgeQuizRecordings();
        super.onDestroy();
    }

    @Override public void onVerseTap(VerseRef verse) {}
    @Override public void onReady() {}
    @Override public void onError(String message) {
        Toast.makeText(this, message == null ? "Erreur Mushaf" : message, Toast.LENGTH_SHORT).show();
    }
    @Override public void onPageShown(int page) {}
}
