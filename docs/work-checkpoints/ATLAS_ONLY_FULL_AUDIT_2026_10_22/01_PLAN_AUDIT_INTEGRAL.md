# QURAN HAAFIDH — PLAN D'AUDIT CONTRADICTOIRE INTÉGRAL
Version : 22 octobre 2026 — **APK DEBUG ATLAS SEUL** (sans Correspondances)

## Mandat, principes et référence
Auditer le **dernier debug signé du 22/10**, et non un ancien APK ou la branche historique Correspondances. Examiner toutes les fonctions livrées, le code, les données et le rendu ; détecter activement les régressions, les corriger par commits isolés, puis relancer l'audit sur l'APK exactement produit.
Référence source applicative : `work/haafidh-1171-atlas-only-pre20-mask` @ `040af230d35f148ac323155a8ae6868e89094aef`. Voir `00_APK_DEBUG_BASELINE.md`.
**Correspondances est absente VOLONTAIREMENT** : en contrôler l'absence dans le binaire sans tenter de l'intégrer. Ne pas toucher aux sources privées ni aux acquis réels. Contrat Atlas : dernière version visuellement approuvée avant le 20 octobre et décisions éditoriales closes le 19 octobre (366 Unités + 1 053 Passages, 1 419 titres). Écraser les arbitrages approuvés est interdit.

## Méthode commune — couverture et preuve
Chaque scénario doit produire : ID unique, fonction et point d'entrée, référence du comportement validé, attendu, observé, données d'essai, environnement, capture/log/test avec SHA ou lien, verdict PASS/FAIL/NON TESTABLE/À ARBITRER, gravité P0/P1/P2/P3, correction et commit, retest, risque résiduel.
Un PASS de compilation ou de test source n'est jamais assimilé à un PASS d'interaction ou de rendu BOOX. Ne pas inventer de résultats ni déduire un état UI des noms de méthodes. Chaque écran/chemin actif doit figurer dans l'inventaire : la couverture est calculée avec le nombre de scénarios exécutés et les bloqueurs, jamais estimée.

### CP00 — Baseline et intégrité, AVANT toute modification
- Relever vrais HEAD, tags, diff et état de branche ; conserver lien/commit du binaire de référence.
- Récupérer **le véritable fichier .apk signé** ; vérifier SHA-256, taille, package, versionName/versionCode, certificat avec apksigner, mapping de classes DEX, assets et ressources ; rapprocher le SHA déclaré du manifeste (pas le supposer valide).
- Comparer au debug installé précédent sans l'écraser ; identifier le build CI de staging et distinguer ce staging incomplet de l'APK finalisé hors ligne.
- Garder le manifeste de provenance sans exposer clés, corpus privés ou autres secrets sur ce dépôt PUBLIC.
- Sortie : `00_APK_DEBUG_BASELINE.md` complété avec preuve indépendante, ou statut BINAIRE NON DISPONIBLE.

### CP01 — Inventaire exhaustif des fonctions et accès
- Énumérer chaque Activity/Fragment/View accessible, menus, raccourcis, paramètres, écrans hérités encore actifs, dépendances et sources de données.
- Tracer : écran origine → action → affichage/moteur → lecture/écriture → retour → rotation/recréation/veille.
- Croiser manifeste fusionné, code, navigation réelle et captures pour repérer tout écran omis ou inatteignable.
- Sortie : `01_INVENTAIRE_FONCTIONNALITES.csv` et `02_MATRICE_NON_REGRESSION.csv`.

### CP02 — Pureté de la variante Atlas seule
- Vérifier **absence volontaire** de Correspondances dans Accueil, Atlas, Lecture, Sabqi, Révision, menus et routes.
- Vérifier manifeste fusionné, classes DEX, ressources, catalogue, dépendances et code actif du **binaire réel**, pas seulement l'absence d'une icône.
- Exécuter `scripts/verify_atlas_only_purity.py`, inspection APK et contrôle négatif d'accès direct à toute activité supprimée. Ne pas réactiver cette fonctionnalité.
- Sortie : preuve source ET binaire ; toute classe/ressource inattendue = anomalie.

### CP03 — Titres Atlas, EXACTEMENT 1 419
- Réconcilier par ID, sourate, plage et titre approuvé **366 titres Unité + 1 053 titres Passage** ; rendre `EXPECTED/ACTUAL/DIFF` sur 1 419 lignes. Respecter toutes décisions utilisateur validées, dont les arbitrages du 19 octobre.
- Titre Unité gras et plus grand ; titre Ayah Theme plus petit et italique. **Aucun numéro de verset** ni « Extrait » dans les sous-titres ; pas de symboles parasites, `\\n`/`\\t` littéraux, encodage corrompu ni références techniques.
- Personnages : translittération arabe suivie de l'arabe entre parenthèses selon le contrat validé (ex. Mūsā (موسى), ʿĪsā (عيسى)) ; préserver diacritiques, parenthèses et sens RTL.
- Ne jamais modifier les limites canoniques ni « améliorer » les titres éditorialement sans arbitrage.
- Sortie : `03_ATLAS_1419_TITRES.csv` ; **zéro divergence injustifiée**.

