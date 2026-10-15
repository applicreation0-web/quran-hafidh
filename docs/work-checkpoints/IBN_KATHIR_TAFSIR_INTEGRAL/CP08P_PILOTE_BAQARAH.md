# CP-08P — Pilote Al-Baqarah sur téléphone (2026-10-15)

## Verdict — livrable de prévisualisation, pas Tafsir intégral
- **PASS** : modèle 13 thèmes d'Ibn ʿĀshūr (Quran.com), UI graphique hub-and-spoke, passage vers les 173 blocs Ibn Kathīr sans retoucher leurs frontières, All Passages, surlignage et instance QCF préservés.
- **PASS** : onglet Tafsir dans le même écran par clic sur l'en-tête de l'amorce, panneau QCF inchangé, retour exact ; chargeur privé refuse toute entrée sans `group_id`/édition/provenance/hash corrects.
- **PASS** : compilation Android staging sans texte privé (CI `38714078142`), audit gel Hifz/Amorces (CI `38714078186`), 98 tests Android instrumentés + tests reprise après force-stop (CI `38714078193`). Captures Android : `audit-cp08p-baqara-13-themes.png`, `audit-cp08p-baqara-all-173-passages.png`, `audit-cp08p-baqara-tafsir-panel-return.png`.
- **PASS signature privée hors GitHub** : APK téléphone personnel `Quran-Haafidh-Al-Baqarah-PHONE-PILOT-Tafsir-Pending.apk`, SHA256 `a3a76157fa5d1923327d94dd3d8598dc25a1b32f51ef995f55a3550ccab92a17`, taille 68 220 972 octets, app ID distincte `com.quransafeguard.hifz.ibnkathirbaqarapilot`, signature v2 et alignement ZIP contrôlés ; 8 chunks privés, 1 708 titres, 195 sans titre, 28 adaptations sûres *Israelites*. Titres et clés conservés exclusivement dans un APK personnel local, JAMAIS dans CI / GitHub public.
- **NON EXÉCUTÉ** : test physique sur téléphone ou BOOX.
- **NON INTÉGRÉ** : texte intégral Darussalam d'Al-Baqarah (173/173). Le panneau signale loyalement cette indisponibilité ; zéro commentaire non vérifié intégré.
- **INTERDIT** : qualifier ce pilote de release officielle ou déclarer 173 commentaires validés.

## Audit documentaire M7 réutilisé
- Les 173 blocs Ibn Kathīr d'Al-Baqarah ont un identifiant et des frontières canoniques.
- La matrice M7 privée attribue des références de page à 144 blocs (141 `ARCHIVE_DJVU_XML+EXACT` et 3 `LINE_SUFFIX` demandant contrôle), tandis que 29 blocs n'ont pas encore de page localisée. La localisation d'un TITRE n'est pas une preuve que le commentaire complet est correctement délimité.
- Au moins une référence M7 appelle un contrôle de cohérence de pagination ; ne pas exposer une localisation comme « vérifiée » sans relecture du volume PDF original.
- Les PDF/OCR Darussalam complets ne sont pas présents dans le checkout public ni dans le paquet téléphone ; le texte intégral nécessite CP08A/B et un contrôle de droits avant distribution.
- Le corpus reste `1903` blocs, `1708` titres et `195` absences ; aucune promotion d'un bloc non titré sur simple suggestion.
- Seuls les titres de navigation peuvent être adaptés : 28 équivalences sûres ont été appliquées au paquet privé ; les titres avec `Jews` restent conservés sauf validation exégétique spécifique. Source/commentaire inchangés.

## Implantation
- `IbnAshurBaqaraThemes.java` : 13 thèmes, plages parfois disjointes et recouvrantes. Le premier intitulé anglais de navigation est clarifié, original Quran.com `The Opening Challenge` documenté séparément.
- `IbnKathirMapActivity.java` : graphisme hub/spokes, mode All Passages, interface Tafsir haut/QCF bas, aucun changement des moteurs Hifz.
- `IbnKathirFullCommentaryPrivate.java` : chargement local fail-closed, zéro contenu licencié dans Git.
- `IbnAshurBaqaraThemesTest.java`, `IbnKathirFullCommentaryPrivateTest.java` et `HifzAndMapRuntimeSmokeInstrumentedTest.java` : règles source + parcours Android.
- `.github/workflows/haafidh-cp08p-baqara-staging.yml` : compilation du code **sans** les titres/commentaires privés.
- `scripts/ci_android_runtime_visual.sh` : vérifie les trois nouvelles captures CP08P au lieu de la capture de l'ancienne liste.

## Reprise
1. Attendre le retour de l'utilisateur sur le téléphone (lisibilité du hub, taille QCF, sélection des thèmes, retour Tafsir).
2. Traiter les retours CP08P dans la même branche et rééditer un APK personnel distinct, sans supposer d'essais physiques PASS.
3. CP08A/B documentaire : retrouver dix OCR/scans Darussalam, vérifier droits, délimiter les 173 commentaires d'Al-Baqarah et corriger les sources/pages douteuses, par bloc et avec preuve. Ne jamais reconstruire du texte de commentaire par IA.
4. Une fois validé : réinjecter les contenus personnels hors GitHub public, signer, refaire Android + vérification UI réelle.
5. **La généralisation aux 114 sourates est hors portée de ce chat ; ouvrir un NOUVEAU CHAT après validation utilisateur.** Lire `HANDOVER_GENERALISATION_NOUVEAU_CHAT.md` avant de la lancer. Ni nouvelle branche ni nouveau projet.

HEAD de référence de build : `d3c85a8e5a574c7a05190906cbecaee9b5078462`. Le HEAD de documentation peut être ultérieur. Référence officielle 1.17.1 : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
