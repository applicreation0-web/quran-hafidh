# Quran Haafidh 1.17.9 — reste réel et reprise sans duplication
Date du constat : 2026-10-31. Rapport de diagnostic indépendant et **sans publication**.

## Référentiel exact
- Dépôt : `applicreation0-web/quran-hafidh`.
- Base GitHub étudiée : `work/haafidh-1179-pivots-review-20261031`, SHA `cd9c993b9d1fef9de446e22f23b71bb4d21f407a`.
- Base de tests Android : `work/haafidh-1179-finalisation-291-20261030`, SHA `a4f440c88893d173f001829a0167294fcff56b34`.
- Branche Hifz préservé : `work/haafidh-1179-preserved-regression-20261031`, SHA `326c64ace61762ae226fa812cf286966d95c5929`.
- Correctifs complémentaires strictement **test-only** : `fix/haafidh-1179-preserved-test-fixture-20261031`.
- La session Work de fin de matinée du 31 octobre indique un correctif local supplémentaire SVG/QCF, mesures de hauteur, glyphes, clipping et garde CI, **non visible sur ces HEAD GitHub**. Préserver / committer / comparer son diff local AVANT toute intégration ; ne jamais écraser ce travail.

## Conserver les acquis — NE PAS recommencer
- Source éditoriale unique : al-ʿAbbād. Le JSON GitHub à cette base enregistre 294 dossiers : 278 retenus/corrigés, 10 en statut éditorial `à arbitrer`, 6 exclus.
- La décision plus récente de Work du 31/10 a admis le cas **62, 13:18 ↔ 39:47**, portant l'état candidat déclaré à **279 / 9 / 6**, non encore certifié dans GitHub. Les dix dossiers ont déjà fait l'objet d'une orientation éditoriale V2 dans `docs/audit/HAAFIDH_1179_PIVOTS_CONTEXTUELS_V2_20261031.md`. Ne pas redemander les arbitrages à l'utilisateur : retrouver les décisions finales de Work et effectuer seulement le rapprochement données ↔ décision ↔ QCF.
- Le projet dispose déjà de l'index de correspondances par page, des deux pages QCF, des outils de mots pivots, du panneau al-Jalālayn, de la machine d'état de reprise des pages dévoilées, des protections de dates/temps/curseurs, des suites JVM, des contrats, et de l'audit des 556 côtés des 278 comparaisons contre les coupures d'encre. Les cas 120,124,142,145,147,149,161,185,197,198,199,293 sont déjà documentés pour la revue locale des repères et des omissions.
- GitHub Actions `41176863950` : JVM catalogue PASS ; tests Android de deux profils FAIL (chacun deux tests), partitions exhaustives du catalogue **SKIPPED** après cet échec.
- GitHub Actions `41177487345` : 52 tests préservés, 1 FAIL. Attestation de certificat historique `41175865964` : PASS. Aucune preuve d'un APK signé officiel installable en mise à jour n'est fournie.

## Diagnostics ciblés et corrections réelles apportées sur branche indépendante
1. `PostFinalRecallInstrumentedTest.corruptFrozenItqanCannotEraseRealMondayCalendarOrSevenDates` — un test initialisait les préférences au jour courant, puis projetait la semaine du 26/10/2026 sans avoir fixé la date de démarrage ni la cadence recommandée. Les autres tests de calendrier utilisent cette fixture historique. **Correction test-only** : rétablir dans la fixture l'état initial daté et la transition RECOMMENDED ; SHA `ce4cb7ab87815bd2c404bcfa16056c4f24a8639a`. Aucun moteur / donnée utilisateur modifié ; rerun nécessaire.
2. `RevisionAtlasInstrumentedTest.activeHasNoHintUntilRealValidationAndKeepsMaskAcrossRecreation`, smartphone — dans le log, le dialogue montre réellement `VALIDER QUAND MÊME`, tandis que le test attend une égalité sensible à la casse avec `Valider quand même`. **Correction test-only** : recherche compatible `textAllCaps`, comparaison `equalsIgnoreCase` ; SHA `78b777e034f3fe2f4b1638f92160362724aaef46`. Rerun nécessaire.
3. Même test sur profil BOOX — l'arbre d'accessibilité se termine sur `Pixel Launcher isn't responding` (boutons système `Close app` / `Wait`). **Anomalie d'émulateur/fiabilité CI à isoler**, pas une preuve de corruption Hifz. Ne pas masquer un ANR applicatif potentiel en supprimant les assertions.
4. `AtlasDiscriminantInstrumentedTest.mushafTapOpensContextualTafsirAndSwitchPreservesBothQcfPages` — les Rect des deux pages ont des dimensions non nulles sur les deux profils, mais le garde `waitForStablePageBounds` ne franchit pas les conditions cumulées (focus, insets, orientation, trames, visibilités). La condition qui bloque **n'est pas encore identifiée**. Instrumenter exactement les booléens d'échec, séparer les tests du dialogue/du focus, et exécuter l'écran isolément avant de retoucher la géométrie. Comparer d'abord avec le correctif local Work du 31/10.