### CP04 — Atlas interactif, deux frises, retour et Jalalayn
- Carte racine = **Unités uniquement** U1/U2/... avec curseur de sélection visible, rail coulissant et Unités accessibles.
- En Lecture : rail Unités plus grand, rail Passages plus petit ; contrôler qu'ils sont effectivement présents et utilisables, sur la BOOX, pas seulement compilés.
- Pour plusieurs sélections successives : rail Passages, sous-titre Ayah Themes et **surlignage géométrique réel** doivent basculer synchroniquement vers la plage exacte ; jamais le passage précédent ou toute une Unité par erreur.
- Test des frontières d'Unité, chevauchements de passages (dont 2:120–121), sourate mono-Unité, longues sourates, al-Baqarah U4/page 19, Fātiḥah, Qāf→adh-Dhāriyāt.
- Feuilletage toutes pages de l'Unité ; pagination QCF, double cercle page active, tailles et visibilité des lignes ; préservation des choix à l'aller-retour Atlas→Lecture→Atlas et après recréation.
- Panneau Jalalayn latéral **sans disparition du mushaf** ; scroll séparé, fermeture, retour au même verset. Supprimer surcharge de commandes et bandes inutiles selon contrat.
- Sortie : `04_ATLAS_FRISES_NAVIGATION_SURLIGNAGE.md` avec captures de trois sélections et leurs différences.

### CP05 — Lecture libre : gommage et amorces
- Le bouton bascule 100 % masque / texte normal **en lecture libre**, sans ouvrir de séance Hifz, sans crédit ou progression.
- Masque réel des mots/lignes QCF, avec uniquement les débuts d'amorces exactes et les six repères physiques autorisés (trois premiers et trois derniers mots), **pas des versets entiers**.
- Auditer `GeometryRepository` → vrais identifiants des 15 lignes → `MushafView` → JS/géométrie → cues/repères → `setMask(100)` : si lignes vides ou callback hors ordre, le bouton ne vaut pas PASS.
- Couvrir automatiquement les 604 pages pour géométrie/absence de blanc fantôme ; visuellement les pages de frontières, densités extrêmes et notamment captures page 14 **et 19 comme simples cas de régression, pas exceptions fonctionnelles**.
- Toggle rapide, swipe, veille, retour, changements de sourate ; sur géométrie invalide, texte lisible + diagnostic explicite, pas de faux masquage.
- Sortie : `05_GOMMAGE_604_PAGES.md` avec captures comparées AVANT/APRÈS.

### CP06 — Mushaf, lecture, tafsīr et fidélité QCF
- Contrôler **604/604** pages originales avec vrais tracés et géométries, ratio/proportions, noms de sourates, waqf, basmala, numéros, rosettes, parité, marqueurs Juz/Hizb et absence de clipping ou de flou.
- Swipe, focus et fade-away, sélection de plage, navigation page/verset/sourate, marque-pages/annotations si actifs ; absence de masque persistant après changement de mode.
- Ressources privées Atlas attendues (7), chargement hors ligne, ouverture/fermeture des commentaires Jalalayn, aucun retour forcé vers une ancienne interface.
- Sortie : `06_MUSHAF_QCF_LECTURE_TAFSIR.md`.

### CP07 — Moteurs Hifz et planning en vigueur
- Identifier la **dernière configuration réellement approuvée** avant de calculer les attentes (ne pas appliquer une ancienne cadence contradictoire).
- Sabqi / Apprentissage : cinq lignes si paramétrées, progression et bordures de sourate, masque, reprise.
- Itqān / Stabilisation : sessions par page, répétitions, intégrité des fragments, cas post-Nās, jonction Qāf→adh-Dhāriyāt.
- Consolidation, révisions actives/passives, Murājaʿah, libre et boule de neige : corpus actifs/passifs, dates, jours désactivés, six repères, promotions, séances reportées.
- Tester indépendamment chaque mode : départ, abandon, reprise, validation, deuxième validation, fin de plage, retour arrière, veille et fermeture forcée.
- Calendrier sept jours, matin/soir, exceptions dominicales si actives, fuseau/date et états de progression.
- Sortie : `07_MOTEURS_HIFZ_PAR_MODE.md` avec preuves des règles effectivement exécutées.

