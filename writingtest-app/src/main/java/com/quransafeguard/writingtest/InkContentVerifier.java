package com.quransafeguard.writingtest;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Content verification via ML Kit's digital-ink recognition — checks WHICH word(s) were actually
 * written, complementing WritingCanvasView's geometric Palier 3 score. The trajectory score alone
 * is shape-only and blind to identity: confirmed on a real device where a wrong/reordered word
 * scored close to a correct one because its overall traced shape happened to be similar once
 * centered and rescaled.
 *
 * Recognizes the WHOLE ink as drawn — it has no notion of "how many words" the trajectory search
 * settled on, since ML Kit reads every stroke on the canvas regardless. The caller is responsible
 * for deciding which expected text(s) to check the returned candidates against (see
 * WritingTestActivity.evaluateAuto(), which tries every word-count length rather than just the
 * trajectory's best-scoring one — the two searches are independent and can disagree).
 *
 * Unlike hifz-app's InkContentVerifier, this test app calls it unconditionally (no settings gate,
 * no HifzPrefs dependency) — its whole purpose here is to validate what each layer does and
 * doesn't catch, on demand, for a tester.
 */
final class InkContentVerifier {
    interface Callback {
        void onResult(List<String> candidates);
        void onError(String message);
    }

    private InkContentVerifier() {}

    static void verify(List<float[]> strokesAsFlatXYT, Callback callback) {
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ar");
        } catch (Exception e) {
            callback.onError("Modèle arabe introuvable : " + e.getMessage());
            return;
        }
        if (modelIdentifier == null) {
            callback.onError("Aucun modèle d'écriture arabe disponible.");
            return;
        }

        DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();
        RemoteModelManager manager = RemoteModelManager.getInstance();
        manager.download(model, new DownloadConditions.Builder().build())
            .addOnSuccessListener((OnSuccessListener<Void>) unused -> recognize(model, strokesAsFlatXYT, callback))
            .addOnFailureListener((OnFailureListener) e -> callback.onError("Échec du téléchargement du modèle : " + e.getMessage()));
    }

    private static void recognize(DigitalInkRecognitionModel model, List<float[]> strokesAsFlatXYT, Callback callback) {
        DigitalInkRecognizer recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
        Ink.Builder inkBuilder = Ink.builder();
        for (float[] stroke : strokesAsFlatXYT) {
            Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
            for (int i = 0; i + 2 < stroke.length; i += 3) {
                strokeBuilder.addPoint(Ink.Point.create(stroke[i], stroke[i + 1], (long) stroke[i + 2]));
            }
            inkBuilder.addStroke(strokeBuilder.build());
        }

        recognizer.recognize(inkBuilder.build())
            .addOnSuccessListener((OnSuccessListener<RecognitionResult>) result -> {
                List<String> candidates = new ArrayList<>();
                for (RecognitionCandidate c : result.getCandidates()) candidates.add(c.getText());
                callback.onResult(candidates);
            })
            .addOnFailureListener((OnFailureListener) e -> callback.onError("Échec de la reconnaissance : " + e.getMessage()));
    }
}
