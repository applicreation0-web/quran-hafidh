# WORK HIGH — QURAN HAAFIDH 1.17.2
## REPRISE STRICTE APRÈS INTERRUPTION WORK — AUDIT, CORRECTIONS ET CANDIDAT DE RELEASE

Date : 22 octobre 2026. Ne pas repartir de zéro, ne pas remplacer la baseline, ne pas publier.

### 0 — BASELINE INCHANGEABLE ET FICHIERS À LIRE
- Dépôt : applicreation0-web/quran-hafidh.
- Branche : work/haafidh-1171-atlas-only-pre20-mask.
- Dernier commit applicatif identifié : 040af230d35f148ac323155a8ae6868e89094aef.
- Dernier commit documentaire connu **avant ce prompt** : f6ba42f3e64d2bfc87f7f9e12db50080b5ed3e21. **Revérifier les HEAD actuels**.
- APK de départ annoncé : QURAN_HAAFIDH_BOOX_ATLAS_FRISE_PASSAGES_GOMMAGE_CORRIGE_DEBUG_20261022.apk ; SHA-256 déclaré a14e9892afa36b8a996246024332562cb903e5af634f65e2c03d933edc1e01cd. Recalculer à partir des octets si disponibles ; ne pas confondre avec le staging public incomplet.
- Lire intégralement les checkpoints GitHub : 00_APK_DEBUG_BASELINE.md et 01_PLAN_AUDIT_INTEGRAL.md dans docs/work-checkpoints/ATLAS_ONLY_FULL_AUDIT_2026_10_22/.
- VERSION CIBLE = release candidate officielle **1.17.2** (et non simple debug). Publication uniquement sur accord explicite de l'utilisateur.
- **Correspondances volontairement absentes** de cette variante. Ne pas réintégrer.

### 1 — RÉCUPÉRER LES CORRECTIONS DE LA SESSION WORK INTERROMPUE (P0 REPRISE)
Le dernier écran Work a indiqué : défaut de schéma de récupération reproduit ; 4 tests échouaient AVANT correction ; 5 tests ciblés passent APRÈS patch ; un test Android de préservation des magasins/stores et du journal a été créé mais NON exécuté ; suite Gradle complète en cours ; aucun appareil ADB connecté. C'est un compte rendu de Work, pas encore une preuve git/CI indépendante.
1. Inspecter le workspace local de l'ancienne exécution (status, diff, commits, fichiers non suivis, tests, logs), sans reset, clean, checkout destructif ni écrasement du travail.
2. Préserver/committer les correctifs cohérents du schéma et les tests après validation, sur la bonne branche. Rattacher logs et preuve avant/après à leur commit.
3. **Exécuter le test Android nouvellement ajouté**, vérifier intégrité des stores et journal, puis relancer toute la suite Gradle ; consigner chaque verdict.
4. Si l'ancien workspace n'est plus accessible, déclarer explicitement NON RÉCUPÉRÉ. Repartir uniquement des changements retrouvés dans les commits/fichiers/checkpoints ; ne jamais dire que le patch non sauvegardé existe sur GitHub.
5. Première réponse courte : HEAD réel, fichier(s) modifiés récupérés ou manquants, tests réellement exécutés, anomalies P0/P1, prochaines actions.

### 2 — NOUVEAU BLOCAGE P1 PRIORITAIRE : LECTURE LIBRE, MASQUAGE ET ANCRES
L'inspection en lecture seule du code de la branche a repéré des risques **à reproduire** :
- MushafView.java, méthode runWhenReady (vers ligne 391) : une seule variable pending Runnable ; toute nouvelle commande pendant ready=false écrase la précédente.
- reader.js : prepare() transmet pageShown avant ready ; StudyReaderActivity.onPageShown déclenche applySemanticCues/applyFreeActiveMask. En préchargement, plusieurs appels distincts setMaskFollowsSelection / setLandmarkLines / setPageLandmarkBoxes / setSemanticCues / setMask(100) peuvent être perdus, mal ordonnés ou viser une page obsolète.
- StudyReaderActivity.showFreeActiveMaskPage() peut appeler mushaf.show(page,...,pageLines=[],0) si géométrie de lignes indisponible ; applyFreeActiveMask() qualifie freeActiveMaskPageReady uniquement selon six repères + ancres, sans vérifier directement que la géométrie des lignes du folio est exploitable. reader.js n'applique le gommage que si lineIds et lines non vides. Risque : bouton affiché « activé » mais texte non gommé.
- Risques associés : rechargements successifs, watchdog retry, changement rapide page, ON/OFF, retour Atlas, pause/reprise, vieille géométrie et commandes JS asynchrones.
Ces points sont des **défauts/risques de code à tester et corriger**, pas une recette BOOX déjà effectuée.

#### Correction fonctionnelle attendue
A. Mettre en place une séquence de commandes sûre (file ordonnée, transaction unique sur WebView ou bootstrap atomique) ; **ne jamais écraser silencieusement les commandes requises**.
B. Associer les opérations et rappels à un **identifiant de génération / numéro de page**. Rejeter les retours JS d'une page antérieure ; invalider les commandes périmées après nouvelle navigation ou retry ; préserver la sûreté des autres modes MushafView.
C. Installer les véritables lignes géométriques, six repères et premiers mots d'amorces **avant** setMask(100) ; conserver exactement les trous de mots, pas des versets entiers. Ne pas modifier les tracés QCF originaux.
D. Exiger une disponibilité cohérente du folio + lineIds + géométrie + repères + ancres ; si invalide, **fail-open lisible**, masque désactivé/état du bouton exact, diagnostic explicite et aucune donnée Hifz écrite.
E. Un seul appui = ON ; second appui = OFF ; restaurer le texte et les états du lecteur. Les ancres, options d'affichage, focus Atlas et masquage n'interfèrent pas.
F. Limiter le patch au périmètre nécessaire ; si MushafView est partagé par Sabqi/Itqān/Murājaʿah, exiger non-régression de ces modes.

