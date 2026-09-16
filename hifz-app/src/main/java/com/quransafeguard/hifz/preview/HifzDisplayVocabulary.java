package com.quransafeguard.hifz.preview;

/** Display-only compatibility for labels persisted by pre-0.7.5 builds. */
final class HifzDisplayVocabulary {
    private HifzDisplayVocabulary() {}

    static String canonicalize(String label) {
        if (label == null || label.isEmpty()) return label;
        return label
  .replace("Reprise du soir", "Apprentissage · reprise")
  .replace("Leçon neuve", "Apprentissage")
  .replace("Sabqi review", "Apprentissage · reprise")
  .replace("Ṣabqī", "Apprentissage")
  .replace("Sabqi", "Apprentissage")
  .replace("Itqān installé", "Stabilisation installée")
  .replace("Itqan installé", "Stabilisation installée")
  .replace("Itqān", "Stabilisation")
  .replace("Itqan", "Stabilisation")
  .replace("Ancrage fractionné", "Stabilisation")
  .replace("Ancrage", "Stabilisation")
  .replace("Entretien installé", "Révision installée")
  .replace("Entretien", "Révision")
  .replace("Murājaʿah", "Révision")
  .replace("Murajaah", "Révision");
    }
}
