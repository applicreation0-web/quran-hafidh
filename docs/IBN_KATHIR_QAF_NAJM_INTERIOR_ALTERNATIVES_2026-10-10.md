# J2 — Revoir les amorces *au sein du verset*

L'index des huit groupes de Qāf et des huit d'An-Najm est documentaire : une proposition de mot peut commencer **à l'intérieur** d'un verset, sans correspondre au début du groupe.

La première comparaison globale a identifié des débuts répétitifs (Qāf 50:12, 16, 31, 36, 39, 45 ; An-Najm 53:27 et 53:31). Il serait contre-productif d'allonger mécaniquement 50:39 jusqu'à **12 mots** simplement pour obtenir un préfixe mondialement unique.

**Neuf alternatives contextuelles** sont donc documentées, dont deux hypothèses dans 50:36 qui ne seront pas activées simultanément sans revue. Chaque suite est vérifiée directement sur les mots textuels de Quran.ws Hafs et les coordonnées QCF, et ses occurrences exactes sont comptées à l'échelle des 6 236 versets.

Sources Ibn Kathīr : https://quran.com/qaf/36/tafsirs/en-tafisr-ibn-kathir ; https://quran.com/qaf/39/tafsirs/en-tafisr-ibn-kathir ; https://quran.com/an-najm/27/tafsirs/en-tafisr-ibn-kathir ; https://quran.com/an-najm/31/tafsirs/en-tafisr-ibn-kathir .

Script : `scripts/audit_ibn_kathir_interior_cue_alternatives.py`. Sorties : `ik-j2-interior-cue-rarity.json` et `.md` dans les artefacts CI, jamais dans l'APK.

Statut **0 clés approuvées / pas d'activation**. La rareté n'est qu'un facteur ; sens, rappel, géométrie, proximité des passages et minimalité restent à trancher avant GO.