## Contrat interface restant à CERTIFIER (sans refaire les maquettes)
- Deux pages QCF originales, ratio 345:550 et indices de mots véritables ; pas de texte arabe recomposé ni de fragments de glyphes coupés.
- Mots/groupes discriminants agrandis en police QCF, contraste lisible sur BOOX, sélection stricte des mots pertinents, omissions « présent / absent » sans faux mot substitué, plusieurs pivots lorsque nécessaire, brèves glosses françaises revues.
- Tafsīr ouvrable par **verset exact** depuis chacun des deux Mushaf ; dans le dernier code poussé, `MapQcfThumbnails.Listener.onPageChosen(int page)` ne sélectionne que le **côté**, et `selectTafsir(side)` résout `current().first/second`. Cela ne prouve pas un vrai ciblage par verset dans la page. Vérifier d'abord le patch local Work (travail polygonal annoncé), puis exiger un test de clic sur un verset différent sur la même page.
- Le panneau doit être fermable, revenir au contexte exact et ne pas modifier les deux QCF ni le chronomètre de Révision passive. Aucun indice / dévoilement durant la restitution active pré-validation. Relecture corrective visible + rappel masqué complet si page dévoilée ; aucun crédit/receipt artificiel.
- Les captures de l'ancien SHA montrent les interfaces réellement rendues, **pas** le contrat final ni la latence E-Ink physique.

## Tests minimaux à rejouer, dans cet ordre
1. Conserver tout diff Work local non committé ; rapprocher les deux correctifs tests ci-dessus par cherry-pick **seulement si absents**.
2. Rejouer séparément les trois échecs nommés et la suite `ActiveReviewCorrectionInstrumentedTest`, sur téléphone et profil BOOX ; conserver traces de focus, insets, ANR et orientation.
3. Rejouer les 52 tests de schéma/finales/restauration de `41177487345` ; confirmer la conservation des données en présence de snapshots Itqān malformés, sans modifier les initialisations.
4. Exécuter JVM / contrats / index / provenance / 604 QCF / contrôles raster, puis les partitions Android du **catalogue entièrement approuvé** pour chaque profil. Les partitions du dernier workflow étaient SKIPPED ; ne pas les compter comme réussies.
5. Relancer crash/recréation/force-stop après dévoilement, arrivée d'une nouvelle date, dialogue, restauration/export/import v3, presse rapide, interruption longue, orientations multiples, mémoire basse et chronomètre en pause ; comparer crédits, acquisitions et finales avant/après.
6. Auditer captures BOOX et smartphone sur mots QCF, Tafsīr, masques et coupures. L'émulation ne certifie pas à elle seule le ghosting et la rémanence E-Ink réelle.
7. Examiner séparément la vraie signature officielle et un upgrade **sur place** de la 1.17.8/1.17.4 installée, sans désinstallation et sans publication. Le certificat local non officiel doit être exclu ; ressources privées restent hors Git.

## Portes de publication
**NO GO** tant que les tests Android restent rouges, que le catalogue candidat n'est pas figé et certifié, que les sauvegardes/finales/crédits n'ont pas leurs preuves instrumentées, que les captures finales ne sont pas acceptables et que le certificat / upgrade privés ne sont pas vérifiés. Pas d'APK public, pas de changement de moteur Hifz ou des initialisations pour contourner un test. Aucun GO implicite.

## Sources vérifiables
- Android + JVM : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41176863950
- Préservation finales/schéma : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41177487345
- Attestation : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41175865964
- Comparaison des deux correctifs tests : https://github.com/applicreation0-web/quran-hafidh/compare/cd9c993b9d1fef9de446e22f23b71bb4d21f407a...78b777e034f3fe2f4b1638f92160362724aaef46

Ce document est un état de reprise, **pas** un certificat de réussite, et n'autorise pas de release.
