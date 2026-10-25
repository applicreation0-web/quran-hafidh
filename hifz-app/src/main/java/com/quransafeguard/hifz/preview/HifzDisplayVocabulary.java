package com.quransafeguard.hifz.preview;

/** Display-only compatibility for labels persisted by pre-0.7.5 builds. */
final class HifzDisplayVocabulary {
    private HifzDisplayVocabulary() {}

    static String canonicalize(String label) {
        if (label == null || label.isEmpty()) return label;
        // Reserve the new Itqān label before translating historical "Entretien"
        // (which originally meant passive review), then restore it verbatim.
        String scoped=label.replace("Itqān · Entretien","__ITQAN_MAINTENANCE__")
            .replace("Entretien · Itqān","__ITQAN_MAINTENANCE_REVERSED__");
        return scoped
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
  .replace("Entretien installé", "Révision passive installée")
  .replace("Entretien", "Révision passive")
  .replace("Murājaʿah", "Révision")
  .replace("Murajaah", "Révision")
  .replace("__ITQAN_MAINTENANCE__", "Itqān · Entretien")
  .replace("__ITQAN_MAINTENANCE_REVERSED__", "Entretien · Itqān");
    }
}
