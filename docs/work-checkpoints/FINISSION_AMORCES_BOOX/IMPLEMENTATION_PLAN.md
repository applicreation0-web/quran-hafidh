# Finition Amorces, interfaces, révisions et debug BOOX — plan d’exécution

> Exécution native, séquentielle et récupérable. Chaque tâche doit être clôturée,
> testée, committée et poussée avant le début de la suivante.

**But :** terminer CP-00 à CP-07 sur la branche existante sans rouvrir les
travaux M1–M9 déjà prouvés.

**Architecture :** conserver les moteurs Hifz actuels et ajouter seulement les
preuves ciblées, la carte graphique et les mécanismes de paquetage nécessaires.
Les titres approuvés restent un corpus privé : GitHub ne reçoit qu’un conteneur
AEAD et ses outils reproductibles ; le debug personnel reçoit les données
déchiffrées uniquement lors de la construction privée.

**Pile :** Java/Android, Gradle 9.5.0, AGP 9.3.0, JDK 17, tests JVM et Android.

**Spécification :** brief utilisateur « FINITION AMORCES, INTERFACES,
RÉVISIONS ET DEBUG BOOX », reçu le 14 octobre 2026.

## Contraintes globales

- Dépôt `applicreation0-web/quran-hafidh` et branche
  `work/haafidh-1171-ibn-kathir-integration-independent` uniquement.
- Référence officielle `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`
  protégée ; branche QUL exclue.
- Aucun Quiz, J10, mot à mot, nouveau dépôt, nouvelle branche ou nouvelle PR.
- Aucun secret, clé, titre en clair ou APK privé dans Git ou les CI publiques.
- Aucun PASS sans commande fraîche et sortie lue ; tout test matériel non réalisé
  reste `NON EXÉCUTÉ`.

## Risques à surveiller

1. Un titre en clair ou une clé apparaît dans l’historique Git ou un journal.
2. Une promotion du dimanche écrit `Acquis` sans alimenter réellement le corpus
   de Révision active après redémarrage.
3. Une miniature utilise une image reconstituée plutôt que la page QCF réelle.
4. Un écran de Mémorisation libre crédite par erreur le parcours Hifz.
5. Un APK est attribué au mauvais HEAD, au mauvais certificat ou à un corpus
   incomplet.

## Tâches

- [x] **CP-00 — Inventaire et reprise.** Vérifier Git/GitHub, CI, preuves M8,
  paquets M7-B/M9, APK/clé et références graphiques ; figer la liste des manques.
- [x] **CP-01 — Titres chiffrés dans GitHub.** Écrire d’abord les tests de
  structure/intégrité/déchiffrement, produire le conteneur AEAD depuis les huit
  chunks approuvés, générer une clé privée séparée, puis committer uniquement le
  chiffré, le manifeste et les outils sans secret.
- [x] **CP-02 — Progression et révisions.** Ajouter les scénarios d’intégration
  Sabqi/Itqān du dimanche et de reprise ; observer le RED éventuel, appliquer le
  correctif minimal seulement si le défaut est reproduit, puis revalider.
- [x] **CP-03 — Carte sémantique.** Porter la structure graphique utile de
  `IslahiMapActivity`, l’index 1 903 groupes et les miniatures QCF ; écrire les
  tests de parcours et d’état avant l’implémentation puis vérifier Android.
- [x] **CP-04 — Visuel et Mémorisation libre.** Auditer les 23 pictogrammes et
  les zones tactiles, ajouter les assertions d’indépendance de Mémorisation
  libre, corriger uniquement les écarts prouvés et produire des captures.
- [ ] **CP-05 — Non-régression.** Exécuter les suites pertinentes et la matrice
  fonctionnelle sans rejouer automatiquement M8 ; geler le HEAD candidat.
- [ ] **CP-06 — Debug privé BOOX.** Reconstruire localement le corpus depuis le
  chiffré avec la clé privée, construire/signifier l’APK du HEAD CP-05, vérifier
  signature, inventaire, SHA-256, hors-ligne et reprise, puis sauvegarder les
  livrables hors Git.
- [ ] **CP-07 — Clôture.** Distinguer code, émulateur, APK et BOOX physique ;
  laisser les contrôles matériels non réalisés en `NON EXÉCUTÉ`.
