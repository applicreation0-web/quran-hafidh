# Quran Haafidh 1.17.5 — clôture des changements exécutables CP2–CP6 (24 octobre 2026)

## Périmètre
Travaux sur `work/haafidh-1175-boox-ux-hifz-atlas`.
**Exercice facultatif de rappel/masquage des titres d'Unités Atlas expressément EXCLU** à la demande du propriétaire. Aucun code correspondant introduit.
Aucune publication publique et aucune release officielle autorisées.

## Modifications effectivement codées
- `WeeklyBilan.java` et `WeeklyBilanTest.java`: bilan du lundi au jour courant, calculé uniquement à partir des reçus datés Sabqi, Itqān, Révision active/passive; sépare validées, leçons en retard, révisions non effectuées, séance(s) du jour et historique non vérifiable. Aucune écriture de progression ni attribution de maîtrise. Quatre tests unitaires.
- `MainActivity.java`: bilan dans Parcours, sous la frise à sept jours; lecture seule de `DashboardLedger` et `WeeklyProgramPolicy`.
- `SettingsActivity.java` / `Ui.java`: agrandissement des petits textes secondaires BOOX et des lignes de réglages. Le libellé post-An-Nās «1 hizb · entretien» est remplacé par «Stabilisation · 1 hizb» pour ne pas confondre Itqān et Révision passive.
- `ItqanPostNasMaintenanceTest.java`: mise à jour de l'attente d'interface; protocole et source Itqān inchangés.
- Gardes de gel `scripts/guard_frozen_hifz_engines.sh` et `scripts/audit_hifz_quiz_against_1171.sh`: autorisent exclusivement la nouvelle classe de consultation `WeeklyBilan.java`; aucune permission nouvelle de changer les moteurs Hifz.
- Audit instrumenté étendu aux tests `HifzUiLayoutInstrumentedTest` et `MapQcfLifecycleInstrumentedTest`.

## Preuves exactes
- Source testée: `2c07c30eb5a963db316c783fd4a2bda96bd9ddc3`.
- JVM/source: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40092547897 — PASS. 820 tests, 0 échec, 0 erreur, 2 ignorés (166 suites XML).
- `WeeklyBilanTest`: 4/4 PASS.
- `M8TargetedLongitudinalRegressionTest`: horizons 180, 365, 548 jours PASS avec reçus persistés et contrôles de doubles crédits; simulation Java, PAS navigation physique BOOX de chaque jour.
- `LongHorizonHifzPersistenceStressTest`: 3/3 PASS, 1000 lignes, 52 semaines et redémarrages.
- Compilation/staging public SANS corpus privé: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40092547864 — PASS au même commit `2c07c30e`. Le fichier APK intermédiaire est volontairement NON INSTALLABLE comme candidat privé final.
- Android instrumenté: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40092439686 — 29/29 PASS, 0 erreur, 0 ignoré, APK sur émulateur avec QCF public. SHA `64fab65f0cb866ae14f1af0fa5592d70b67c7448`. Entre cette révision Android et `2c07c30e`, seuls le workflow de staging et l'attente du test JVM post-An-Nās ont changé; **aucun code Android applicatif**.
- Captures CI: accueil, Parcours, Parcours après défilement disponibles dans l'artifact du run Android. Les captures Pixel ne constituent pas une validation BOOX.

## Audit des périmètres préservés
- CP3 (masquage): `MushafPendingCommandsInstrumentedTest` et `AyahThemesReviewDisplayInstrumentedTest` PASS. Le démarrage atomique de Lecture avec masque précis existe; le moteur QCF n'a pas été modifié. Flash d'encre/ghosting physique BOOX non mesurable par l'émulateur.
- CP4 (Hifz): la cadence réelle, migrations, persistance, filière ×3 post-An-Nās, historiques, frontières et contrôles de double crédit restent couverts par les tests existants; seules les chaînes de présentation et le bilan ont été modifiés ici.
- CP5 (Atlas): code Atlas et corpus certifiés antérieurs non modifiés; tests statiques et cycle de vie des vues QCF PASS. Vérification visuelle complète des 114 sourates avec **sept ressources privées** impossible dans l'artifact public qui les exclut volontairement. Aucun changement des 366 Unités / 1 053 Passages.
- CP6: bilan implémenté; exercice de titres Atlas EXCLU.

## NO-GO restant pour paquet signé / BOOX physique
1. Incorporer les sept ressources Atlas privées et leurs SHA-256 sur un poste d'assemblage hors dépôt public.
2. Signer le candidat avec une clé appropriée; pour mise à jour directe de la 1.17.4 officielle, même appId et même certificat historique, plus versionCode supérieur à 42.
3. Vérifier l'installation réelle BOOX, les tailles native E-Ink, le ghosting, cinq cycles de masquage, retour Carte→Lecture, titres des 114 sourates et conservation des acquis.
4. Ne jamais présenter l'APK de staging GitHub comme un APK privé final. Aucun effacement de données, aucune publication officielle.

**Statut:** travaux CP2/CP6 et contrôles logiciel passés; **NO-GO release/validation physique**. Il ne s'agit pas d'une déclaration que les contrôles BOOX impossibles en CI ont été exécutés.
