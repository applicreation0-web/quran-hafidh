# Audit bloquant — Quran Haafidh 1.17.1 Ibn Kathīr
Date : 2026-10-10. Statut : **NO GO APK** tant que tout Ibn Kathīr n'est pas intégré.

## Politique de livraison imposée
Aucun nouvel APK de test, lien d'artefact APK ou release à fournir à l'utilisateur avant :
1. Remplacement exhaustif et sûr d'Al-Munīr par Ibn Kathīr, pour les **amorces ET la Carte**.
2. Vérifications de non-impact des moteurs Hifz, sauvegardes et Quiz.
3. Exécution complète des tests et audit contradictoire des cas limites.
4. Accord explicite pour un seul candidat test intégré.

Le workflow `ibn-kathir-engine-frozen.yml` est désormais **audit-only** :
aucune étape `assembleDebug`, aucun upload d'APK, aucun lien d'installation.

## Base et méthode
- Référence officielle immuable : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21` (Quran Haafidh 1.17.1).
- Branche de travail : `work/haafidh-1171-ibn-kathir-integration-independent`.
- Audit statique des fichiers par Git blob SHA / `git diff` sur l'intégralité des arbres.
- Tests CI : `:hifz-core:test`, `:hifz-app:testDebugUnitTest`, `:hifz-app:sourceContractTest`; tests de géométrie/cues et gel de glyphes.
- Aucun test physique BOOX ou téléphone réalisé dans cette mission d'audit.

## Résultats confirmés
| Surface | Comparaison à 1.17.1 | Portée |
|---|---|---|
| `hifz-core/` | Byte-identique (5 fichiers du dépôt) | Moteur et tests sources |
| `HifzSessionActivity.java` | Identique (Git blob) | Pilotage Hifz |
| `HifzPrefs.java` | Identique (Git blob) | Progression/sauvegardes |
| `AnchoringQueue.java` | Identique | Ancrage technique Itqān |
| `MushafView.java` et `hifzreader/` | Identiques | Rendu Quran, masques/interaction |
| `QuizActivity.java`, `QuizCorpus.java`, `QuizQuestion.java`, `QuizHistory.java` | Identiques | Logique et historique Quiz |
| `app/src/main/assets/reader109/geometry.json` et `waqf.json` | À comparer systématiquement en CI | Source géométrique |
| Corpus Quiz | Le même calcul dynamique `learned ∪ stabilized ∪ acquired` que 1.17.1 | Versets éligibles seulement si toutes leurs lignes le sont |
| Tests CI avant nouvelle restriction audit-only | SUCCESS, run `38033470323` | JVM et contrats verts, sans preuve physique |

Des scripts additionnels `audit_hifz_quiz_against_1171.sh` et `guard_frozen_hifz_engines.sh`
rejettent la modification de code critique ; une nouvelle régression
`QuizCorpusTest.corpusGrowsAcrossWeeksFromLiveProgressionWithoutRecreditOrMutation`
éprouve la croissance hebdomadaire sans changer `QuizCorpus`.

**Attention :** fichier inchangé = preuve statique ; la fonctionnalité n'est pas certifiée
sur matériel tant que l'ensemble intégrée n'a pas été éprouvé.

## Blocage majeur : Ibn Kathīr encore incomplet
1. `IbnKathirGroupIndex` livre 1 903 groupes de commentaires numériques couvrant 114 sourates et 6 236 versets.
2. `IbnKathirMapActivity` livre une liste et une miniature du Mushaf ; la carte sémantique riche précédemment spécifiée (hiérarchie, fiche structurée, localisation multipage et représentation du bloc) n'est pas complètement réalisée.
3. `SemanticPassageRepository` est encore l'**ancien dépôt Al-Munīr** ; il dépend de `semantic_passages_v2_1.json`, `semantic_titles_v2_3.json` et leurs sources.
4. `StudyReaderActivity` active encore ses amorces depuis ce dépôt et `SemanticTitlePopup` utilise ses titres.
5. `HifzSessionActivity.applyItqanAnchors()` consomme directement les anciens repères à des fins de rappel post-An-Nās, avec une politique « fail-open » en cas d'absence de géométrie exacte. **Ne pas modifier le moteur !**
6. Le fichier `quranic_cues_v1.json` généré en parallèle reproduit fidèlement 1 243 repères historiques, mais **ce n'est PAS l'implantation des repères Ibn Kathīr** et ce sidecar n'est pas le fournisseur d'amorces du runtime.
7. Les groupes `IKEN...` n'ont pas encore tous des amorces coraniques sélectionnées, vérifiées géométriquement et auditables. Ne pas imposer mécaniquement « une clé par groupe » sans confirmation des mots.
8. Les droits et titres/préambules anglais originaux restent à établir : pas de texte religieux inventé ni de commentaire protégé ajouté par défaut.

### Migration attendue (interdiction de modifier les moteurs)
- Construire un référentiel Ibn Kathīr vérifié, partagé par **Carte** et **clés amorce**.
- Employer une couche d'adaptation conservant `SemanticPassageRepository` **comme API** si nécessaire aux appels actuels Hifz, mais remplacer ses données et titres historiques. Garder les identifiants `SP...` seulement en alias de migration contrôlée, jamais comme référentiel éditorial.
- Garantir la sécurité des amorces des répétitions Itqān post-Nās : exactitude des word boxes, masques validés ; sinon page lisible (fail-open).
- Ne toucher à aucun moteur ni à `HifzPrefs`, `MushafView`, `QuizCorpus` ou `QuizActivity`.
- Retirer les fichiers Al-Munīr et les scripts historiques de **la production de l'APK**, après preuve d'équivalence fonctionnelle.
- Une Carte consultée ne déplace pas la progression, le signet ni le corpus Quiz.

## Matrice de réception Hifz / Quiz avant le premier nouvel APK
**Tests automatisés obligatoires**
- Intégrité 114 sourates, 1 903 groupes, 6 236 versets et géométrie réelle (incl. Qāf, multipage).
- Parité des fichiers protégés vs 1.17.1 et absence de ressources éditoriales anciennes dans les assets.
- Sabqi : même durée, validations, lignes et échéances ; aucun effet sur moteur.
- Itqān : répétitions, calendrier, post-An-Nās, fiabilité de l'amorce / fail-open et masque 100 %.
- Consolidation / Renforcement / Révision active : moteurs, comptes, dates, repères faibles inchangés.
- Quiz : 10 questions ; modes Mixte/Continuer/Précédent ; contexte réel QCF, masquage, correction, audio temporaire supprimé, histoire et option d'ajout de faiblesse.
- Quiz dynamique : après ajout de nouvelles lignes en semaine N+1, une nouvelle série relit la progression, et les anciennes restent éligibles.
- Navigation Carte ↔ lecture : restaurations exactes de pages, défilement et signets.
- Sauvegarde/restauration, migration, cas de données anciennes, orientation téléphone/BOOX.

**À réaliser ensuite sur une candidature unique intégrée, avec autorisation de l'utilisateur**
- Parcours réel Hifz et Quiz sur téléphone/BOOX, notamment ordre des masques, lecture E-Ink et maintien de la progression entre sessions.
- Vérification de l'identité/signature Android et du non-remplacement de l'installation officielle.

## VERDICT
- Conformité des moteurs à l'état source 1.17.1 : **PASS statique**.
- Tests automatiques sur la dernière CI achevée : **PASS** ; test hebdomadaire et audit officiel ajoutés ensuite, à vérifier à la prochaine CI.
- Non-impact fonctionnel Hifz/Quiz après migration Ibn Kathīr : **NON TESTABLE EN L'ÉTAT**, car migration non réalisée.
- Ibn Kathīr complet (repères + carte) et purge définitive Al-Munīr : **FAIL / INCOMPLET**.
- Nouveau debug ou release : **NO GO**.

Tout verdict GO nécessite les trois niveaux de preuve : invariance du code moteur, tests comportementaux, essais réels validés.
