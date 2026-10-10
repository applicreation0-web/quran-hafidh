# QURAN HAAFIDH 1.17.1 — J5–J7, audit contradictoire

**Référence officielle** `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21` · **Branche unique** `work/haafidh-1171-ibn-kathir-integration-independent`.

**Statut de ce document :** audit du code, des contrats et de la couverture des tests. Le résultat de chaque CI doit être lu sur le HEAD correspondant. Un CI vert ne constitue PAS une preuve d'installation sur BOOX ni une validation tactile/E-Ink. **Aucun APK produit par cette campagne.** Pas de release sans approbation.

## Méthode contradictoire

Pour chaque famille, chercher la condition qui **invaliderait** le comportement attendu, puis vérifier le code effectif (pas les commentaires seuls), exécuter un test comportemental lorsque disponible, et conserver le gel strict des moteurs non modifiés.

Sources de preuve :

- Tests Android/JVM de `hifz-app` et `hifz-core` via `ibn-kathir-engine-frozen.yml`.
- Tests négatifs sur le corpus Hafs/QCF et la Carte Ibn Kathīr (groupe exact, fail-open).
- Comparaison **octet pour octet** du code protégé contre 1.17.1, exceptions J1–J6 explicitement construites : `verify_authorized_hifz_session_delta.py`, `verify_authorized_program_preferences_delta.py`, `verify_authorized_consolidation_delta.py`.
- `audit_j5_j7_contradictory.py` (20 vérifications indépendantes de couverture statique).
- `SnowballValidationIdempotenceTest`, `PassiveApprisAndSundayContractTest`, `ConsolidationSevenAndSundayFinalTest`, `ItqanCompletionBoundaryPolicyTest`, `IbnKathirFirstWordsCorpusTest`, `WeeklyProgramPolicyTest`.
- `verify_contractual_ui_icons.py`, `verify_j6_icon_roles.py`, `audit_all_screen_icons.py`.

## J5 — anomalies des validations

| Contre-exemple recherché | Mesure de prévention / test |
|---|---|
| Une validation du lundi est reproposée mardi sans aucune nouvelle ligne | Comparaison de l'empreinte **semaine + ensemble ordonné d'unités** ; le lendemain, le même ensemble n'est pas dû |
| Une nouvelle unité est ajoutée mercredi après la séance lundi | Empreinte différente : un nouvel ensemble cumulatif devient dû |
| Validation deux fois du même ensemble | Un seul commit atomique date + empreinte + effacement du snapshot OPEN ; second appel refusé |
| Interruption puis redémarrage BOOX | La session gelée est restaurée, sans remonter un compteur à zéro |
| Interruption mardi, reprise mercredi, nouveau bloc ajouté entretemps | Snapshot des unités figé à l'ouverture, date de séance restaurée, nouvelles unités restent dues après fermeture |
| Mauvais état temporel LEARNING ↔ STABILIZATION | Clés indépendantes par famille, identifiants de cycle datés et vérifiés |
| Finale dimanche rejouée et double promotion vers Acquis | Garde de date sous verrou transactionnel; les semaines entièrement acquises sont exclues des prochaines finales, même dans la fenêtre 8 semaines |
| Passage à minuit / prochaine semaine | L'empreinte porte le lundi d'ancrage; l'historique des 8 semaines ne reçoit aucune remise à zéro |
| Changement de programme Classique/Recommandé | Choix daté indépendant des curseurs/états/historiques; tests de conservation |
| Interruption d'une finale et réouverture avec un autre cycle | Contrôle du `cycleId` et du suffixe de date pour ne pas imputer des répétitions à un autre dossier |

**Différence essentielle :** nouveau **cumul** autorisé, **même cumul** non crédité à nouveau. Les moteurs de promotion restent responsables des passages vers Acquis; une révision de semaine n'y touche pas.

