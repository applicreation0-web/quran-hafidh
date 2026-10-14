# Chantier BOOX mot-à-mot — contrôle d'entrée

**Code public autorisé** : politique d’accès, sélection non ambiguë d’un mot QCF, tests et clés de localisation des amorces. Aucun moteur Hifz/compteur/masque/corpus Ibn Kathīr modifié.

**Corps anglais NON intégré** : les candidates 77 429 entrées ne prouvent pas l’alignement exact aux 77 432 positions QCF. Les cinq divergences à clarifier : 2:181, 8:6, 13:37, 15:7, 37:130. Aucun réalignement en cascade, aucune glose inventée.

**Droits NON établis** : le miroir CC BY-NC-ND ne suffit pas à démontrer les droits amont des traductions QuranWBW ; Quran Foundation impose une politique de Content Sync incompatible avec une absence permanente de réseau. Rechercher une **édition originale aux droits explicites** ou une autorisation écrite, puis produire une table validée 77 432/6 236/604 avant activation. Ne jamais copier un jeu anglais dans GitHub ou la CI publique tant que ces droits ne sont pas établis.

**Suite technique** : brancher le bouton Lucide search (24 dp, touche 48 dp) dans Lecture / Mémorisation libre / Sabqi / Itqān, sans alterer la pagination et avec deux barrières indépendantes `scheduledMask` et `displayedMask`. Blocage de la loupe si Révéler, état fail-open, rendu stale, passage de page, crayon. Aucun debug « mot-à-mot » tant que le glossaire n’est pas juridiquement sûr et complètement apparié.

**Tests actuels** : source contract et JVM des règles pures. Pas une preuve de fonctionnement tactile BOOX ni de la fonctionnalité complète.
