# J2 — contre-épreuve des 30 positions Qāf / An-Najm

Le contrôle commence **après** l'audit complet des 1 903 groupes qui vérifie les 77 432 mots Quran.ws Hafs contre les 77 432 clés de QCF. Il réutilise exactement **les octets épinglés** du JSON déjà téléchargé et revérifie leur SHA256 `9b9eb07ff5cff144bf964400924e593d97115e4b937db3c61783f782a5168075`.

`scripts/analyze_ibn_kathir_qaf_najm_global_rarity.py` calcule pour **14 positions Qāf + 16 positions An-Najm** :

- la suite des mots arabes *réellement fournie par la source épinglée* ;
- le nombre d'occurrences du fragment envisagé à l'échelle des 6 236 versets ;
- le préfixe minimal qui distingue le début du verset de tout autre segment équivalent dans le Coran ; **aucun maximum arbitraire** (jusqu'à la fin du verset) ;
- les occurrences concurrentes avec leurs références ; les variantes éventuelles entre aperçu Quran.com et source Hafs ;
- le contraste indispensable entre 50:23 et 50:27 (deux rôles différents du mot `qarīn`).

Le résultat est récupéré comme **artefact JSON et Markdown dans la CI**, sans copier le texte complet du Quran.ws dans le dépôt ni dans l'APK.

**Le contrôle lexical ne sélectionne pas automatiquement l'amorce.** Il ne prouve ni sa valeur de déclic, ni le choix du meilleur mot initial, ni l'identité graphique du mot dans la fonte QCF. Toute différence documentée bloque l'approbation en attendant une contre-revue. `approved=0`, `runtime_ready=false`.

Étape suivante : revue contradictoire des résultats réels et décisions par amorce, notamment sur 50:23/27 et les répétitions du début de sourate. Pas de modification du moteur Hifz/Quiz ni de `SemanticPassageRepository`.
