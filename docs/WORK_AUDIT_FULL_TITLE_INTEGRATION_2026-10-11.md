# WORK HIGH — AUDIT CONTRADICTOIRE AVANT DEBUG — QURAN HAAFIDH

**Strictement READ-ONLY. Aucun commit, aucune branche, aucun APK, aucune release.**
**Ne commencer qu'après le gel du dernier HEAD réel. Aucun ancien APK comme preuve du dernier HEAD.**

## Références à figer
- Dépôt public (code) : `applicreation0-web/quran-hafidh`
- Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`
- Référence officielle 1.17.1 : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Relever le SHA réel au démarrage, puis utiliser **exactement** ce SHA pour toutes les commandes, archives, captures et conclusions.
- Contrat de 23 icônes : `docs/FROZEN_UI_ICONS_13_PLUS_10.tsv`.
- Contrat graphique et moteur : standard fonctionnel et visuel déjà préparé + 133 cas de la matrice Work précédente (demander la pièce jointe si absente ; ne pas inventer 133 PASS).
- Paquet **privé joint au Work**, qui n'appartient pas au dépôt GitHub : `IBN_KATHIR_QURANCOM_TITRES_1903.csv`, `CONTROLES_ET_LIMITES.json`, huit `chunk-*.json` privés et le ZIP source Quran.com.
- Source unique autorisée à l'affichage des titres : Quran.com, Tafsīr Ibn Kathīr anglais, ressource API 169 ; aucune récupération de titre depuis spa5k.

## Faits vérifiables avant l'audit éditorial
- Index canonique de **1 903** groupes sur **114** sourates et **6 236** versets, SHA des frontières : `deffec6e775965d7b1f717a63fc4d0d60c1933540aacefc13e1333d30c49be62`.
- ZIP Quran.com fourni : SHA-256 `8b07799cbe02a7f907be84bbff2b09f95e19a319dffcf3a5458fd8365fa755b1`, manifeste **2 951 entrées HTML h2** depuis 114 sourates et 6 236 réponses de versets.
- Les h2 correspondent à **1 749 groupes** au moins ; **154 groupes sans h2**. La présence d'un h2 n'est PAS une validation éditoriale.
- Première passe conservative du paquet privé : **1 689 groupes provisoirement préparés**, **163 sans titre admissible**, **51 discordants**. Ces totaux sont les sorties automatiques du registre, pas un verdict de Work.
- **Ne PAS déclarer 1 689 titres validés**. Des titres h2 fusionnent parfois avec du commentaire, par exemple Al-Kahf 18:9 ou Qāf 50:1. Trois préfixes littéraux de h2 ont été isolés à titre **expressément provisoire**, avec preuve du texte original dans le CSV. Contrôler le contexte et le texte affiché.
- Quran.com Conditions développeurs actualisées 2026-10-04 : un usage strictement personnel ne prouve pas une licence d'archivage hors ligne illimité ; confirmer les droits et restrictions, notamment la durée de cache.
- Le dépôt GitHub est **public**. Aucune donnée de titres issue du ZIP ne doit être ajoutée au code public. Les huit chunks sont une **annexe privée**, destinée au contrôle du rendu via un checkout local jetable ; aucun upload public.
- NOUVEAU chemin d'affichage : `StudyReaderActivity.java` -> `IbnKathirQuranComPrivateTitles.sourceHeadings(context,group)`, fichier privé `assets/ibn_kathir_qurancom_private/chunk-NNN-NNN.json`. Les anciens `IbnKathirTitlePack` et `IbnKathirOfflineTitles` restent dans l'historique mais **ne doivent plus être utilisés à l'affichage**.
- Sans annexe privée, absence de popup = **comportement de repli voulu**, pas titre inventé. Un test sans paquet ne prouve pas l'intégration finale. Les tests Work doivent vérifier AVEC le paquet et la continuité SANS paquet.

## P0 — Source Quran.com, éditorial, coordonnées
1. Vérifier les **1 903 lignes du CSV** une par une ou avec des preuves automatisées solides, l'URL de chaque source, le chapitre, la position du verset, l'ordre des h2, les frontières et l'absence d'emprunt entre groupes. Ne pas mettre le statut « vérifié » sans source et contexte convaincants.
2. Contrôler obligatoirement Fātiḥah, 2:1, 2:25–27, 18:9–16, Qāf, Adh-Dhāriyāt, An-Najm, An-Nabaʾ, An-Nās. Séparer les titres véritables des préambules, vertus, citations, narrations et prose fusionnée. Titres anglais originaux non reformulés.
3. Vérifier **1 903 premières amorces**, deux premiers mots QCF réels du bloc avec les vrais cas de fin de sourate/page ; **77 432 zones**, **604 pages**, **6 236 versets**. Aucune nouvelle amorce intérieure ni modification des frontières. Amorces OFF conservé ; ON par défaut si aucune préférence.
4. Vérifier que les titres n'affectent ni boîte QCF ni masque de lecture, ni la validation de récitation, ni l'état de Hifz. Contrôler les 23 icônes gelées.
5. Les titres multiples doivent rester exactement dans leur propre groupe et être affichés **un à la fois par appui successif**, pas dans un gros pavé. Manque de titre = aucun dialogue.

## P0 — BOOX / interface
1. Comparer les interfaces accueil, Lecture, Mémorisation libre, Parcours Hifz, Progression, Apprentissage, Stabilisation, Renforcement, Consolidation, Révisions, Réglages, Planning, sélection de programme et Tafsīr aux captures validées ; aucun redesign.
2. Contrôler le surlignage gris clair **derrière les mots QCF exacts**, pas soulignement, ni rectangle massif ; Mushaf visuellement prioritaire.
3. Le titre 11,5sp regular centré LTR, largeur max 480dp, SANS encadré, 3,2s ; ENTIÈREMENT au-dessus du haut réel de la barre des icônes avec un écart de 4dp minimum, en portrait et paysage, texte court/long, titres multiples.
4. La nouvelle fenêtre démarre transparente puis devient visible **après** l'ancrage sur la barre mesurée ; pas de scintillement, de placement arbitraire ni de commandes recouvertes ; vérifier captures physiques BOOX si disponibles.
5. Commandes et 23 icônes exacts, 13 principales + 10 secondaires, cerveau pour Parcours Hifz, pas de libellés sous pictogrammes et pas de cartes énormes.

## P0 — Moteurs, planification, état V6
- Comparer à 1.17.1 et aux **deltas Itqān explicitement autorisés** seulement.
- Sabqi et ligne physique, 37 répétitions, 3 jours standard ; Itqān avant Nās : 2 pages / 4 dates, 40 répétitions, transitions pages/sourates, masques, validation ; après Nās : entretien et amorces.
- Stabilisation et reprise après fermeture, acquisition unique, éviter double crédit ; consolidation/réinforcement nouvel acquis ×7, ancien acquis continu ; éviter réapparition le lendemain après validation.
- Révision active + planning réel, aucune révision active ni générale dimanche mais séances Itqān et validation finale conservées ; programme standard 3/4, programme manuel **uniquement** ce que le moteur autorise. Simuler 8 semaines, semaine retardée, modification de programme, frontières Itqān et réouverture de sessions.
- Persistance V6, absence de perte d'acquis, restauration après arrêt de processus, rollback horaire, acquisitions/validations atomiques, séparation des profils et état des pages.
- Vérifier l'absence de Quiz au runtime si telle est la direction gelée du HEAD, sans réintroduire l'ancien code.

## Tests indépendants et preuves
- Vérifier gel des moteurs : `bash scripts/guard_frozen_hifz_engines.sh`, puis exécuter tous les scripts de contrôle de source, les tests JVM `:hifz-core:test :hifz-app:testDebugUnitTest :hifz-app:sourceContractTest` et les tests Android instrumentés ; **ne pas assembler ni publier d'APK**.
- Vérifier les résultats des CI à partir des runs du **même SHA**. Un vieux PASS n'est pas un PASS du HEAD.
- Audit de navigation, titres avec annexe privée et sans, orientation, écran petit et BOOX, titres multiples, popup sans chevauchement et changement de page.
- Produire `WORK_AUDIT_VERDICT.md`, `IBN_KATHIR_QURANCOM_REVIEW_FINAL.csv` (1 903 lignes), `MANUAL_8WEEK_SIMULATION.csv`, `ICON_SHA_CHECK.csv`, `HIFZ_REGRESSION_MATRIX.csv`, `NO_SIDE_EFFECT_EVIDENCE.md`, preuves de licences et captures adaptées.

## Verdict
- **NO-GO si P0** ou régression de progression, titres erronés présentés comme originaux, source/coordinate corrompue, ou droits bloquants pour l'usage visé.
- Classer chaque anomalie P0/P1/P2, fichier/ligne, scénario de reproduction et correction exacte.
- Ne déclarer GO que pour le **HEAD figé** et pour un **paquet privé identifié par son SHA**, jamais GO générique.
- Work ne doit PAS faire de commit, ouvrir de PR, modifier le dépôt, construire un debug, ni produire une release.
- Toute correction après Work => nouveau HEAD, tests relancés et relecture indépendante avant un **unique debug** distinct des précédents.