### CP08 — Acquis, données, migrations, stress
- Sur **copies de sauvegardes de test** : upgrade in-place, compatibilité package/certificat/versionCode, restauration, changement de date, reprise et crash aux transitions.
- Invariants P0 : **zéro perte d'acquis, double crédit, saut de ligne/page, progrès anticipé, validation réapparue ou incohérence ledger/UI**.
- Exécuter simulations 180/365/548 jours et les 80 scénarios historiques si accessibles ; comparer hash et états pré/post, tester réapparition à ~120 jours.
- Ne jamais désinstaller, réinitialiser ou modifier les acquisitions réelles pour tester.
- Sortie : `08_PERSISTANCE_MIGRATION_STRESS.md`.

### CP09 — Autres fonctions, sans oubli
- Accueil/programme, paramètres, mémorisation libre, progression/cartes, versets faibles, annotations, Quiz s'il existe, audio, affichage des aides/panneaux, réglages E-Ink et toute vue secondaire active.
- Chaque point d'entrée doit avoir cas nominal, cas d'erreur, retour, fermeture/redémarrage, persistance et comparaison au contrat.
- Sortie : `09_AUTRES_ECRANS_ET_INTERACTIONS.md`.

### CP10 — Icônes et ergonomie BOOX
- Vérifier les **23 icônes figées**, unicité, rôle/action/état, dessins et proportions, icône ancre aux emplacements contractuels ; ne pas réintroduire les huit icônes Correspondances ni une ancienne icône key-round si elle a été remplacée.
- Portrait plein écran, hiérarchie titres/contraste N&B, aucune barre rognant le QCF, tactile et focus, ghosting et clignotement, temps de chargement longues Unités, absence de scroll vertical parasite.
- Preuve obligatoire : captures sur émulateur ET observations BOOX physique lorsqu'elle est disponible ; ces deux environnements sont distingués.
- Sortie : `10_ICONES_BO0X_VISUEL.md`.

### CP11 — Sécurité, build reproductible et QA native
- Vérifier package, certificat `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`, signatures v2/v3 selon configuration, manifest fusionné, permissions, stockage, logs, WebView/JS bridge, chargement 100 % hors ligne et absence de secrets.
- Exécuter gardes Hifz/Atlas/icônes, tests unitaires et contrats source Gradle, puis tests instrumentés Android (navigation, capture, rotation/recréation, retour, sauvegarde) sur le **nouveau HEAD**.
- Tests négatifs : resource corrompue, géométrie absente, double clic, changement rapide de sélections, migration interrompue.
- Ne pas modifier arbitrairement snapshots/tests/gardes pour obtenir un PASS ; en cas de FAIL, conserver log/diff et cause.
- Sortie : `11_SECU_BUILD_QA_APK.md`, rapport signé/hashes des octets réels.

### CP12 — Contre-expertise et verdict de livraison
- Un auditeur indépendant examine les écarts entre contrat, résultats et captures, les cas limites et toutes les modifications depuis `040af230` ; cherche des contre-exemples, pas une confirmation de succès.
- Classer par P0 (acquis/intégrité/coran/sécurité), P1 (fonction majeure), P2 (performance/UX), P3 (cosmétique). Corriger P0/P1, puis P2; retester dépendances et **tout le système** après derniers changements.
- **GO seulement pour un CANDIDAT DEBUG privé** si absence Correspondances prouvée, 1 419 titres concordants, rails et surlignage fonctionnels, gommage réel, Hifz/migrations intacts, 604 QCF, 23 icônes, APK signé et tests pertinents PASS.
- Si test BOOX physique impossible : verdict **CANDIDAT À TESTER SUR BOOX**, pas « BOOX validé ». En cas de blocage, continuer toute correction/test réalisable ; interdiction de release publique et de release finale sans accord exprès.
- Livrables : `12_CONTRE_AUDIT_VERDICT_FINAL.md`, `13_RECETTE_PHYSIQUE_BOOX.md`, `14_APK_VERIFICATION.json`, matrice COMPLETE PASS/FAIL/NON TESTABLE/À ARBITRER, journal des commits et APK privé **hors dépôt public** avec SHA-256 réel.

## Conditions de clôture chiffrées
- Inventaire : 100 % des surfaces accessibles recensées et scénarios associés.
- Atlas éditorial : 1 419/1 419 contrôles et 0 écart non justifié.
- Mushaf : 604/604 actifs contrôlés, géométries vérifiées, échantillons visuels datés (ne pas prétendre que 604 pages ont été vues sur BOOX si seules vérifications statiques).
- Contrat : 23/23 icônes, 0 résidu Correspondances, aucune anomalie P0/P1 ouverte, chaque correction retestée.
- Intégrité : signature/migration in-place démontrées sur données de test, et aucune altération des acquis réels.
- Preuves : liens CI, captures, versions, SHA et résumé PASS/FAIL ; « non testé » explicite quand l'appareil physique est indisponible.


## Objectif de livraison définitif — RELEASE OFFICIELLE 1.17.2 (clarification utilisateur du 22 octobre)

