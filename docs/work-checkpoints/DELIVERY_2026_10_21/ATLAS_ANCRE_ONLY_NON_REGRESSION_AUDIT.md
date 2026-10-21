# Audit de non-régression — Atlas et Hifz (21 octobre 2026)

## Directive utilisateur prioritaire

**Hors Correspondances, conserver Atlas et toutes les fonctionnalités telles que validées. La seule nouveauté graphique/terminologique autorisée est « Ancre » et son icône associée.** Aucun redesign, changement de titres, navigation Hifz, Mushaf, progression ou sources.

Ancre Atlas = `ic_ui_semantic_anchor`. Amorces historiques = `ic_ui_recall_key` (key-round inchangé). Ne pas faire de l'ancien libellé Amorces un alias qui modifie le pictogramme de ce parcours.

## Référence et contrôle binaire des sources

Base antérieure : commit `feb044b843621d016d18770464dcbb4365fcd6cc` (20 octobre).
Audit du code sur `ba13de8e337be82f582480ed9a5d334c4928ad45`.
Comparaison des SHA d'objets Git, octet pour octet : **identiques** sur ces fichiers :
- `MawdudiMapScreen.java` — carte graphique Atlas ;
- `MawdudiMapData.java` — chargement et catalogue d'unités/passages ;
- `StudyReaderActivity.java` — Lecture, ancres, passages et navigation ;
- `MushafView.java` — affichage QCF ;
- `MapJalalaynDialog.java` — tafsīr ;
- `MapQcfThumbnails.java` — miniatures ;
- `MapUnitMarkers.java` — repères d'Unités ;
- `HifzPrefs.java` — persistance ;
- `HifzSessionActivity.java` — séance ;
- `ItqanFourSessionPolicy.java` — Itqān ;
- `hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt` — moteur.

Différences historiques entre la base et la nouvelle branche hors Correspondances :
- `AtlasMapActivity.java` est le nouveau wrapper privé de l'écran `MawdudiMapScreen` préexistant, avec le même relais de retour `returnFromReader`, même sauvegarde et `back()`.
- `MainActivity.java` ouvre ce wrapper au lieu de l'activité documentaire héritée et ajoute l'accès Correspondances.
- `AndroidManifest.xml` enregistre le wrapper et les activités Correspondances en non-exporté.
- `Ui.java` ajoute le lien visuel de Correspondances ; l'ancien regroupement erroné « Ancre OU Amorce » a été corrigé (commits ci-dessous).

Ces adaptations de raccordement ne démontrent pas, à elles seules, une parité visuelle BOOX ; le même code `MawdudiMapScreen` et le même lecteur QCF sont cependant conservés.

## Régression avérée, corrective et tests

Une modification antérieure envoyait à tort `Afficher les amorces` vers `ic_ui_semantic_anchor`, au lieu de son `key-round` contractuel.

- `8671db1011665560f7516bb0ceaca6dd0417fdfe` — test rouge anticipé : séparer le rôle Ancre du rôle Amorces.
- `9c1f48b5907314589b293575d522c6600c41dfe3` — production : icône Ancre réservée à Ancre ; Amorces restauré sur key-round.
- `7e64147e195b62a72db90086c5f26fdb37a05234` — garde J6 mis à jour (l'ancien garde imposait par erreur l'alias).
- `494a9f3c9beb1c9c4c50c50f4df433db7af44c49` — tests du rôle singulier Ancre.
- `ba13de8e337be82f582480ed9a5d334c4928ad45` — test Atlas de la distinction de pictogrammes.

Les échecs CI intermédiaires sur `9c1f48b` provenaient du garde obsolète J6. Ne pas les dissimuler : tests mis à jour puis CI relancée. Les runs `39554668534` et `39554668546` étaient encore en cours lors de cet audit.

Avant ce correctif, la CI Hifz/Atlas/Correspondances, QCF et staging BOOX avait réussi sur `a7bcee677900b3a4781f4a3bb015d34d28b25ac0`, mais ne prouvait pas la conformité du pictogramme sur le dernier commit.

## Porte de livraison

Attendre les nouveaux résultats CI sur la dernière source, puis tests Android instrumentés et contrôle physique BOOX de l'apparence, du clignotement, de la restauration, de Jalalayn, des 604 pages et de la signature. Les essais physiques restent NON EFFECTUÉS.

Aucun moteur Hifz ou ressource privée ajouté. Aucun APK public, aucune release finale.
