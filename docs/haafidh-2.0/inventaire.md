# Inventaire de qualification Quran Haafidh 2.0.0

Périmètre relevé dans le manifeste principal, les appels de navigation natifs, les stores et les ressources de l’arbre de développement. La colonne Preuve indique le périmètre observé. Chaque ligne reste ouverte pour les variantes et états indiqués. Le programme d’épreuve comprend FR, EN et WO, états normal, vide, chargement, erreur, indisponible et interrompu, selon les états qui existent effectivement sur chaque surface.

## Surfaces et accès

| ID | Accès et variantes | Attendu et continuité historique | Preuve et reste à couvrir |
|---|---|---|---|
| UI-01 | Accueil de `MainActivity`, reprise et choix de séance | Même entrée pour reprendre une séance, aucune progression au chargement, orientation portrait | Capture de l’assistant sur 320 dp. Toutes les tuiles, les titres dynamiques et les états d’erreur restent à qualifier |
| UI-02 | Assistant initial, offres de parcours, sélecteur, résumé, restauration initiale | Sauvegarde confirmée seulement, passage déclaratif séparé des crédits, retour possible à chaque étape | Parcours instrumenté et mélange des trois langues à poursuivre sur toutes les étapes |
| UI-03 | Parcours, calendrier, sélection et détails matin/soir, retards, bilan, finales et reprises cumulatives | Dates ISO et états indépendants des langues, ordre et créneaux historiques conservés | Campagne Android sur le calendrier et les 7 jours, détail des prévisions et toutes combinaisons à couvrir |
| UI-04 | Réglages, programme et corpus | Horaires conseillés, plages, volumes et régimes, historique séparé des réglages universels | Cas JVM de configuration et d’import. Restent formulaires, refus, horaires et annonces d’accessibilité |
| UI-05 | Réglages, Révisions, Affichage, audio, À propos, Diagnostic et réinitialisation | Chronos et vitesse cohérents avec les brouillons, choix explicite avant suppression | États pack absent/invalide, diagnostic, confirmations, libellés et fichiers sélectionnés à exercer |
| UI-06 | Réglages, Sauvegarde et restauration initiale | Documents Android OPEN/CREATE, restauration V3 transactionnelle, cinq ensembles | Mise à jour Android synthétique et interruption du journal, manque de stockage et restauration privée ouverts |
| UI-07 | Cap Hifdh et carte de progression `ProgressMapActivity` | Distinction déclaré, travaillé et acquis, folios et motifs sans dépendance à la couleur | Contrats de source et tests JVM. Dates, graphiques, contrastes et agrandissement à qualifier en trois langues |
| UI-08 | Lecture `StudyReaderActivity`, sélecteurs, annotations, agrandissement et réglages lettre/page | Folio fixe, hors parcours, aucune validation. Navigation Mushaf, hizb et repères | Géométrie et tests historiques. Lecteur libre trilingue et accès conditionnels à vérifier |
| UI-09 | Tafsir en lecture, panneaux de l’Atlas, popup des amorces et sources | Corpus et langue d’origine préservés, coordonnées indépendantes, attribution et provenance | Droits et états vide/erreur/chargement, libellés HTML/JS et corpus alternatifs à vérifier |
| UI-10 | `AtlasMapActivity`, cartes sémantiques et repères, variantes selon les actifs | Lecture seule, ordre canonique, gestion des sources et miniatures | Feuille de qualification des actifs. Couverture éditoriale, visuelle et hors ligne à compléter |
| UI-11 | `IbnKathirMapActivity` historique, pilote Baqara et `AtlasSourcePdfActivity` | Activités internes déclarées. L’entrée publique ouvre `AtlasMapActivity`. La référence PDF mérite un relevé d’accès par variante | Le pilote comporte un appel historique à `IbnKathirMapActivity`. Couvrir l’absence des actifs privés et les intents conditionnels |
| UI-12 | Similitudes `AtlasDiscriminantActivity` et Correspondances `CorrespondenceComparisonActivity`, accessibles depuis Lecture/séances | Comparer et naviguer sans crédit, retour à la page, préservation des correspondances et aides | Captures et tests état/orientation à compléter, y compris occurrence absente et sélection précise |
| UI-13 | `HifzSessionActivity`, Sabqi, Itqān, finales, reprises et cumul | Masque, page, date et protocole d’une séance ouverte. Aucun double crédit. Règles de reprise historiques | JVM et calendrier Android partiels. Répétitions, audio, résilience, changement de langue et interactions des deux régimes |
| UI-14 | `HifzSessionActivity`, révision active et Fragilités autonomes ou programmées | Liste de pages, masquage, dévoilement ciblé, report, chrono, aucune validation artificielle | Campagnes Android, quatre formats et six transitions. Non-régression complète, BOOX physique et interruptions restent ouvertes |
| UI-15 | `FreeMemActivity` depuis Outils avancés | Compteur, page et paramètres locaux, aucun crédit sur le parcours | Stock vérifié dans la migration synthétique. Présentation, audio et transitions FR/EN/WO à compléter |
| UI-16 | `WeakVersesActivity` présente dans les sources | Pas d’activité déclarée sous ce nom dans le manifeste principal. L’entrée Fragilités active utilise le lecteur de séance | Vérifier le code résiduel et les variantes avant tout retrait |

