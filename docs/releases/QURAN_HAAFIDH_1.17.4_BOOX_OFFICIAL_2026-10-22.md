# Quran Haafidh 1.17.4 — BOOX privé officiel signé

Date : 22 octobre 2026. Source versionnée : `07114fb9981deb5e36249be9447bd09d50758c76`.
Code gelé sur `release/quran-haafidh-v1.17.4-boox-private-source`.
**Lignée Atlas + Hifz uniquement, sans Correspondances.**

## Correctifs suite aux captures réelles
1. Apprentissage — « séance 1/3 », « 2/3 », « 3/3 » était déjà calculé mais `MainActivity.compactScheduleLine` supprimait ces indices. La ligne condensée les conserve comme pour les quatre étapes Itqān. Tests explicites sur les trois états et la fin de semaine. Aucun statut « fait » inventé.
2. Révision active — ancienne activation de l'écran via `setPageLandmarkBoxes` et `setSemanticCues` sur une page déjà visible : le chargement réaffichait fugacement l'encre en clair et imposait plusieurs rafraîchissements BOOX. Correction dans `HifzSessionActivity` : les six repères et les amorces exactes sont préparés hors écran puis transmis au premier rendu via `showFreeActivePreparedPage`. La page entière reste cachée tant que son masque 100 % et ses trous de mots exacts n'existent pas. Absence de géométrie : fail-open, sans trou inventé.
3. Révision active — résidus de traces d'encre non couvertes en plein masquage : `reader.js/fullEraseWordSegments` s'appuyait sur le centre des cellules, manquant les mots/signes débordant leurs cellules. À 100 %, tous les mots exacts des lignes physiques concernées sont recouverts, avec marge géométrique de 1,15 unité. Les amorces protégées restent des trous exacts, les rosettes QCF restent inchangées, les paliers 25/50/75 % et leur mélange sont intégralement gelés.
4. Cadrage des amorces de la 1.17.3 : maintenu invisible et cliquable ; la géométrie originale n'est pas remplacée.
5. Révision active/passive : durées identiques 30 puis 45+ minutes selon acquis ; Itqān post-An-Nās un hizb ×3 visible-visible-masqué, maintien ×4 des séances déjà ouvertes ; moteur premier passage ×40 inchangé.

## Preuves d'audit
- Hifz/Atlas/QCF et sources figées : **2/2 jobs PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39863471951
- Compilation privée de sources exactes, ressources privées exclues de CI : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39863471882
- Audit Android élargi : **25/25 PASS**, 0 ignoré, 0 échec, dont masque/amorces des pages 6 et 11, V6, migrations et reprise après interruption https://github.com/applicreation0-web/quran-hafidh/actions/runs/39863471916
- App `versionName=1.17.4-AyahThemes114-PRIVATE`, `versionCode=42`; comparé aux références installables privées de même application et certificat 1.17.1(39), 1.17.2(40), 1.17.3(41).
- Inspection indépendante de ZIP : 703 entrées avant/après; 699 identiques, seules `AndroidManifest.xml`, `META-INF/version-control-info.textproto`, `assets/hifzreader/reader.js`, `classes.dex` diffèrent du BOOX 1.17.3 ; tous les corpus et pages QCF identiques.
- 604 folios QCF, 7 sources Atlas/AyahThemes SHA-256 contrôlées, aucun fichier Correspondances.

## APK privé signé hors GitHub
- Nom : `QURAN_HAAFIDH_BOOX_ATLAS_1.17.4_OFFICIELLE_PRIVEE_SIGNEE.apk`.
- Taille : **91 834 248 octets**.
- SHA-256 : `e0a6d60886273cc04485c99cf9bc9b4ca1a926344a3fc13a7dcccce62a15d9e8`.
- Identifiant Android : `com.quransafeguard.hifz.ayahthemes114private`.
- Certificat SHA-256 : `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.
- Signature Android : v2 et v3 **PASS** ; relecture du fichier signé et contrôle ZIP **PASS**.
- Compatibilité statique mise à jour depuis APK BOOX privés 1.17.1/1.17.2/1.17.3 : **PASS** (même paquet, même certificat, code supérieur).
- **Ne jamais** désinstaller la version existante, effacer ses données ou réinitialiser les acquis : sauvegarder puis installer par-dessus.
- **Validation physique BOOX** (non simulable par CI) : conservation des acquis, aucune apparition brève du corps, absence de traces résiduelles après cinq activations et changements de folios, lecture QCF, titres d'Atlas et repères hebdomadaires.

## Distribution
Le dépôt GitHub est public. Les 7 ressources documentaires privées et la clé de signature ne sont ni publiées ni ajoutées à une release GitHub. Un workflow GitHub public a précédemment rencontré **HTTP 403 / Resource not accessible by integration** à la création d'une page Releases ; rien ne permet d'en annoncer la publication. Ce document et une branche de source figée constituent les preuves publiques ; l'APK signé est livré uniquement au propriétaire dans cette conversation, sans redistribution publique de corpus.

**Verdict : audit automatisé PASS et APK privé officiel signé disponible ; recette E-Ink/installation physique encore à confirmer, et GitHub Release publique non créée.**
