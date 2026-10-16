# Quran Haafidh — plan de chantier post-audit Atlas (16 octobre 2026)

**Portée :** une seule application privée, une seule branche `work/haafidh-1171-ibn-kathir-integration-independent`. Ce document est un **plan**, pas une validation ou un patch de production.

## État de référence figé

- APK privé signé contrôlé par Work : `Quran-Haafidh-ATLAS-SIMPLIFIE-60005894-PRIVATE-DEBUG.apk`.
- SHA-256 binaire : `8132e5fceccf06ad1b5588f8dee2d1558993a060f5394c2c286cb07d6e0c48c1`.
- Code exact compilé : `600058942b51a5775a5dec774d321ef0a39a236d`.
- HEAD documentaire avant ce plan : `250f4c7f873b926e37a1e9b282345d86c099700a`.
- Baseline officielle 1.17.1 protégée : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Contrôle contradictoire reçu le 16/10 : **pas de nouvelle régression moteur démontrée**, A-01/P2 sur titre 27:67–82, limitations de runtime privé/BOOX/restauration ; release bloquée. Le rapport est une pièce externe et non une nouvelle CI.
- Sources déjà validées numériquement : 114 sourates, 173 macro-unités (2–9 subdivisées ; 106 autres à une unité), 1 053 passages Ayah Themes inchangés, 6 236 versets, 604 pages SVG QCF. `macros_114.json` privé contient actuellement des **labels de remplacement**, pas des titres Maududi authentifiés.

## Décision utilisateur : calendrier du dimanche — NE PAS « CORRIGER »

- **Dimanche matin :** Stabilisation/Itqān prévue + **Renforcement final Sabqi**.
- **Dimanche soir :** **Consolidation finale Itqān**.
- Pas de séance distincte de révision active/passive le dimanche.
- Les finales validées alimentent les acquis/révisions selon les règles existantes ; conserver la logique et la persistance actuelles. Ceci corrige un précédent diagnostic éditorial erroné ; **aucune anomalie de calendrier n'est retenue**.

## CP-00 — Reprise et verrouillage documentaire

Lire l'audit Work associé au binaire `60005894` et les deux STATUS/CP historiques ; vérifier HEAD réel, branche, statut Git et CI. Inventorier les modifications ultérieures sans écraser les travaux. N'utiliser ni APK précédent ni succès CI historiques comme preuve du futur APK.
**Sortie :** matrice de traçabilité exigences → fichiers → preuves, état P0/P1/P2.

## CP-01 — Correctifs d'interface minimaux, sans moteurs Hifz

1. Accueil `MainActivity` : **Apprentissage — 1 page/semaine** ; **Stabilisation — 2 pages/semaine**. Aucun détail « 5 lignes », « 4 séances », « 1/3 » ou « 1/4 » sur cette ligne d'accueil. Les règles sous-jacentes (3 × 5 lignes ; 4 séances/2 pages) sont conservées. Ne pas masquer le mode spécifique d'entretien après An-Nās, dont l'objectif peut différer.
2. Pages Atlas : **parité** = remplissage constant d'une page à l'autre ; **sélection** = anneau/double contour (et léger accroissement de taille si nécessaire), sans inversion de la parité. **Vérifier d'abord dans la vraie vue Lecture quelle classe pair/impair est noire/blanche** avant de figer la table de styles. Garder des zones tactiles d'au moins 48 dp, contrastées sur BOOX.
3. Barre d'actions Atlas : **Tafsir et Lecture centrés, symétriques**, pictogrammes contractuels, sans libellés ; fonction précise et ordre de lecture cohérents ; retour toujours évident ; zéro bouton supplémentaire.
4. **A-01 P2 An-Naml 27:67–82** : pour la sélection canonique entière, afficher le **titre du parent canonique** dans le bandeau condensé comme développé ; pour les trois sous-repères exacts, afficher leur propre titre. Ne pas diviser/dupliquer le passage canonique ; assurer sélection et retour non régressifs.
5. Aucune réécriture de `hifz-core`, moteurs Sabqi, Itqān, consolidation, renforcement, révisions, crédits, masques, sauvegardes et Jalalayn.

**Sortie :** diff d'interface minimal, tests ciblés A-01 / page paire-impaire / sélection / boutons, contrôle du dimanche sans changement ; commit + preuves.

## CP-02 — Inventaire éditorial exhaustif et provenance des titres

**Deux objectifs à distinguer :** (A) attribuer un intitulé à chacune des 173 unités **existantes** ; (B) donner aux 114 sourates une véritable architecture thématique **uniquement lorsque des divisions textuellement sourcées existent**. Ne pas transformer une sourate courte en sous-unités artificielles.

Sources privées : EPUB anglais `Towards Understanding Quran (Maududi)` déjà fourni ; introductions, sous-sections et plages vérifiées ; recoupement possible par éditions Tafhim/Tafheem et corpus Ayah Themes (titres de passages indépendants). Ne pas transférer textes/clefs privés vers GitHub ou CI publiques.

