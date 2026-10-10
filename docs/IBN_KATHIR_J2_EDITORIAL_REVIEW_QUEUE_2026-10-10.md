# J2 — Fiches éditoriales Ibn Kathīr, 2026-10-10

Cette étape réutilise **strictement** les trois dossiers de CI déjà vérifiés (1903 groupes, mots Quran.ws Hafs/QCF, et décision éditoriale NO GO). Elle ne modifie pas la segmentation.

Script `scripts/build_ibn_kathir_editorial_review_queue.py`.

- Fichier complet : **1 903 fiches** avec groupe IKEN, sourate, plage, premières/dernières pages, mot de départ QCF, voisins documentaires, collisions lexicales, différences textuelles et référence du fichier primaire anglais au commit épinglé.
- Échantillon contradictoire **109 groupes**, avec tous les groupes de Qāf, des exemples Baqarah/Najm/Dhāriyāt/An-Nās, préfixes longs, groupes multipages et répétitions.
- Les brouillons lexicaux sont des **aides de revue**, jamais des clés validées.
- Toutes les fiches restent `UNREVIEWED`, `approved=0`, `runtime_ready=false`. Les champs de justification, coordonnées sélectionnées et droits sont vides.
- Pas de reprise des commentaires Darussalam anglais : droits non établis. Le lien vers `tafsir/en-tafisr-ibn-kathir/<surah>.json` pointe la source documentaire exacte sans invention.
- Les catégories `NO_KEY_REQUIRED`, `ONE_OR_MORE_VERIFIED_KEYS` et `BLOCKED` restent des issues POSSIBLES, jamais des décisions automatiques.
- Aucune ressource éditoriale passée dans l'APK ; aucune modification de `SemanticPassageRepository` ou des moteurs.

**Bloquants :** `IKEN002_047_047` (2:47/2:122), `IKEN015_006_009` (15:7), droits de reproduction Darussalam, preuve indépendante d'un déclic mnémotechnique par clé.