## États, stores et tâches

| ID | Surface | Relation à la sauvegarde et cas critiques |
|---|---|---|
| DATA-01 | `quran_hifz_preview_v1` | V3 MAIN. Schéma 6, réglages, curseurs, positions, preuves physiques, annotations, rotations, brouillons et résultats actifs. Contrôler chaque famille de clés et les migrations |
| DATA-02 | `quran_hifz_dashboard_ledger_v1` | V3 LEDGER. Présence attestée, dates, libellés historiques et bornes de couverture |
| DATA-03 | `quran_hifz_j10_v1` | V3 J10. Historique de révision antérieur conservé, cohabitation avec la représentation en schéma 6 |
| DATA-04 | `hifz_preview_session_gates` | V3 GATES. Compatibilité des états de contrôle historiques |
| DATA-05 | `free_mem_preview` | V3 FREE_MEM. Mémorisation ponctuelle et réglages indépendants |
| DATA-06 | `quran_hifz_restore_journal_v1` | Journal local de restauration et récupération dans `HifzRecoveryApplication`, hors payload V3. Examiner les interruptions à chaque écriture et au redémarrage |
| DATA-07 | `haafidh_display`, `hifz_study`, `hifz_tafsir_reading`, `haafidh_semantic_map` | Langue locale, position de Lecture, réglage de commentaire et repères d’Atlas hors des cinq ensembles. Tester maintien lors d’une mise à jour et non-écrasement lors d’un import |
| SYS-01 | `MushafView`, `hifzreader/reader.js`, police et dessin, préparation des cartes, PDF et miniatures | Rendu local et tâches asynchrones liées aux activités, cycle chargement/prêt/échec/annulation. Identifier les contenus accessibles via enveloppes JS avant validation |
| SYS-02 | Audio et Documents Android | Import ZIP local et lecture intégrée dans les séances concernées. Contrôler refus, annulation, espace, arrêt brutal et droits, sans audio en Lecture libre |
| SYS-03 | Manifeste et synchronisation | Le manifeste principal observé déclare des activités et l’application de récupération. Aucun service, récepteur de widget ou worker Drive autonome n’a été identifié dans ce périmètre. L’import/export emprunte le sélecteur système. La synchronisation et ses collisions restent à qualifier séparément |
| SYS-04 | Actifs Gradle, géométrie QCF, Tafsirs, cartes et dictionnaire | Résultats d’assemblage dans la matrice. Vérifier paquet final, variantes, origine, droit, attribution et absence d’actifs ou de données privés en CI |

## Interactions à vérifier à chaque candidat

| Cas | Invariant décisif |
|---|---|
| INT-01 · Langue × écran × état | Les actions, les plafonds, les reçus et les dates ISO restent identiques lors des six transitions |
| INT-02 · Changement de semaine, mois et année, jour bissextile, fuseau ou heure d’été | Un changement d’affichage ne reprogramme aucun créneau. Une reprise utilise les preuves et dates de la séance selon son protocole |
| INT-03 · Déclaration/route × brouillon × régime Itqān | Changer le programme conserve le travail en cours, ne fabrique aucun crédit et n’active aucun enchaînement inapplicable |
| INT-04 · Sauvegarde × interruption × historique | Les cinq ensembles restent cohérents et les états historiques gardent leur sens après double import et mise à jour |
| INT-05 · Portrait/paysage × largeur × texte à 100/150 % × langue | Folio QCF fixe, zones essentielles accessibles, aucune superposition, annonces compréhensibles |