Pour chaque unité candidate, consigner : sourate, ayat début-fin, titre EN, statut `literal Maududi` / `editorial summary sourced to Maududi` / `no reliable title`, ancre bibliographique exacte (section et référence), confiance, décision et contrôle des chevauchements. Ne pas faire passer une synthèse pour une citation. Contrôler spécialement le premier bloc de chaque sourate, frontières et sens de « Israelites / Children of Israel » sans falsifier le texte d'une citation.

**Sortie A :** 173/173 lignes auditées : source du titre ou repli neutre explicitement marqué, sans libellé inventé. **Sortie B :** table des divisions complémentaires sourcées pour 106 sourates jusque-là monobloc ; documenter clairement celles où aucune subdivision n'est justifiée. Respecter intégrité des 1 053 passages canoniques, y compris chevauchements.

## CP-03 — Validation éditoriale contradictoire avant intégration

Contrôler ligne par ligne les titres, la concordance des versets, les recouvrements, le sens des mots et la cohérence de l'anglais. Les titres littéraux doivent être fidèles ; ceux reformulés doivent être identifiés comme rédactionnels. Signalement et arbitrage des litiges ; **aucune suppression silencieuse des titres de passages Ayah Themes**.
**Sortie :** catalogue privé source-safe gelé, rapport des points en suspens et vérification exhaustive 114 sourates.

## CP-04 — Intégration des titres et ergonomie Atlas

Déployer les titres d'unités approuvés dans le paquet d'affichage privé vérifié par intégrité, et seulement les éventuelles nouvelles limites **approuvées**. Dans l'Atlas, conserver U1/U2… et leurs positions fixes ; nom de l'unité sélectionnée lisible **en texte simple**, jamais sous forme de pavé/carte ; ne pas noyer la page QCF ou pousser hors écran des versets. Si titre long, disposition bornée et action d'expansion dédiée sans recouvrir le Mushaf. Passages Ayah Themes toujours en points liés, ordre canonique, titre exact, pages QCF et polygones exacts. Une vue = un écran BOOX sans scrolling vertical ; lecteur QCF séparé en plein écran avec ses 15 lignes, retour stable, Jalalayn intact.
**Sortie :** un commit d'intégration, miniatures et captures de cas représentatifs (Al-Baqarah, An-Nisā’, Al-Mā’idah, An-Naml, sourate à une unité, titre long, jointure de pages).

## CP-05 — Tests et génération d'un seul candidat privé

Sur SHA exact : gardes gel des moteurs + suites JVM/source + 101 Android + 2 persistance/force-stop + simulations Hifz 180/365/548 j selon scénarios réellement exécutés. Tester également bornes/titres 114 sourates, 1 053 passages, surlignage 604 QCF, navigation parités et sous-repères An-Naml, reprise de position, orientations/densités effectivement disponibles. Distinguer PASS/FAIL/NON EXÉCUTÉ et preuves historiques. Assemblage local avec assets privés **hors CI**, signature par la clé PKCS#12 historique compatible et vérification certificat, package, versionCode, zipalign, apksigner, SHA-256. Ne publier aucune clé ni contenu privé.
**Sortie :** un APK candidat signé, SHA-256, journaux, captures et rapport de provenance ; pas de release.

## CP-06 — Audit Work High indépendant (READ-ONLY)

Audit du **nouveau SHA candidat signé** par une session distincte, sans changer son code ni les données Hifz. Vérifier l'élimination de A-01, l'origine des titres Maududi et leur couverture, la cohérence des pages, le dimanche, l'UI BOOX, les tests moteurs et persistance. Ne pas confondre un test code-only avec une exécution du binaire privé. Relever toutes les limites et défauts, n'accepter aucune auto-certification du producteur.
**Sortie :** verdict documenté PASS ou blocage précis.

## CP-07 — Recette BOOX physique et release privée sous conditions

Sur la BOOX réelle, sauvegarder les acquis **avant** installation ; test de mise à jour par-dessus l'APK existant **sans désinstallation** ; après mise à jour valider sauvegarde/restauration sur copie isolée, crédits et séances, arrêt forcé/reprise, page entière 15 lignes, parité et sélection, 114 sourates/passage réel, retours et Tafsir ; aucune perte d'acquis. Contrôle visuel réel « aucun défilement vertical » et cibles tactiles, indépendamment des seules captures émulateur.
Release privée **uniquement** si CP-06 favorable, CP-07 documenté, signatures identiques, aucune P0/P1 et défaut P2 A-01 résolu. Pas de release publique, de nouveau dépôt/branche/PR ; ne jamais remplacer l'officielle 1.17.1.

## Règles d'efficacité Work et sauvegardes

- Toujours réutiliser les checkpoints, EPUB et tests existants ; une seule branche ; sauvegarde par checkpoint avec commit et preuves.
- Réaliser l'extraction éditoriale et les contrôles de titres **avant** de multiplier les builds/APK. La correction graphique peut être préparée en parallèle sans mutation Hifz.
- Un seul APK candidat **après** l'intégration des travaux approuvés ; un audit final indépendant ; puis recette physique.
- Tout écart de périmètre ou tout titre non sourcé est signalé, non « résolu » par invention.
