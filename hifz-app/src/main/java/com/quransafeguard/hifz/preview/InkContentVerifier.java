package com.quransafeguard.hifz.preview;

import android.content.Context;

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

import com.quransafeguard.hifz.core.ArabicTextComparison;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.List;

/**
 * The ONLY class in Quran Hifz allowed to reference a network-capable API
 * (enforced by verifyHifzProductBoundary). Checks whether handwritten strokes recognize as the
 * expected verse's real word — content only, never neatness/shape (that stays with the fully
 * offline Palier 2/3 geometric check).
 *
 * Confirmed empirically (three on-device tests during this feature's design, see
 * ArabicTextComparison's docs) that ML Kit's Arabic ink model never recognizes tashkil (short
 * vowels) — only the consonant skeleton. So this catches a wrong word or a wrong base letter,
 * never a wrong vowel.
 *
 * Every path here starts by checking {@link HifzPrefs#advancedWritingVerificationEnabled()};
 * when it is false (the default), this class never touches the network, never downloads a
 * model, and never calls any ML Kit API.
 */
public final class InkContentVerifier {
    public interface Callback {
        /** matches is null when the verse has no known text in the corpus to compare against. */
        void onResult(List<String> candidates, Boolean matches);
        void onDisabled();
        void onError(String message);
    }

    private InkContentVerifier() {}

    public static void verify(Context context, List<float[]> strokesAsFlatXYT, VerseRef verse, Callback callback) {
        HifzPrefs prefs = new HifzPrefs(context);
        if (!prefs.advancedWritingVerificationEnabled()) {
            callback.onDisabled();
            return;
        }

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
            .addOnSuccessListener((OnSuccessListener<Void>) unused -> recognize(context, model, strokesAsFlatXYT, verse, callback))
            .addOnFailureListener((OnFailureListener) e -> callback.onError("Échec du téléchargement du modèle : " + e.getMessage()));
    }

    private static void recognize(Context context, DigitalInkRecognitionModel model,
                                   List<float[]> strokesAsFlatXYT, VerseRef verse, Callback callback) {
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

                String expected = VerseText.get(context).textFor(verse);
                Boolean matches = expected == null ? null : ArabicTextComparison.anyMatches(candidates, expected);
                callback.onResult(candidates, matches);
            })
            .addOnFailureListener((OnFailureListener) e -> callback.onError("Échec de la reconnaissance : " + e.getMessage()));
    }
}