**Le debug Atlas seul daté du 22 octobre 2026 est uniquement la BASE DE DÉPART de l'audit, et NON son livrable final.** La mission complète est : reprendre ce dernier debug, auditer exhaustivement toutes les fonctionnalités réellement intégrées, leurs comportements, l'intégralité des titres et icônes, corriger chaque anomalie identifiée, prouver la non-régression, puis préparer la **release officielle Quran Haafidh v1.17.2** pour validation explicite de l'utilisateur. **Ne pas publier automatiquement** cette release.

### Séquence obligatoire
1. Vérifier et figer la provenance du debug du 22/10 (SHA, certificat, variante Atlas seule, branche/commit, actifs) et l'utiliser comme baseline.
2. Exécuter les audits CP00 à CP12 sur toute l'application ; contrôler Atlas, Hifz, Lecture, réglages, autres écrans, 1 419 titres, 23 icônes, 604 folios QCF, navigation, données, sécurité et performance. Correspondances demeure volontairement absente : ne pas la réintégrer au périmètre v1.17.2.
3. Corriger par commits ciblés toutes les anomalies prouvées ; retester chaque correction, ses interactions et toute l'application après les dernières modifications. Aucun passage en release avec P0/P1 ouvert ou différence non justifiée avec le contrat.
4. Préparer un APK/AAB **candidat officiel v1.17.2** avec numéro de version cohérent, versionCode strictement supérieur à l'installation existante, même applicationId et certificat compatible, ressources privées autorisées hors dépôt public, build reproductible, manifeste et hashes. Inspecter **les octets du binaire final** et la conformité au HEAD final, pas seulement les sources.
5. Exécuter les essais de migration **sans désinstallation** sur données de test, tests Android/JVM/instrumentés, vérifications sécurité, restauration, longitudinal Hifz, lecture hors ligne et véritable recette sur appareil BOOX. Si les essais physiques ne peuvent être réalisés, le résultat reste candidat non validé ; ne pas déclarer la release prête.
6. Exiger une **contre-expertise indépendante finale** et remettre un dossier de décision GO/NO-GO par domaine, le journal des corrections et preuves, le candidat privé v1.17.2, et les réserves résiduelles. **GO technique ≠ autorisation de publication**.
7. **Publication officielle 1.17.2 seulement après accord explicite de l'utilisateur**, après réussite de tous les contrôles bloquants. Aucun déploiement public, release/tag de publication, ni substitution à la version installée sans cet accord.

### Livrables révisés
- Dossier d'audit exhaustif et anomalies corrigées, avec liens des commits, captures et résultats reproductibles.
- Rapport contradictoire final sur le **build officiel candidat v1.17.2** (et non sur un ancien debug).
- Manifeste du binaire candidat : applicationId, versionName=1.17.2, versionCode, empreinte APK, certificat, commit exact, inventaire d'actifs, PASS/FAIL/NON TESTABLE.
- Procédure de migration 1.17.1 → 1.17.2 et plan de retour arrière **sans perte d'acquis**.
- **Statut final attendu** : « CANDIDAT OFFICIEL 1.17.2 — PRÊT POUR AUTORISATION » si l'audit complet et les tests BOOX sont tous concluants ; sinon « NO-GO RELEASE », avec corrections poursuivies.

**À aucun moment le succès du debug seul ne clôture la mission.**


## Addendum bloquant — Lecture libre et reprise Work (22 octobre, après interruption)
Lire et **exécuter** \`02_PROMPT_REPRISE_WORK_HIGH_1172_APRES_INTERRUPTION.md\` dans ce même répertoire. Ce prompt consolide la reprise de la correction de schéma non encore retrouvée sur GitHub, l'exécution du test Android et de Gradle, et la contre-épreuve P1 prioritaire du masquage/ancres de Lecture libre.

Constats source à reproduire : \`MushafView.runWhenReady()\` n'a qu'un \`pending\` écrasable, les signaux JS \`pageShown\` et \`ready\` ne garantissent pas le séquencement de tous les réglages de masque, et \`StudyReaderActivity.applyFreeActiveMask()\` peut déclarer la page prête alors que la liste de lignes QCF est vide. Corriger l'ordre/génération des commandes et la validation des géométries ; ajouter tests instrumentés, page turnover, sélections rapides, pause/reprise, 604 pages QCF et non-régression Hifz. **La réussite des tests de présence de chaînes Java n'est pas une preuve de fonctionnement réel.**

Le pourcentage d'avancement Work doit venir d'une matrice dénombrée PASS/FAIL/NON TESTABLE et distinguer couverture, clôture validée et résultat de cinq tests ciblés. Aucun pourcentage global estimé sans dénominateur vérifiable. Le candidat 1.17.2 ne peut être déclaré prêt tant que ce contrôle P1 n'est pas clos.
