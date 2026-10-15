# CP-08 — Intégration privée du Tafsir Ibn Kathīr intégral
État : **EN COURS — sources et architecture identifiées, extraction/lecteur NON EXÉCUTÉS**.
Date : 2026-10-15.

## Base gelée et invariants
- Dépôt unique : `applicreation0-web/quran-hafidh`.
- Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`.
- Fin du chantier précédent : `6a17d3e6485f0bf2d9a5288945a5aa494a396eb9` ; APK privé CP-06 signé SHA-256 `117c7e8a432803d3f1f75c97230bc3e116da0a72336f99563a1d815a7a759a09`.
- Cet APK, les checkpoints CP-00 à CP-07, les 1 903 frontières et les 1 708 titres sont conservés intacts.
- Aucun changement aux moteurs Hifz, aux règles du dimanche, à la mémorisation libre, à QUL, aux pictogrammes contractuels ni à la version officielle 1.17.1.

## Sources réutilisables (sans redownload préalable de 238 Mo de PDF)
- Archive originale OCR Darussalam, dix volumes : https://archive.org/details/tafsir-ibn-kathir-10.-Volumes
- L'archive expose dix fichiers `Tafsir_Ibn_Kathir_Vol._N_djvu.txt` (« FULL TEXT », OCR anglais, environ 10 Mo au total), les fichiers hOCR/ABBYY et les images des scans pour arbitrer les OCR difficiles.
- Rapport existant M7 : `M7_MATRICE_1903_FINALE.csv`, `M7_REFERENCES_DARUSSALAM.csv`, `M7_OCR_RAPPROCHEMENTS.csv` si disponible, et les checkpoints M7/M7-B. Les PDF antérieurement vérifiés et leurs SHA-256 sont dans `INVENTAIRE_IBN_KATHIR_DARUSSALAM_10_VOLUMES_2026-10-13.csv` (Library).
- Retrouver d'abord les caches, extraits et index déjà persistés ; télécharger uniquement les sources manquantes.

## Architecture attendue
1. Dans `IbnKathirMapActivity`, conserver `Sourate → bloc graphique → miniature QCF → Lecture → retour` et ajouter « Tafsir » sur le bloc sélectionné.
2. Nouveau lecteur **hors ligne**, anglais intégral, pleine hauteur disponible sur BOOX, texte défilant, titres non tournants, volume/page source visibles, bloc précédent/suivant et retour conservant le groupe, la page de miniature et le scroll.
3. Indexation **par identifiant canonique exact** `IKENsss_aaa_bbb` du `IbnKathirGroupIndex` existant ; ne jamais supposer que chaque groupe correspond à une page OCR entière. Respecter les 1 903 frontières intactes. En cas d'incertitude de découpage : état explicite « commentaire à vérifier » et référence de page, jamais un commentaire d'un autre groupe.
4. Contenu anglais intégral **dans un paquet personnel privé** généré et injecté localement ; GitHub public ne recevra que scripts, schémas, contrôles, empreintes, manifeste sans texte ni secret et code lecteur. Aucune traduction ou réécriture IA du commentaire.
5. Vérifier les conditions de réutilisation de l'édition Darussalam avant toute distribution ; la lecture sur Archive.org n'autorise pas automatiquement la redistribution du texte de traduction. Ne pas publier d'APK contenant les commentaires.
6. Chargement local asynchrone ou paresseux des commentaires longs, prise en charge de l'OCR incomplet, mémoire bornée, taille de police et contraste E-Ink. Pas de réseau demandé par Android ; pas de mutation de préférences Hifz.

## Checkpoints et preuves — ne pas sauter les étapes
- **CP-08A Sources** : disponibilité du texte OCR des 10 volumes, hashes, nombre de pages, comparaisons avec M7, liste des lacunes OCR. **NON EXÉCUTÉ**.
- **CP-08B Correspondances** : table `group_id → volume → PDF/OCR pages → start/end offset → fiabilité` ; assertion 1 903 IDs, doublons 0, gaps signalés, faux appariements bloqués. **NON EXÉCUTÉ**.
- **CP-08C Lecteur** : UI anglaise pleine hauteur + bouton Tafsir + groupe précédent/suivant + retour état exact ; tests contractuels et Android. **NON EXÉCUTÉ**.
- **CP-08D Paquet privé** : format, hashes, injection locale, contrôle absence de texte intégral dans Git/CI public. **NON EXÉCUTÉ**.
- **CP-08E APK et BOOX** : un nouveau APK privé signé **seulement si** corpus suffisamment validé et tests verts ; préserver le CP-06 final en repli. BOOX physique non exécuté si appareil absent. **NON EXÉCUTÉ**.

## Critères de qualité
- Ne jamais annoncer 1 903 commentaires intégraux avant vérification réelle de 1 903/1 903 correspondances et du texte OCR.
- Une mauvaise frontière documentaire ou un rattachement de texte à un mauvais passage est P0 : arrêter, corriger la correspondance, conserver les preuves.
- Ne pas relancer M7, M8 ou CP-00 à CP-07 : réutiliser leurs preuves et moteurs.
- Après chaque sous-CP, commit GitHub uniquement des sources/documentations publiques autorisées, rapport PASS/FAIL/NON EXÉCUTÉ et état de reprise. Rien uniquement dans /tmp.
- Les erreurs OCR non fiables seront signalées avec le volume et la page originale, sans reconstruction fictive.

## Reprise exacte
Retrouver d'abord les dix fichiers OCR texte depuis l'environnement précédent ou Archive.org, vérifier leur alignement avec M7 (volumes/pages), puis créer la première table d'appariement sur des groupes exemples (court/multipage/grande sourate) avant d'écrire une fonctionnalité écran dépendant de données inexistantes.
