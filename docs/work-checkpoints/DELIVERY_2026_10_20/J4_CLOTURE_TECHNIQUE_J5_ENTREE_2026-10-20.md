# J4 — Bilan de validation technique, puis entrée J5

Date : 20 octobre 2026. Dernier HEAD avant ce checkpoint : `feb044b843621d016d18770464dcbb4365fcd6cc`.

## Vérifié par CI, et non simplement annoncé

**[J4 QCF Refrain QA — exécution 39459122128](https://github.com/applicreation0-web/quran-hafidh/actions/runs/39459122128)** : SUCCESS sur les deux jobs :
- `Eight icons and read-only UI` : SUCCESS (contrats d'icônes et de non-écriture Hifz).
- `Refrain neighbors and QCF geometry` : SUCCESS ; `:hifz-app:testDebugUnitTest :hifz-app:sourceContractTest` : BUILD SUCCESSFUL.

**[Hifz native emulator regression — exécution 39460310500](https://github.com/applicreation0-web/quran-hafidh/actions/runs/39460310500)** au commit `feb044b843621d016d18770464dcbb4365fcd6cc` : SUCCESS sur émulateur Android ; 108 tests instrumentés, persistance externe `force-stop`, 32 captures de recette ; vérifie notamment `CorrespondenceNavigationInstrumentedTest` (comparaison synthétique sur pages QCF réelles, rendu, restauration des panoramiques A/B, rejets de lancements non approuvés), et le retour QCF/Atlas.

**[Ibn Kathir Hifz + Amorces audit ONLY — exécution 39460310372](https://github.com/applicreation0-web/quran-hafidh/actions/runs/39460310372)** : SUCCESS sur même HEAD.

Ce sont des contrôles sur la branche de travail ; aucune affirmation d'exécution sur la BOOX physique.

## Contrat J4 : fait dans le lecteur

- Feuillets du mushaf authentique Hafs/KFQC (604 SVG Brotli, récupérés et vérifiés par la CI), aucun mushaf recréé.
- **R047 / al-Qamar** : 4 positions de refrain ; contexte précédent / refrain grisé / suivant, reste atténué.
- **R048 / ar-Raḥmān** : 31 positions, rail de **5 pastilles coulissantes au maximum** selon Atlas, actif agrandi et double cercle. Sélection d'une occurrence = ses vrais feuillets QCF, avec les deux voisins normalement lisibles. Deux mots balises arabes adjacent + mot-clé facultatif, sans titre de tafsīr ni quiz.
- La borne historique, l'ID et les ancrages restent inchangés ; le contexte de lecture voisin n'est jamais promu en « preuve ».
- Même moteur visuel QCF pour comparaison, série et centre local exact ; pas de nouveaux moteurs Hifz, pas de score ni de double crédit.
- Contrôles négatifs explicites sur appariements, origine et fenêtres de pages. La grande structure centrale R032 et les quatre pôles de R078 ne sont pas certifiés par la seule capacité « deux pages ». Les cas non représentables sont verrouillés individuellement.

## Portes restantes : ne pas confondre avec une validation commerciale ou privée

**J4 technique : PASS pour le sous-ensemble de capacités ci-dessus, pas « 110/110 approuvées ».**
- `CorrespondenceRuntime.APPROVED_SHA256` est intentionnellement vide ; aucun `approved_v1.json` actif.
- Les dossiers historiques (80 documentés / 24 en attente / 6 écartés) restent privés hors dépôt public. « Documenté » != « certifié » ; les positions R047/R048 testées sur fixtures ne sont PAS intégrées au catalogue actif.
- Vérifications dossier par dossier encore requises : type démontré, limites/foyers exacts, sources, droits, format QCF affichable et QA.

**J5 Android émulateur : PASS** (voir exécution 39460310500). **J5 BOOX physique : NON TESTÉ** ; contraste E-Ink, vitesse, apparition d'un clignotement, lisibilité des deux feuillets, retour à la page active, signature compatible et conservation des données doivent encore être contrôlés sur appareil réel.

**Publication publique et release finale interdites** sans l'accord explicite de l'utilisateur. Aucun APK de livraison signé n'est produit par ce checkpoint.

## Reprise sans répétition

Poursuivre seulement : (1) validation des candidats strictement affichables sans surlignage trompeur, sans forcer les neuf dossiers non démontrés ; (2) contrôle Android/BOOX physique sur la vraie page ; (3) signer un APK debug privé compatible après les portes de sécurité, sans release publique.

Les nouvelles correspondances N001–N008 restent indépendantes et ne bloquent pas J5.
