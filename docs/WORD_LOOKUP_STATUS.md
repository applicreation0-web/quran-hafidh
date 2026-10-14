# Chantier BOOX mot-à-mot — contrôle d'entrée

**Code public autorisé** : politique d’accès, sélection non ambiguë d’un mot QCF, tests et clés de localisation des amorces. Aucun moteur Hifz/compteur/masque/corpus Ibn Kathīr modifié.

**Corps anglais NON intégré** : les candidates 77 429 entrées ne prouvent pas l’alignement exact aux 77 432 positions QCF. Les cinq divergences à clarifier : 2:181, 8:6, 13:37, 15:7, 37:130. Aucun réalignement en cascade, aucune glose inventée.

**Droits NON établis** : le miroir CC BY-NC-ND ne suffit pas à démontrer les droits amont des traductions QuranWBW ; Quran Foundation impose une politique de Content Sync incompatible avec une absence permanente de réseau. Rechercher une **édition originale aux droits explicites** ou une autorisation écrite, puis produire une table validée 77 432/6 236/604 avant activation. Ne jamais copier un jeu anglais dans GitHub ou la CI publique tant que ces droits ne sont pas établis.

**Suite technique** : brancher le bouton Lucide search (24 dp, touche 48 dp) dans Lecture / Mémorisation libre / Sabqi / Itqān, sans alterer la pagination et avec deux barrières indépendantes `scheduledMask` et `displayedMask`. Blocage de la loupe si Révéler, état fail-open, rendu stale, passage de page, crayon. Aucun debug « mot-à-mot » tant que le glossaire n’est pas juridiquement sûr et complètement apparié.

**Tests actuels** : source contract et JVM des règles pures. Pas une preuve de fonctionnement tactile BOOX ni de la fonctionnalité complète.


## 14 octobre 2026 — piste source primaire examinée

**Quranic Arabic Corpus (Université de Leeds, Kais Dukes)** est une piste plus sérieuse qu'un miroir sans chaîne de droits claire : https://corpus.quran.com/download/ publie l'annotation morphologique sous GNU GPL avec attribution obligatoire et des conditions spécifiques (« verbatim copies », « changing it is not allowed »). La FAQ indique que les glosses anglaises de son interface proviennent d'une compilation originale Shehnaz Shaikh / Kausar Khatri ensuite retravaillée : https://corpus.quran.com/faq.jsp et https://corpus.quran.com/contact.jsp .

**Décision de sécurité** : la GPL annoncée sur le fichier d'annotation **ne suffit pas à prouver automatiquement** la licence de reproduction hors ligne de chaque glose anglaise affichée sur le site. Avant d'embarquer les glosses, contrôler le contenu effectif du téléchargement primaire (présence des glosses ou seulement balises morphologiques), la licence attachée à ce contenu, les conditions de redistribution d'une APK privée et l'attribution. La licence MIT d'un miroir qui recopie l'API Quran Foundation ne transfère pas les droits de la source.

**Gates inchangés** : alignement sur 77 432 clés QCF, cinq divergences de segmentation revues et aucune glose devinée ; loupe masquée/verrouillée tant que le paquet n'est pas approuvé. Les clés géométriques et tests peuvent rester publics.
