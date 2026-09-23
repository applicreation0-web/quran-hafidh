# Quran Hifz — matrice de non-régression 0.7.3 → 0.7.4

Date: 2026-09-15

## Références exactes

- 0.7.3 applicatif: `da0a79a480e22d683ac5e4d2d61a11dc589a8226`
- Métadonnées 0.7.3: `versionCode=10`, `versionName=0.7.3-boox`
- 0.7.4 publié: `75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6`
- Métadonnées 0.7.4: `versionCode=11`, `versionName=0.7.4-boox`
- Delta: 0.7.4 est 74 commits devant le HEAD 0.7.3 ci-dessus.

Cette matrice est un gate P0 pour 0.7.5. Une ligne non protégée par un test durable doit recevoir un test avant que la fonctionnalité 0.7.5 concernée ne soit considérée terminée.

| ID | Régression / risque corrigé entre 0.7.3 et 0.7.4 | Garde 0.7.4 existant | Exigence 0.7.5 |
|---|---|---|---|
| R01 | Crédit J10 dépendant d’un libellé UI visible au lieu de champs structurés | `PreBoox074SourceContractTest.c1StructuredCreditsDoNotDependOnVisibleLabels` + `J10ReviewObserverInstrumentedTest` | Conserver le chemin structuré. Les nouveaux crédits Entretien/J10 ne doivent jamais être dérivés d’un texte visible. |
| R02 | Forecast/priorité J10 avec effets de bord ou synchronisation dans une lecture pure; risque d’ANR/latence | `PreBoox074SourceContractTest.c2C3C13J10IsAsyncPureAndHasNoOpeningPriority` | Les nouveaux forecast schema-6 restent purs; toute IO/sync hors chemin de calcul UI. |
| R03 | J10 jugé soutenable sur la fenêtre globale alors qu’une échéance précoce est déjà manquée | `AstraFixPolicyTest.j10ForecastFailsAtFirstMissedDeadlineNotEndOfWindow` | Le forecast 0.7.5 doit échouer à la première échéance non couverte. Aucun budget ultérieur ne masque un déficit antérieur. |
| R04 | Historique J10 inconnu traité comme J0 et donc artificiellement repoussé | `AstraFixPolicyTest.unknownHistoricalAcquisitionStartsDueNotAtJ0` | Schema 6 remplace le faux seed par `UNKNOWN_DUE`, dû immédiatement, sans fausse `LocalDate`. |
| R05 | Validation J10 possible avant affichage/parcours de toute la matière requise | `AstraFixPolicyTest.j10CannotValidateUntilEveryRequiredPageWasShown` | Le crédit 0.7.5 est encore plus strict: uniquement lignes réellement visitées ET éligibles. |
| R06 | Temps passé en J10 non imputé au budget de la séance hôte | `AstraFixPolicyTest.j10ElapsedTimeConsumesHostSessionBudget` + `J10HostBudgetStoreInstrumentedTest` | Maintenir la comptabilité hôte; distinguer capacité Entretien/J10 des minutes Sabqi/Ancrage/renforcement. |
| R07 | Fermeture immédiate du J10 pouvant bloquer toute préemption future | `J10PreemptionLifecycleInstrumentedTest.immediateJ10CloseDoesNotDisableTheNextPreemption` | Le nouveau flux réserve/préemption doit préserver ce cycle de vie et ne jamais latcher une suppression permanente. |
| R08 | Date de séance modifiée après minuit/restart et crédit J10 attribué au mauvais jour | `HifzSessionDateInstrumentedTest.expiredEveningReviewRestartedAfterMidnightCommitsAndCreditsRecordedDate` + garde source C12 | Toute séance 0.7.5 capture sa date une fois; restart/process death ne la remplace pas par `today`. |
| R09 | Progression Ancrage partielle perdue après kill/restart/process death | `ProcessDeathPersistenceInstrumentedTest` | Gate P0: préserver `itqanBlockIndex`, répétitions, aide, révélations, start/end, protocole, queue et tous nouveaux états v6 après interruption. |
| R10 | Unité Ancrage en cours changée par réordonnancement de queue/promotion | `AnchoringProtocolContinuityInstrumentedTest.fullFractionatedUnitKeepsFullAcrossQueueReorder`, `lightFractionatedUnitKeepsLightAcrossQueueReorder`, `startEndAndProtocolArePreservedExactly` | Une unité legacy active 0.7.4 reste exactement la même jusqu’à fin/abandon explicite; aucune conversion demi-page en cours d’unité. |
| R11 | FULL/LIGHT perdu après restart alors que répétitions ou bloc étaient déjà commencés | `AnchoringProtocolContinuityInstrumentedTest.killRestartWithItqanRepAboveZeroResumesSameUnitAndProtocol`, `killRestartWithItqanBlockIndexAboveZeroResumesSameUnitAndProtocol` | Le protocole individuel, le `planProtocol` groupé et le progrès persistent séparément, sans downgrade/upgrade silencieux. |
| R12 | Dashboard et écran de séance en désaccord sur l’unité/protocole courant | `AnchoringProtocolContinuityInstrumentedTest.todayCardAndSessionScreenAgreeOnUnitAndProtocol` | Toute UI 0.7.5 dérive du même état persistant; aucun calcul parallèle divergent. |
| R13 | Validation prématurée d’un Ancrage fractionné ou double crédit J10 | `AnchoringProtocolContinuityInstrumentedTest.noPrematureValidationAndNoDoubleCredit` | Les blocs historiques terminés seuls peuvent alimenter l’overlay; le reste reste pending/quarantaine selon v4.3. Aucun double crédit. |
| R14 | Fractionnement traversant une sourate / géométrie incohérente | garde source C23 + `C23SurahAwareAnchoringInstrumentedTest` + `AnchoringProtocolContinuityInstrumentedTest.crossSurahBlockSelectionSurvivesActivityRecreation` | Le nouveau demi-page a limites page ET sourate dures; tests corpus complet 604 pages obligatoires. |
| R15 | Entrées géométriques utilisables avant chargement/avec état invalide, provoquant crash ou comportement non déterministe | `PreBoox074SourceContractTest.c19GeometryEntryPointsFailClosed` | Loader v6 fail-closed pour `LineMeta.verses` vide ou >15 lignes/page; aucune action Hifz si géométrie invalide. |
| R16 | Migration historique écrasant progression non liée ou se répétant à chaque ouverture | `HifzPrefsV4MigrationInstrumentedTest.schemaThreeMigrationIsAtomicAndPreservesUnrelatedProgress`, `schemaOneMigratesThroughV2V3ToV4WithoutLosingProgress`, `migratedV1StateReopensAfterProcessDeathWithoutSecondMigration` | Migration 5→6 doit comparer snapshots avant/après, être idempotente et ne perdre aucun Sabqi/Itqān/curseur/date/calibration installé. |
| R17 | Page Ancrage validée téléportant le curseur Entretien | `HifzPrefsV4MigrationInstrumentedTest.validatedAnchoringPageJoinsMaintenanceWithoutTeleportingItsCursor` | Le transfert `À ancrer → Acquis` ne reset ni ne téléporte le curseur Entretien. |
| R18 | Échec Ancrage laissant du temps écoulé résiduel ou boucle immédiate sur une seule entrée | `HifzPrefsV4MigrationInstrumentedTest.failedAnchoringAtomicallyClearsElapsedAndSinglePageDoesNotImmediateLoop` | Les nouveaux échecs/report conservent la même propriété d’atomicité et évitent tout double quota/boucle immédiate. |
| R19 | Complétion d’une entrée absente de la queue acceptée silencieusement | `HifzPrefsV4MigrationInstrumentedTest.completedAnchoringEntryMissingFromQueueFailsClosed` | Toutes transitions v6 fail-closed si identité/cycle/unité persistée incohérente. |
| R20 | Nouvelle promotion préemptant une unité fractionnée déjà commencée | `AstraFixPolicyTest.newPromotionIsInsertedBeforePendingReconstruction` + `inProgressFractionatedEntryIsNeverPreemptedByNewPromotion` | Une unité en cours garde priorité; nouvelles unités/cycles n’écrasent jamais une session OPEN. |
| R21 | Chrome Study masqué mais espace non rendu au Mushaf | `HifzUiLayoutInstrumentedTest.hiddenStudyChromeReleasesItsLayoutHeight` | Toute nouvelle UI doit conserver ce comportement et réutiliser les primitives design existantes. |
| R22 | Titre long Ancrage tronqué/ellipsisé sur BOOX | `HifzUiLayoutInstrumentedTest.structuredSessionHeaderAllowsLongAnchoringTitleToWrapWithoutEllipsis` | Les nouveaux libellés renforcement/quarantaine ne doivent pas recréer clipping/ellipsis destructeur. |
| R23 | Régressions interaction E-Ink: ACTION_CANCEL, timeout rendu, invalidation non-EInk, contrôles compacts | `PreBoox074SourceContractTest.c5ToC9AndEinkUiHardeningRemainPresent` + C14–C21 | Les nouvelles vues passent le cosmetic contract et validation physique téléphone puis BOOX. |
| R24 | Icônes/actions UI non mappées ou hors famille visuelle | `PreBoox074SourceContractTest.c22EveryLiteralUiActionLabelIsPinnedByTheMappingTest` + `UiIconMappingTest` | Toute nouvelle action visible 0.7.5 doit être ajoutée au mapping/design contract, pas d’icône ad hoc. |
| R25 | Workflow audio personnel embarqué réintroduit dans le produit | `PreBoox074SourceContractTest.personalEmbeddedAudioWorkflowIsAbsent` | Audio hors périmètre 0.7.5; aucun workflow/fichier audio ajouté. |

