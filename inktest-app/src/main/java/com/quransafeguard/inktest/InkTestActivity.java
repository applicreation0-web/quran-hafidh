package com.quransafeguard.inktest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.vision.digitalink.RecognitionCandidate;
import com.google.mlkit.vision.digitalink.RecognitionResult;

/**
 * Single-purpose diagnostic: write an Arabic word WITH tashkil (short vowel marks) below,
 * run ML Kit's Arabic digital-ink model, and show every raw recognition candidate exactly
 * as returned — no cleanup, no comparison. The only question this answers: do the fatha/
 * kasra/damma/sukun/shadda marks show up in the output at all, for any candidate?
 */
public final class InkTestActivity extends Activity {
    private DrawView drawView;
    private TextView statusView;
    private TextView resultView;
    private DigitalInkRecognitionModel model;
    private boolean modelReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView instructions = new TextView(this);
        instructions.setTextSize(15);
        instructions.setText("Écris ce mot (avec ses voyelles) dans le cadre ci-dessous :\n\n"
                + "اَلْحَمْدُ\n\n"
                + "(fatha sur le alif, sukun sur le lam, damma sur le dal — si ces signes "
                + "ressortent dans le texte reconnu en bas, l'API les capte.)");
        instructions.setGravity(Gravity.START);
        root.addView(instructions);

        drawView = new DrawView(this);
        LinearLayout.LayoutParams drawParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(260));
        drawParams.topMargin = dp(12);
        drawParams.bottomMargin = dp(12);
        root.addView(drawView, drawParams);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        Button clearBtn = new Button(this);
        clearBtn.setText("Effacer");
        clearBtn.setOnClickListener(v -> {
            drawView.clear();
            resultView.setText("");
        });
        Button recognizeBtn = new Button(this);
        recognizeBtn.setText("Reconnaître");
        recognizeBtn.setOnClickListener(v -> onRecognize());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        buttonRow.addView(clearBtn, btnParams);
        buttonRow.addView(recognizeBtn, btnParams);
        root.addView(buttonRow);

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setPadding(0, dp(12), 0, dp(4));
        root.addView(statusView);

        resultView = new TextView(this);
        resultView.setTextSize(22);
        resultView.setTextIsSelectable(true);
        resultView.setPadding(0, dp(4), 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(resultView);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        downloadModel();
    }

    private void downloadModel() {
        statusView.setText("Téléchargement du modèle d'écriture arabe…");
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ar");
        } catch (Exception e) {
            statusView.setText("Impossible de résoudre le modèle arabe : " + e.getMessage());
            return;
        }
        if (modelIdentifier == null) {
            statusView.setText("Aucun modèle d'écriture arabe disponible pour ML Kit.");
            return;
        }
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build();
        RemoteModelManager manager = RemoteModelManager.getInstance();
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        manager.download(model, conditions)
                .addOnSuccessListener((OnSuccessListener<Void>) unused -> {
                    modelReady = true;
                    statusView.setText("Modèle prêt. Écris le mot puis appuie sur Reconnaître.");
                })
                .addOnFailureListener((OnFailureListener) e ->
                        statusView.setText("Échec du téléchargement du modèle : " + e.getMessage()));
    }

    private void onRecognize() {
        if (!modelReady || model == null) {
            statusView.setText("Le modèle n'est pas encore prêt.");
            return;
        }
        if (drawView.isEmpty()) {
            statusView.setText("Écris d'abord le mot avant de lancer la reconnaissance.");
            return;
        }
        statusView.setText("Reconnaissance en cours…");
        DigitalInkRecognizer recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build());
        Ink ink = drawView.buildInk();
        recognizer.recognize(ink)
                .addOnSuccessListener((OnSuccessListener<RecognitionResult>) result -> {
                    StringBuilder sb = new StringBuilder();
                    int i = 1;
                    for (RecognitionCandidate candidate : result.getCandidates()) {
                        sb.append(i).append(". ").append(candidate.getText()).append('\n');
                        i++;
                    }
                    if (sb.length() == 0) sb.append("(aucun candidat retourné)");
                    resultView.setText(sb.toString());
                    statusView.setText("Terminé — " + (i - 1) + " candidat(s). "
                            + "Vérifie si les voyelles (َ ِ ُ ْ ّ) apparaissent ci-dessus.");
                })
                .addOnFailureListener((OnFailureListener) e ->
                        statusView.setText("Échec de la reconnaissance : " + e.getMessage()));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