#### Tests incontournables
- Reproduction pré-patch et preuve post-patch du défaut de commandes écrasées : ready=false, **plusieurs** runWhenReady, pageShown avant ready ; assertions sur ordre et effet final réel JS.
- Test Android instrumenté sur une WebView/Activity réelle, non simple contains() de chaînes Java/JS ; capturer l'état masque, les repères, le folio, le résultat visuel.
- ON/OFF avant chargement, ON/OFF rapide, 10 changements de pages, swipe pendant init, chargements lents, navigation en arrière, changement sourate, retour Atlas→Lecture, ouverture/fermeture tafsīr, veille/reprise, Activity recréée, watchdog/retry, arrêt forcé.
- Page sans ancre, page à multiples ancres, géométrie absente/corrompue, six repères manquants ; échec lisible sans faux succès ; pages 14 et 19 comme exemples historiques, **pas des exceptions**.
- Balayage programmatique de **604/604** pages QCF, repères/boîtes, et échantillonnage visuel des frontières ; distinguer validation statique et validation BOOX physique.
- Test isolation Hifz : aucune mutation des acquis, crédits, calendrier, progression, journal ; tests de masque partagé Sabqi/Itqān/Murājaʿah, surlignage et géométrie.
- Ne pas « corriger » le test pour masquer le défaut ni seulement augmenter un délai.

### 3 — POURSUITE DE L'AUDIT CP00 À CP12 (NE RIEN OMETTRE)
Toutes les fonctions et tous leurs parcours : Atlas 366 Unités + 1 053 Passages = 1 419 titres attendus/affichés, frontières, translittération arabe + arabe entre parenthèses, pas de préfixes Extrait ni numéros de versets dans les sous-titres ; deux frises, sélection Ayah Themes et surlignage synchronisés, Jalalayn latéral ; 23 icônes contractuelles ; 604 pages QCF ; Lecture libre ; Hifz Sabqi/Itqān/consolidation/Murājaʿah, calendrier et validations ; mémorisation libre, faibles, annotations, audio, paramètres, Quiz si actif, restauration, sécurité, hors-ligne, migration 1.17.1→1.17.2, stress 180/365/548 jours et 80 scénarios historiques si accessibles.
- Chaque surface : attendu/approuvé, constat réel, preuve, PASS/FAIL/NON TESTABLE/À ARBITRER, P0/P1/P2/P3, correction commit, retest, risque.
- Les **cinq tests ciblés réussis n'impliquent pas 100 % du projet** : reprendre les résultats réellement exécutés, refaire les tests impactés, pas les audits historiques déjà arbitrés.
- Contre-expertise indépendante et audit global sur le binaire candidat FINAL.

### 4 — POURCENTAGE OBJECTIF ET REPORTING
Ne jamais inventer d'avancement global. Créer une MATRICE de critères de validation pré-définis (IDs uniques, dénominateurs figés par domaine).
Pour chaque domaine, afficher : critères prévus / exécutés / PASS / FAIL / NON TESTABLE / en attente + liens des preuves.
- « Avancement de vérification » = (nombre de critères exécutés avec résultat documenté / nombre de critères applicables planifiés) × 100.
- « Clôture validée » = (nombre de critères PASS, preuves vérifiables et éventuelles corrections retestées / nombre de critères applicables planifiés) × 100.
- Ne pas confondre 5/5 tests ciblés avec une couverture générale, ni 0/13 checkpoints formellement clos avec zéro travail réalisé.
- Pour les scénarios bloqués par absence BOOX/ADB, les déclarer NON TESTABLE avec responsable/action, sans les créditer comme PASS.
- Mise à jour de la frise des jalons CP00–CP12 à chaque checkpoint achevé.

### 5 — RELEASE OFFICIELLE 1.17.2, SANS PUBLICATION AUTOMATIQUE
Corriger tous P0/P1, traiter P2 nécessaires ; exécuter suite JVM/Gradle + instrumentés Android + tests visuels + migrations et contre-audit. Produire un candidat privé 1.17.2 avec versionCode supérieur, même appId/certificat, actifs privés hors dépôt public, SHA-256 du binaire réellement construit et provenance Git attestée.
Recette sur BOOX physique requise pour prétendre au GO final ; aucun appareil ADB connecté dans la capture de reprise. Tant que non testée : « candidat non validé BOOX — NO-GO release ».
Ne jamais publier ni taguer une release officielle sans l'accord explicite de l'utilisateur ; ne pas désinstaller, réinitialiser les acquis, exposer clé/secret/corpus sur GitHub public. Si un blocage persiste, poursuivre les corrections/test réalisables et présenter les blocages restants, pas un faux PASS.

**ACTION IMMÉDIATE : récupérer et sauver le patch du schéma interrompu ; exécuter le test Android non lancé et toute la suite Gradle ; ensuite reproduire et corriger la séquence Lecture libre/MushafView, puis reprendre CP00–CP12 sur la 1.17.2 complète.**
