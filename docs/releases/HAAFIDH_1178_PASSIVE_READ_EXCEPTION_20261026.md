# Exception ciblée au gel de HifzPrefs — audit 1.17.8

Le rejeu local d'une copie de la sauvegarde v3/schéma 6 a reproduit une
`IllegalStateException: Physical line ids are not in canonical order` dans
`HifzPrefs.murajaahCorpus(GeometryRepository)`. Les identifiants appris peuvent
être enregistrés selon une chronologie différente de leur position physique.
`GeometryRepository.versesOnLines` exige un ordre physique strict.

Le défaut est également reproduit sans aucune donnée privée par
`PassiveApprisAndSundayContractTest.passiveCorpusAcceptsChronologicalLearnedIdsWithoutReorderingSavedProgress`.
Le test échoue avant correction et réussit après correction.

Le module post-finale optionnel ne peut corriger cette méthode centrale : la
révision passive et le calendrier l'appellent indépendamment de toute lecture
post-finale. Une réparation des préférences enregistrées aurait au contraire
réécrit l'état utilisateur, ce qui est exclu.

L'exception autorisée par le mandat est donc utilisée pour construire uniquement
une copie ordonnée des identifiants lors de cette lecture. Aucune écriture de
préférence, initialisation, migration, transition, crédit, date, finale ou
sourceSnapshot n'est modifiée. Les identifiants inconnus restent rejetés.

`verify_1178_family_invariants.py` reconstruit exactement le fichier officiel
1.17.7 en retirant ce seul adaptateur et compare tous les octets restants. Les
trois autres fichiers protégés restent identiques à la 1.17.7.

Sur la copie privée : corpus admissibles de 4 pages Sabqi et 7 pages Itqān,
56 lignes de calendrier sur huit semaines, aucun changement des acquis ni des
finales OPEN. Le calcul du calendrier peut créer son cache de plan Itqān
`itqanLegPlanV1.TAIL_HUJURAT_NAS` ; cela ne constitue ni une validation ni un
crédit. Aucune lecture cumulative rétroactive du 25 octobre n'est créée.

La sauvegarde privée ne figure ni dans le dépôt ni dans la CI. Cette note ne
constitue pas une autorisation de publication.
