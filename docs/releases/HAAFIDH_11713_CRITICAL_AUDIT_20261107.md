# Quran Haafidh 1.17.13 — audit critique sémantique, bibliographique et Hifz

**Date :** 7 novembre 2026  
**État :** candidat conditionnel. **NO-GO pour une livraison officielle tant que l'APK complet signé historiquement n'a pas été vérifié et soumis à un essai d'upgrade préservant l'historique.**  
**Base immuable de départ :** `3d8b16dd1a8869b87c0e0fdf2f2ef7359f7e9811` (PR #11, candidat 1.17.12 encore ouvert).  
**Identité candidate :** `com.quransafeguard.hifz`, `versionName=1.17.13`, `versionCode=51`.

## Correspondances : revue de la portée des preuves

- **62 fiches héritées**, dont **60 ADMIS**, **2 conditionnelles R032/R033 maintenues hors runtime**, 48 exclusions historiques préservées. Une admission supplémentaire R111 (2:113/2:118, attribution locale à Ibn ʿĀshūr) totalise **61 relations exposées**.
- R027 (93:6–8 / 94:1–4) : renforcement de la traçabilité par **al-Suyūṭī, Asrār tartīb al-Qurʾān, section Al-Sharḥ**, https://g1.islamweb.com/ar/library/content/223/73/سورة-ألم-نشرح. L'auteur rapproche explicitement 93:6 et 94:1. Cela **n'authentifie pas** la tradition citée et **ne prouve pas** l'équivalence terme à terme des listes de bienfaits.
- R091 (22:77 / 23:1–2) : renforcement par **al-Suyūṭī, Asrār tartīb al-Qurʾān, section Al-Muʾminūn**, https://g1.islamweb.com/ar/library/content/223/24/سورة-المؤمنون. L'auteur relie expressément la fin d'al-Ḥajj à l'ouverture d'al-Muʾminūn. L'app conserve la limite « aucun parallélisme global ni identité des prescriptions ».
- R011, R029, R036, R078, R096 : leurs descriptions reposent essentiellement sur des comparaisons textuelles observables, sans attribution savante spécifique à revendiquer. Attention à **ne pas convertir une similitude lexicale en preuve de symétrie/chiasme**.
- R032 et R033 : propositions de composition complexe **non certifiées**, exclues de l'application active.
- R036/R044/R053/R079 : liens qui recoupent des paires de Similitudes mais exposent un **mécanisme complémentaire** selon le registre de dédoublonnage. Ne pas compter un simple verset commun comme doublon.

## Similitudes (*mutashābihāt*) : portée et admissibilité

- Corpus audité : **295 décisions**, dont **280 actives** (201 « retenu » et 79 « corrigé »), 9 « à arbitrer », 6 « exclu ». Ne promouvoir ni les 9 arbitrages ni les 6 exclus sans preuve nouvelle.
- Les relations lexicales reposent sur l'ouvrage d'al-ʿAbbād, le texte arabe Tānzīl, des indices précis de mots et leur géométrie QCF ; distinguer texte de l'auteur, variation effectivement présente et écran mnémotechnique.
- Le couple n°62 (13:18 / 39:47) possède actuellement **des groupes pivots de taille différente (1/3 mots)** ; ce n'est pas en soi une erreur sémantique, mais le moteur doit respecter l'unité de comparaison plutôt que forcer une fausse symétrie. Aucun pivot ne doit être changé sans revue bibliographique.
- Délimitation : les similitudes de mots ne certifient pas de relations rhétoriques au niveau des sourates. Les comparaisons doivent rester navigables depuis les deux versets et les deux folios.

## Refrains et ergonomie

- Al-Qamar **4** repères (54:17,22,32,40), Ar-Raḥmān **31**, Al-Mursalāt **10** : frises avec texte coranique voisin, jalons en anglais (un/deux mots), références et positions.
- **Atlas sert uniquement de modèle visuel de frise** (ligne, points, anneaux de sélection), pas de source des mots-clés. Les cues éditoriaux ne sont ni des titres Atlas ni des citations savantes. Aucune symétrie globale déduite du nombre de répétitions.
- Correction purement linguistique : « maintiens de la semaine » → « maintien de la semaine » dans la Consolidation.

## Moteurs Hifz : vérifications exigées et preuves existantes

1. Sabqi et Itqān : jours et curseurs indépendants ; absences cumulées 1–8 semaines sans saut, recréditation ou déplacement d'une séance dans l'autre.
2. Finales Sabqi **moins de 15 lignes** et Itqān **moins de 22 lignes** possibles ; **zéro ligne admissible = pas de finale validable**.
3. Preuve physique des répétitions Itqān V7 : corrélation session-date-jeton-cursor-30 répétitions-reçu ; état illisible ou périmé = échec fermé, sans validation fantôme.
4. Révisions du soir : réception liée aux nouvelles unités effectivement gagnées dans la journée ; ne pas afficher « terminé » si la leçon du jour est encore non effectuée.
5. Revue active : double phase lecture/récitation masquée, reprise après dévoilement, force-stop et changement de date, aucune validation artificielle.
6. Historique et sauvegarde : conservation de progression lors de la migration in-place, signature historique et même applicationId.
7. Contrôler affichage portrait téléphone et simulation BOOX ; **simulation ≠ vérification physique d'un vrai BOOX**.

**Preuves de référence 1.17.12 :** Actions [QCF/Android téléphone et BOOX émulé](https://github.com/applicreation0-web/quran-hafidh/actions/runs/42355586844), [Hifz/Parcours Android](https://github.com/applicreation0-web/quran-hafidh/actions/runs/42355586802), [compilation et JVM](https://github.com/applicreation0-web/quran-hafidh/actions/runs/42355586850) : toutes **SUCCESS sur cette ancienne base**, non transférables automatiquement au commit 1.17.13.

**Vérification 1.17.13 :** cinq nouveaux workflows sur cette branche (runtime/QCF, Hifz/Parcours, P0 Itqān, source/offline, compilation code-only sans actifs privés). Leurs statuts sont à constater sur leurs SHA finaux ; ne pas assimiler « code-only unsigned » à une APK livrable. Tester le générateur `scripts/build_correspondences_catalogue.py` et refuser toute dérive des 60 admissions et 48 exclusions.

## Livraison : garde bloquante

Le nouveau script `scripts/build_11713_private_offline.py` est **un contrôle pour le montage OFFLINE autorisé**, non une preuve que l'assemblage a eu lieu. Il exige sept ressources privées, une APK historique authentiquement signée et la clé historique correspondante ; ni ces données ni cette clé ne doivent être versées sur GitHub. **Ne jamais installer l'APK de staging publique non signée.**

Après réussite complète des cinq CI sur le **même commit final** et après assemblage/signature privés, vérifier : identité APK `1.17.13/code51`, SHA-256 du fichier, certificat et signature APKv2, sept ressources intactes, installation in-place sans effacement, reprise Hifz sur appareil. Alors seulement publier une release nommée **« Quran Haafidh 1.17.13 — Release officielle »**, avec APK complet et somme SHA-256. Tant que cette chaîne n'est pas satisfaite, **NO-GO OFFICIEL** et PR en brouillon.