**Risque à contrôler en réel :** scénario de migration d'une ancienne session ouverte qui ne possède pas le champ optionnel `sourceSnapshot` (ancienne version). Pour ce cas, l'application doit rester conservative plutôt qu'attribuer à tort de nouvelles lignes. Contrôler avant installation le scénario d'upgrade avec une sauvegarde anonymisée représentative. Les tests JVM simulent le disque et le redémarrage, pas une extinction BOOX physique.

## J6 — corpus, planning, horaires

| Contrôle | Règle cible |
|---|---|
| Révision passive | Inclure des versets entièrement couverts par des lignes **Appris ou Acquis**, dès la validation Sabqi; ne pas inclure de verset partiellement appris |
| Révision active | Corpus séparé, inchangé, réservé au matériel stabilisé/éligible antérieur |
| Quiz | Aucun changement au moteur officiel 1.17.1; éligibilité recalculée depuis `learned ∪ stabilized ∪ acquired` |
| Curseurs | Passive et active restent indépendants; calcul du corpus en lecture seule |
| Durée passive | Base 30 minutes et augmentation existante par `MaintenanceCoveragePolicy` conservées |
| Recommandé lundi–samedi | Active 15 min proposée le matin avec Sabqi/Itqān; Renforcement/Consolidation + passive 30 min le soir |
| Dimanche Classique | Uniquement les deux finales; **aucune Révision active ni passive planifiée** |
| Dimanche Recommandé | Itqān ~8 lignes + finale Sabqi le matin; finale Consolidation Itqān le soir; aucune Révision active/passive planifiée |
| Horaires | Matin 05:30–06:30; soir 20:00–21:00 initialement, modifiables via sélecteurs système; aucun verrou |
| Dimanche matin | Durée extensible, sans fin de séance imposée par l'horaire indicatif |
| Programme | Choix daté; maintien du format Classique hors adaptations expressément demandées |

**Précaution sur les lignes Appris :** une **unité physique** n'est pas automatiquement un verset complet : un verset enjambant des lignes non apprises est exclu jusqu'à couverture totale. La Révision passive n'écrit jamais d'Acquis.

## J7 — contre-audit fonctionnalité par fonctionnalité

| Fonctionnalité / surface | Ce qui est vérifié | Ce qu'il reste à voir sur BOOX |
|---|---|---|
| Mushaf Madinah Hafs | 604 pages, 77 432 mots QCF, géométrie inchangée | Rendu des glyphes et ajustement plein écran |
| Lecture ordinaire | Origine Ibn Kathīr unifiée; navigation pages/sourates; annotations hors protocole | Lisibilité des amorces, vitesse d'actualisation |
| Carte sémantique | 1 903 groupes / 114 sourates / 6 236 versets, bornes source | Appui bloc et retour, présentation en portrait |
| Amorces | Deux premiers mots exacts, pas de titre inventé; groupe d'un seul mot possible | Début/fin de page, notamment Qāf |
| Révision active | 15 minutes, source QCF, masques fail-open, curseur propre | Voile/focus, révélation et marquage faible |
| Itqān normal | Nouvelles séances ×40 (20/5/5/5/5), ancien plan persisté | Succession des pourcentages, contraste E-Ink |
| Itqān après An-Nās | Régime spécial conservé, amorces exactes | Aucune zone entièrement masquée sans repère |
| +1 / +2 Itqān | Fin réelle de page/sourate, continuité, sans fusion de sourates | Accepter/refuser, pages 520–521 et bloc suivant |
| Sabqi | Moteur répétitions inchangé | 5 lignes, focus visuel, aucune action superflue |
| Renforcement du soir | Quotas existants inchangés, empreinte anti-rejeu | Action Valider fermée réellement après réussite |
| Consolidation du soir | Nouveau bloc ×7, blocs précédents ×1 continu, ancienne session persistée | Affichage de la séquence et compteur exact |
| Finales | Séparation LEARNING_FINAL / CONSOLIDATION_FINAL, crédit uniquement après validation | Dimanche matin/soir, disparition après succès |
| Programme recommandé | 4 Itqān (mardi/jeudi/samedi/dimanche), cibles 8/7/7/8 | Contraintes vraies de pages et versets |
| Programme classique | Cadence antérieure et historique conservés, dimanche sans Révisions | Navigation, absence de cases fantômes |
| Révision passive | Appris éligible, Acquis inchangé, politique 30 min extensible | Défilement et curseur à la fin |
| Quiz | Code du Quiz gelé, corpus croissant, questions et audio de correction gelés | Cycle audio/fin de correction, boutons compacts |
| Progression | Schéma V6 et file d'Itqān protégés, promotions datées | Cohérence pages/lignes avec historique visible |
| Réglages | Programmes, horaires, sources, plages et corpus | Facilité des sélecteurs BOOX, intitulés compacts |
| Sauvegarde/restauration | Sources de sauvegarde non modifiées | Upgrade depuis sauvegarde réelle + redémarrage |
| 23 icônes | Hashs et rôles contractuels contrôlés par CI | Icônes dynamiques et E-Ink (aucune certification statique) |
| Annotations | Moteur non modifié | Crayon/annuler/effacer, masque correct |
| Audio | Moteur non modifié | Pack installé, source et disparition de la barre |
| Orientation | Contrats UI et rendu statique vérifiés | Portrait BOOX, hauteur utile et boutons alignés |
| Sécurité | Pas d'écriture moteur Quiz ni reset des curseurs | Tests de longévité et de reprise sur appareil |

