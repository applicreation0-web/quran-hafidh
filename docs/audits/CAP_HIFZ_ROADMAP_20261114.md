# Qualification de la cadence Hifz

Le programme recommandé ouvre Sabqi le lundi, mercredi et vendredi, puis Itqān le mardi, jeudi et samedi. La proposition de cadence reste consultative après Al-Baqara.

| Point vérifié dans le dépôt | Conséquence |
| --- | --- |
| `HifzPrefs.currentRoadmapDecision()` n'expose que `CURRENT`. Le verrou An-Nās, la fin physique de Sabqi et les mesures datées de retard n'alimentent pas la décision. | Aucune transition automatique vers `BRIDGE`, `CATCHUP`, `CRUISE`, `POST_SABQI` ou `PERMANENT` ne peut être qualifiée avec ce branchement. |
| `weeklyItqanDay()` n'ouvre une page hebdomadaire neuve que les trois jours Itqān du programme recommandé. Une page ordinaire est découpée en trois créneaux, et la fermeture de la page exclut les autres jours de cette semaine. | Un ratio qui annonce six jours Itqān ne suffit pas à ouvrir six séances réelles. Changer le ratio visible avant de définir leur traitement introduirait des créneaux sans séance, ou du crédit sans leçon. |
| `weeklyProgramFor()` date les changements de programme. `setLearningDaysPerWeek()` conserve l'entier actuel seulement. | Un nouveau ratio appliqué à l'ancien calendrier requalifierait des jours passés. La présence à ces créneaux ne peut pas être reconstruite avec le seul réglage courant. |
| Le verrou An-Nās est irréversible et le Maintien est une lecture complète d'un juz visible puis masqué. | Le retard Itqān du premier parcours et la disponibilité réelle du Maintien exigent des mesures distinctes. Une finale Itqān doit rester fermée après An-Nās, avec reprise possible d'un ancien dossier OPEN. |
| Le curseur Sabqi peut atteindre une limite de plage configurée. Les finales disposent d'un journal distinct des séances ordinaires. | La fin Al-Fatḥ doit être prouvée par le reçu physique final. Un curseur reconfiguré, une fin de sous-plage ou une finale ouverte ne prouve pas cette fin. |

## Conditions de raccordement

- Choisir une règle complète pour les jours Itqān supplémentaires d'une semaine 2/4 ou 0/6 avant An-Nās, en cohérence avec la page hebdomadaire, la reprise et ses finales.
- Persister une décision de cadence par semaine effective avec les preuves de clôture et les lignes du périmètre de retard. Résoudre le calendrier à la date de la séance dans le moteur, l'agenda, les projections et les paramètres.
- Préserver les OPEN et leurs protocoles. Après des semaines sans sauvegarde de clôture, récupérer le dernier état prouvé sans générer d'anciennes séances ni de reçus.
- Vérifier sur Android les changements de dimanche à lundi, les absences de 1 à 8 semaines, les transitions de mois et d'année, les doubles tentatives de clôture, anciens OPEN, finales partielles, An-Nās et Al-Fatḥ, puis l'import et la mise à jour in-place.

La présentation de Cap Hifz et la qualification de la cadence sont suivies sur des branches distinctes. Les réglages de cadence existants continuent d'être lus par les parcours actuels.
