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

## Architecture attendue — CONTRAT VISUEL UTILISATEUR DU 15 OCTOBRE (prioritaire)
**Le Tafsir intégral s'affiche DANS la carte actuelle, sans écran séparé de type Jalālayn et sans remplacer le Muṣḥaf.** La précédente idée de lecteur plein écran indépendant est annulée.

1. **Déclencheur exact :** toucher l'en-tête de l'amorce sélectionnée au-dessus de la miniature QCF (exemple réel : bloc 89, Al-Baqarah 2:148, « Every Nation has a Qiblah »). Le toucher d'un autre nœud continue de sélectionner le bloc ; toucher la miniature continue d'ouvrir Lecture. Éviter toute collision de gestes.
2. **Téléphone portrait :** conserver la disposition verticale actuelle de `IbnKathirMapActivity`. Remplacer uniquement la zone **supérieure** de la liste/carte de blocs par le commentaire anglais intégral du groupe sélectionné, avec titre fixe, plage de versets et référence du volume/page si vérifiée. Le panneau **inférieur** du Muṣḥaf QCF ne bouge pas : même `MushafView`, page, surlignage, sélection, état et dimensions ; pas de recréation de `MushafView` lors du basculement.
3. **BOOX :** conserver la stratégie de rendu Tafsir côte à côte déjà appréciée dans l'application BOOX, si la taille/orientation le permet : colonne commentaire défilante indépendante et colonne Muṣḥaf visible. Si le rapport largeur/hauteur ne permet pas cette présentation sans nuire au Muṣḥaf, basculer sur la même présentation superposée que le téléphone. **Ne jamais cacher le Muṣḥaf, ni superposer le Tafsir sur ses versets.** Ne pas supposer qu'une émulation vaut validation E-Ink physique.
4. **Retour :** flèche explicite « Retour à la carte » dans la seule zone commentaire ; elle restaure exactement sourate, groupe, page et scroll de la carte, sans recharger la page QCF. Le bouton système Retour en mode commentaire a la même sémantique. Lorsqu'on quitte Lecture pour la carte, conserver le comportement CP-03 déjà PASS.
5. **Texte long :** défilement propre à la zone du commentaire, police lisible, paragraphes et contrastes sobres E-Ink, aucune limite arbitraire du texte ; si l'OCR/segment est incomplet, afficher un état d'indisponibilité traçable, sans inventer un texte. Ne pas lancer une nouvelle Activity Tafsir distincte.
6. **Tests CP-08C obligatoires :** ouverture par le seul en-tête, bascule d'un panneau unique, scroll indépendant, appui Retour, état carte exact, instance QCF inchangée, 2:148, bloc court, bloc multipage, rotation/taille pertinente, persistance après recréation, absence de modification des moteurs et du corpus de révision, absence de nouvelle permission réseau.
7. **Indexation **par identifiant canonique exact** `IKENsss_aaa_bbb` du `IbnKathirGroupIndex` existant ; ne jamais supposer que chaque groupe correspond à une page OCR entière. Respecter les 1 903 frontières intactes. En cas d'incertitude de découpage : état explicite « commentaire à vérifier » et référence de page, jamais un commentaire d'un autre groupe.
4. Contenu anglais intégral **dans un paquet personnel privé** généré et injecté localement ; GitHub public ne recevra que scripts, schémas, contrôles, empreintes, manifeste sans texte ni secret et code lecteur. Aucune traduction ou réécriture IA du commentaire.
5. Vérifier les conditions de réutilisation de l'édition Darussalam avant toute distribution ; la lecture sur Archive.org n'autorise pas automatiquement la redistribution du texte de traduction. Ne pas publier d'APK contenant les commentaires.
6. Chargement local asynchrone ou paresseux des commentaires longs, prise en charge de l'OCR incomplet, mémoire bornée, taille de police et contraste E-Ink. Pas de réseau demandé par Android ; pas de mutation de préférences Hifz.

## Langue et terminologie anglaise — décision utilisateur (2026-10-15)

- La carte globale (rubriques adaptées d'Ibn ʿĀshūr) et les titres d'amorces Ibn Kathīr restent **en anglais** ; ne pas imposer une traduction française dans ces vues.
- Pour l'affichage éditorial, privilégier **“Israelites”** lorsqu'une rubrique se réfère spécifiquement à *Banī Isrāʾīl*, aux Enfants d'Israël ou à leurs descendants dans un contexte historique. Envisager de remplacer un “Jews” déjà employé seulement si la source du passage rend cette formulation historiquement et exégétiquement fidèle.
- Ne **jamais** substituer automatiquement “Israelites” à “Jews” : les deux termes ne sont pas toujours synonymes. Lorsque la source mentionne spécifiquement *al-Yahūd* / une communauté religieuse juive, conserver **“Jews”**. Lorsque le Coran renvoie aux détenteurs de l'Écriture, conserver **“People of the Book/Scripture”** si ce terme est le plus fidèle.
- **Ne modifier aucun texte original** d'Ibn Kathīr, d'Ibn ʿĀshūr ou du commentaire OCR. Conserver les chaînes originales, leurs références et leur provenance ; si un intitulé de navigation est adapté, utiliser un champ distinct `display_title_en` ou une table d'overrides sourcés (ID du bloc/thème, original, adaptation, justification, référence).
- Contrôler en priorité les titres sensibles dans les premières amorces (audit CP-08F). Pas de changement global automatique, pas de titre de remplacement non vérifié. Les contenus éditoriaux privés ne doivent pas être committés en clair sur GitHub public.
- Les treize rubriques globales d'Al-Baqarah dérivées de Quran.com / Ibn ʿĀshūr sont des thèmes **pouvant se chevaucher**, et non un découpage exclusif en treize plages successives ; conserver les références des versets et la distinction entre citation exacte et formulation de navigation.

## Checkpoints et preuves — ne pas sauter les étapes
- **CP-08A Sources** : disponibilité du texte OCR des 10 volumes, hashes, nombre de pages, comparaisons avec M7, liste des lacunes OCR. **NON EXÉCUTÉ**.
- **CP-08B Correspondances** : table `group_id → volume → PDF/OCR pages → start/end offset → fiabilité` ; assertion 1 903 IDs, doublons 0, gaps signalés, faux appariements bloqués. **NON EXÉCUTÉ**.
- **CP-08C Panneau Tafsir intégré** : clic sur l'en-tête d'amorce, remplacement du panneau carte **uniquement**, Muṣḥaf inchangé, retour conservant la carte, BOOX côte à côte si applicable ; tests comportementaux et Android. **NON EXÉCUTÉ**.
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