## Observations contradictoires et limites

1. **Ne jamais déduire d'un CI vert que la BOOX a été testée.** Les 23 vecteurs peuvent être exacts tout en étant mal alignés à l'écran. L'audit en appareil (portrait BOOX, E-Ink, toucher, pages 518–522, test Quiz/audio) reste distinct.
2. **Contrat de reprise des anciennes sauvegardes :** les nouvelles sessions persistées disposent du snapshot figé. Des sessions ouvertes sous l'ancien code n'en disposent pas forcément et nécessitent un essai d'upgrade; elles ne doivent pas être silencieusement créditées pour des unités nouvelles.
3. **Changement de programme en milieu de semaine :** le calendrier prend effet à la date du changement, mais une future séance ne doit pas être confondue avec un report ancien. La transition datée et la réconciliation du ledger doivent être observées sur huit semaines.
4. **Classique vs Recommandé :** dimanche reste sans Révision active/passive dans les deux. Les actions directes restent accessibles sans contrainte horaire; aucune absence de session planifiée n'est un blocage d'accès volontaire.
5. **Planning physique 8/7/7/8 :** il s'agit de cibles; les frontières réelles de Mushaf priment. Ne pas présenter une garantie fictive d'exactement 30 lignes si la géométrie force une répartition différente.
6. **Performance passive :** l'enrichissement par lignes Appris reconstruit une projection sur le Mushaf; vérifier la latence lors de l'ouverture d'Entretien sur BOOX.
7. **Incohérence documentaire de l'ancien workflow :** plusieurs étapes `ibn-kathir-engine-frozen.yml` décrivent encore d'anciens brouillons « non approuvés », alors que l'application utilise désormais une amorce **mécanique** autorisée par la nouvelle règle. Les succès de ces étapes ne sont donc pas une preuve de la migration runtime; les nouveaux tests `IbnKathirFirstWordsCorpusTest` le sont.

## Critères bloquants avant J8

Ne pas construire/livrer l'APK tant que : tests et source contracts de la HEAD courante non verts; résidus de snapshot anciens non évalués; anomalie de réapparition non reproduite sur une sauvegarde représentative; régression du Quiz 1.17.1 ou des icônes; comportement masquage Qāf/Adh-Dhāriyāt non observé sur BOOX.

**Livraison logicielle prévue :** un seul APK debug intégré J8, après consolidation des validations, et jamais de release sans accord utilisateur.
