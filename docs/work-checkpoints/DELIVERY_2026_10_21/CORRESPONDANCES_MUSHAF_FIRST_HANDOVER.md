# Correspondances — intégration du lecteur centrée sur le Muṣḥaf

Date : 21 octobre 2026. Branche : `work/haafidh-1171-atlas-correspondances`.
Base de comparaison avant refonte : `004a21c19b102ddefe68c6ec5d54012edbceb8c8`.

## Gel Atlas et Hifz

- SIX fichiers existants protégés par Git blob SHA-1 dans `scripts/guard_atlas_boox_visual_snapshot.py` : `MawdudiMapScreen.java`, `StudyReaderActivity.java`, `MapJalalaynDialog.java`, `MapQcfThumbnails.java`, `MapUnitMarkers.java`, `MushafView.java`.
- Contrôle imposé aux workflows d'intégrité et de staging BOOX. Les 23 icônes legacy et moteurs Hifz gardent leurs tests d'origine.
- Aucune édition de ces six fichiers n'a été réalisée pendant cette refonte.
- Le bouton Correspondances d'Atlas/Lecture/Sabqi ouvre maintenant le même écran que le menu d'accueil. L'origine (page, verset, snapshot opaque) est passée sans écriture des acquis.

## Nouveau parcours Android

- Nouvelle activité interne `CorrespondenceMushafActivity` (non exportée), ouverte depuis la page courante de Lecture (préférence existante, lecture seule).
- Première surface : authentique folio QCF 1–604 sans surlignage artificiel ; les dossiers par page sont recalculés en tournant les pages.
- Frise contextuelle à huit pictogrammes documentaires. Chaque symbole reste visible dans l'en-tête de la comparaison, avec type proposé ou vérifié.
- Comparaison : deux Muṣḥaf lorsque les plages sont sur deux pages différentes ; un Muṣḥaf lorsqu'elles sont sur le même folio. Feuilletage indépendant A/B.
- R047/Qamar : quatre occurrences ; R048/Raḥmān : trente et une occurrences, fenêtre de cinq repères, sélection et comparaison optionnelle ; fin de cycle → début de cycle.
- R007 progression : deux Muṣḥaf et renvoi Yūnus 10:71–73 ↔ Hūd 11:25–49, seconde plage multi-pages feuilletable.
- R078, Alternance hypothétique : quatre rôles A–B–A′–B′ pilotant les versets originaux 109:2,3,4,5 sur le même folio, sans certification littéraire.
- R033, Āyat al-Kursī 2:255 : Centre est affiché comme hypothèse éditoriale. Aucun surlignage au niveau de l'āyah entière ne prétend définir un centre. Le surlignage mot à mot reste fermé tant que les plages de mots et preuves ne sont pas vérifiées.
- Les relations approuvées conservent en priorité les ancres QCF du catalogue runtime dès que `CorrespondenceDocument.displayable` les accepte.
- Retour à la page d'origine, restauration du contexte de comparaison entre changements d'orientation ; source/justification consultée à la demande uniquement.

## Contrôle documentaire exécuté hors GitHub

- Archive originale privée `QURAN_HAAFIDH_BOOX_PACK_PRIVE_9_RESSOURCES_2026-10-21.zip`, aucune modification ni transmission au dépôt.
- `historical_110_v1.json` : SHA-256 `b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2`.
- Exactement **110 dossiers, 277 plages de versets** ; toutes les bornes vérifiées contre les longueurs canoniques des 114 sourates, **0 plage hors sourate**.
- Aucun identifiant de dossier ni source canonique modifié. Défaillance de digest, mauvais compte ou plage hors bornes : rejet complet du répertoire (pas d'activation partielle trompeuse).

## Conditions d'entrée dans un nouveau debug privé

1. CI Hifz/Atlas/QCF PASS, CI J4 QCF PASS, compilation Android privé-flag PASS et staging BOOX PASS **sur le même HEAD final**.
2. Tests et audits des pages de transition sourates et des plages multi-feuillets : R007, R014, R035, R047, R048, R051, R078, R033.
3. Test instrumenté Android du parcours, du retour Atlas/Lecture/Sabqi, des 8 icônes, 2 folios, surlignage et restauration.
4. Assemblage hors ligne des 9 ressources originales, signature historique compatible, vérification APK v2/v3, contrôle précis du catalogue.
5. Recette sur BOOX physique : proportion des pages, première/dernière ligne non coupée, échelle 1/2 folios, absence de flash et non-régression des acquis.

**Aucun APK n'est approuvé par la présente sauvegarde** ; les anciens APK ne comprennent pas cette refonte. Aucune release publique ou finale sans accord de l'utilisateur.