## Tests dimensionnants supplémentaires obligatoires en 0.7.5

Les gardes ci-dessus prouvent l’absence de régression 0.7.3→0.7.4 mais ne suffisent pas pour les nouveautés v4.3. Ajouter avant GO candidat:

1. **Migration 0.7.4 installée → schema 6**: snapshots champ-par-champ de Sabqi/Itqān terminé et partiel, compteurs, protocoles, J10, curseurs, vitesse calibrée; process death avant/après chaque phase.
2. **Conflit stable/pending historique**: quarantaine explicite, date préservée, aucune démotion silencieuse.
3. **Store J10 legacy**: byte-for-byte inchangé après `schema=6`, y compris après une vraie séance 0.7.5.
4. **Renforcement**: tables complètes 1/2/3, mélanges LIGHT/FULL, LIGHT→FULL pendant session, reprise après process death, preuve de non-interférence avec les compteurs individuels.
5. **Entretien**: 0/1/plusieurs tours, corpus discontinu, doublons le même jour, arrêt partiel, préemption puis reprise du curseur normal, crédit uniquement `visited ∩ acquiredCreditLines`.
6. **Géométrie**: 604 pages, max 15 lignes/page, aucune ligne sans verset, limites page/sourate, tailles 8/9/10/11/12/15, fixture déterministe de partition.
7. **UI**: chaque nouvel état (quarantaine, dette héritée, reinforcement cycle/session, demi-page) testé via runtime/instrumentation et vérifié physiquement sur téléphone avant BOOX.

## Gate

Aucun échec de cette matrice ne peut être accepté comme «hors périmètre» du 0.7.5. Toute régression constatée = `NO-GO` jusqu’à reproduction, test rouge, correction et re-test vert.
