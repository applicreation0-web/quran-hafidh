# Quran Hifz — Mise à jour Rétention / Ancrage / Entretien / J10 — v2

Date: 2026-09-15  
Baseline applicative vérifiée: `75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6` (Quran Hifz 0.7.4 BOOX publiée)  
Branche de conception: `work/hifz-retention-j10-redesign-spec`  
Statut: **spécification de mise à jour — aucun code applicatif implémenté à ce stade**

## 1. But de la mise à jour

Simplifier le parcours tout en renforçant la conservation réelle. La modification ne doit **pas** affaiblir les protocoles individuels de Leçon neuve ni d’Ancrage.

Le principe cible devient:

`unité individuelle complète -> renforcement groupé en boule de neige -> acquisition -> Entretien permanent avec garantie J10`.

La principale correction par rapport à la première version de la spécification est la suivante:

- les compteurs, répétitions, masquages et critères de validation **individuels** restent inchangés;
- la boule de neige utilise des **compteurs de renforcement séparés**, avec un quota de répétitions réduit proportionnellement lorsque 2 ou 3 unités sont groupées;
- l’Entretien reste une rotation de matière acquise, et non une nouvelle phase de mémorisation.

## 2. Méthode actuelle 0.7.4 — ce qu’elle fait aujourd’hui

### Leçon neuve
- unité individuelle: 5 lignes;
- compteur principal: ×37;
- progression de masque actuelle: 15 visibles, 5 à 25 %, 5 à 50 %, 5 à 75 %, 7 à 100 %;
- la Reprise du soir porte aujourd’hui essentiellement sur le bloc du jour;
- les blocs passent ensuite dans une file récente et restent soumis à la logique de maturation actuelle.

### Ancrage
Deux protocoles individuels existent et doivent être conservés:

- **FULL ×40**: 15 visibles, 5 à 25 %, 5 à 50 %, 5 à 75 %, 10 à 100 %;
- **LIGHT ×35**: 20 visibles, 0 à 25 %, 5 à 50 %, 5 à 75 %, 5 à 100 %;
- après 3 échecs, un protocole LIGHT bascule en FULL;
- l’unité de travail est actuellement principalement organisée à la page, avec logique spéciale de fractionnement/reconstruction.

### Maturation / Consolidation
Le parcours actuel utilise notamment:
- seuil de 36 blocs avant activation de la Consolidation dominicale;
- âge minimum de 90 jours pour la promotion normale;
- seuil d’assiduité dominicale;
- mécanisme de promotion forcée au-delà de 60 blocs récents.

### Entretien / J10
- Entretien de référence actuel: 45 min;
- vitesse initiale actuelle: 9 s/ligne;
- J10: délai maximal 10 jours;
- anticipation déjà présente: J9 systématique, J8 en tension, J7 si non-tenable;
- le planificateur actuel compte plusieurs types de séances comme capacité potentiellement réutilisable J10, notamment Reprise du soir et Ancrage.

## 3. Méthode cible

### 3.1 Leçon neuve individuelle — **aucun changement pédagogique**

Chaque nouvelle unité Sabqi reste:
- 5 lignes;
- ×37;
- mêmes paliers de masquage;
- mêmes compteurs principaux;
- mêmes règles de validation individuelle;
- même suivi d’aides/révélations et de performance existant.

Le compteur individuel du matin ne doit jamais être remplacé ou augmenté artificiellement par le compteur du renforcement du soir.

### 3.2 Ancrage individuel — **compteurs et répétitions inchangés**

La taille de l’unité change vers une **demi-page Mushaf**, mais le protocole pédagogique individuel reste:
- LIGHT ×35 ou FULL ×40 selon l’état/protocole de l’unité;
- mêmes paliers de masquage que 0.7.4;
- mêmes aides/révélations;
- mêmes critères de validation;
- même escalade LIGHT -> FULL après les échecs prévus;
- même suivi du temps et de la performance.

Le changement concerne donc la **géométrie de l’unité**, pas l’intensité pédagogique individuelle.

### 3.3 Découpage demi-page

Une unité A doit:
- viser environ 7 à 8 lignes physiques;
- rester dans une seule page Mushaf;
- ne jamais traverser une limite de sourate;
- ne jamais couper artificiellement un verset;
- accepter une unité plus courte lorsqu’une fin de sourate impose l’arrêt.

Le réglage « Sourates difficiles à ancrer » et la branche spéciale associée deviennent inutiles pour les nouvelles unités. Les états historiques doivent toutefois être migrés sans perte.

### 3.4 Renforcement « boule de neige » — compteur séparé

Le renforcement groupé ne modifie **jamais** le compteur principal de l’unité individuelle. Il possède son propre état persistant:
- répétitions réalisées;
- quota prévu;
- temps réel;
- masque/étape de renforcement;
- aides/révélations;
- succès/échecs;
- reprise exacte après interruption/process death.

Le renforcement parcourt les unités en alternance, par exemple:
- 2 unités: A1 -> A2 -> A1 -> A2...;
- 3 unités: A1 -> A2 -> A3 -> A1 -> A2 -> A3...;

Il ne faut pas faire toutes les répétitions de A1 avant de passer à A2.

### 3.5 Quotas de répétitions groupées

Le volume global du renforcement reste proche d’**une unité de base**, puis est réparti sur les unités du groupe.

| Groupe | Sabqi base ×37 | Ancrage LIGHT ×35 | Ancrage FULL ×40 |
|---|---:|---:|---:|
| 1 unité | 37 | 35 | 40 |
| 2 unités | 19 + 18 | 18 + 17 | 20 + 20 |
| 3 unités | 13 + 12 + 12 | 12 + 12 + 11 | 14 + 13 + 13 |

Règle conservatrice pour un groupe d’Ancrage mélangeant LIGHT et FULL: **si au moins une unité est FULL, le budget groupé est calculé sur la base FULL**. Claude devra auditer ce choix et signaler s’il crée une charge ou une incohérence excessive.

Le masque de renforcement doit suivre les **mêmes proportions pédagogiques** que le protocole individuel, mais comprimées sur le quota réduit. Cette progression est séparée et ne modifie pas les paliers du compteur individuel.

### 3.6 Planning cible

| Jour | Matin — unité individuelle | Soir — renforcement / Entretien |
|---|---|---|
| Lundi | Leçon neuve S1 ×37 | Renforcement S1, compteur séparé |
| Mardi | Ancrage A1 demi-page ×35/×40 | Renforcement A1, puis Entretien base 60 min |
| Mercredi | Leçon neuve S2 ×37 | Renforcement S1+S2, quota 19+18 |
| Jeudi | Ancrage A2 demi-page ×35/×40 | Renforcement A1+A2, puis Entretien base 60 min |
| Vendredi | Leçon neuve S3 ×37 | Renforcement S1+S2+S3, quota 13+12+12; lot Sabqi acquis si critères atteints |
| Samedi | Ancrage A3 demi-page ×35/×40 | Renforcement A1+A2+A3, puis Entretien base 60 min; lot Ancrage acquis si critères atteints |
| Dimanche | Réserve J10 uniquement si nécessaire | Entretien base 60 min |

**Impact important:** mardi/jeudi/samedi soir comportent deux blocs distincts: renforcement Ancrage puis Entretien. Ils ne doivent pas partager leurs compteurs. Cette charge du soir doit être affichée clairement et ne doit pas être cachée dans le calcul J10.

### 3.7 Acquisition

**Sabqi:** S1+S2+S3 deviennent acquis après le renforcement groupé du vendredi soir, uniquement si:
- chaque unité individuelle a été validée;
- le quota groupé du vendredi a réellement été effectué;
- aucune ligne non parcourue n’est créditée.

`lastReviewed` initial est la date réelle du dernier renforcement validé, normalement vendredi.

**Ancrage:** A1+A2+A3 deviennent acquis après le renforcement groupé du samedi, uniquement si:
- chaque demi-page a validé son protocole individuel LIGHT/FULL;
- le quota groupé du samedi est terminé;
- aucune aide/étape non satisfaite ne permet une acquisition fictive.

`lastReviewed` initial est la date réelle de cette dernière revue, normalement samedi.

Si la séance groupée est interrompue, son compteur séparé reprend exactement au même point. Le lot reste non acquis tant que les critères ne sont pas satisfaits.

## 4. Suppression de l’ancien sas long

Le runtime cible retire du nouveau parcours:
- attente de 90 jours;
- seuil de 36 blocs pour Consolidation;
- assiduité 20/26 comme condition de promotion;
- promotion forcée au-delà de 60 blocs;
- Consolidation dominicale utilisée comme gare avant Ancrage.

Les classes/états historiques ne doivent être supprimés qu’après migration sûre. Ils peuvent rester temporairement comme compatibilité de migration, mais ne doivent plus décider du parcours nouveau.

### Avantage
Le contenu validé entre rapidement dans la rotation permanente, ce qui évite un long sas opaque.

### Risque
Le corpus acquis grossit plus vite. La qualité dépend donc davantage de la fiabilité du J10. Une erreur de crédit J10 devient plus grave qu’avant.

## 5. Corpus acquis / À ancrer

Le stockage doit distinguer explicitement:
- **Corpus acquis**: éligible Entretien + J10;
- **À ancrer**: non éligible Entretien tant que protocole individuel + renforcement ne sont pas validés.

L’interface doit autoriser plusieurs plages dans les deux listes.

Invariants:
- aucune ligne ne peut être dans les deux états simultanément;
- tout chevauchement est refusé explicitement;
- aucune correction silencieuse;
- suppression/édition d’une plage acquise doit réconcilier J10 de façon explicite et testée;
- les données 0.7.4 déjà considérées acquises ne doivent pas redevenir « à ancrer » par erreur.

## 6. Entretien

### 6.1 Durée

**60 minutes = durée de référence, pas blocage horaire.**

L’utilisateur peut:
- arrêter avant 60 min;
- continuer après 60 min;
- sortir/reprendre sans perdre le curseur.

Seules les lignes réellement récitées sont créditées.

### 6.2 Vitesse

- nouvelle estimation initiale: **7 s/ligne**;
- une vitesse réellement calibrée sur l’appareil est préservée;
- une ancienne valeur 9 s/ligne non calibrée migre vers 7 s/ligne;
- 7 s/ligne s’applique à la planification Entretien/J10, **pas** à la mémorisation Sabqi/Ancrage.

Capacité théorique à 7 s/ligne:
`floor(3600 / 7) = 514 lignes par heure`.

### 6.3 Rotation hybride

Pendant Entretien:
1. servir d’abord les lignes menacées par J10;
2. reprendre ensuite la rotation normale depuis son curseur persistant;
3. une préemption J10 ne doit pas déplacer définitivement le curseur normal.

Le principe existant de priorité doit être conservé:
- J9/J10: priorité systématique;
- J8: priorité si tension/non-normal;
- J7: priorité si non-tenable.

### 6.4 Multi-tour

Le couple `début -> fin` n’est plus suffisant pour créditer une séance.

Le stockage de séance doit permettre de prouver:
- IDs de lignes réellement parcourues;
- nombre total d’occurrences/lignes parcourues;
- tours complets éventuels;
- curseur final;
- date de revue effective.

Un petit corpus parcouru deux fois ne doit jamais être confondu avec un seul parcours.

## 7. J10 et croissance du corpus

La règle reste stricte:
`deadline(line) = lastReviewed(line) + 10 jours`.

Capacité J10 normale à compter:
- mardi soir Entretien: 60 min;
- jeudi soir: 60 min;
- samedi soir: 60 min;
- dimanche soir: 60 min.

**Ne pas compter** comme capacité J10 disponible:
- les renforts Sabqi;
- les renforts Ancrage;
- les unités individuelles Sabqi/Ancrage.

Le dimanche matin reste une réserve J10 de 60 min uniquement lorsque le forecast le justifie.

À 7 s/ligne:
- 1 séance: 514 lignes théoriques;
- 4 séances/semaine: 2 056 passages théoriques;
- pire fenêtre de 10 jours: 5 séances normales ≈ 2 570 lignes;
- avec réserve: 6 séances ≈ 3 084 lignes.

Le seuil de tension à 80 % donne environ 2 056 lignes dans cette fenêtre. Ce n’est pas un blocage; c’est un signal de soutenabilité.

Croissance typique si 3 Sabqi + 3 demi-pages Ancrage sont acquis chaque semaine:
- Sabqi: 15 lignes/semaine;
- Ancrage: environ 21 à 24 lignes/semaine;
- total: environ 36 à 39 nouvelles lignes/semaine.

Le système doit afficher NORMAL / TENSION / NON TENABLE sans masquer un déficit d’échéance précoce par une capacité disponible après la deadline.

## 8. Où le code doit changer

| Fichier / zone | Changement prévu | Risque |
|---|---|---|
| `hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt` | Planning hebdomadaire, types/créneaux, suppression du rôle runtime de l’ancienne Consolidation | Élevé |
| `hifz-app/.../PreviewConfig.java` | `SCHEMA_VERSION` -> 6; vitesse par défaut 7; garder ×37/×35/×40; ajouter règles de quotas groupés | Élevé |
| `hifz-app/.../HifzPrefs.java` | Nouveaux états persistants: groupes hebdo, compteurs de renforcement, acquis vs à ancrer, migration | Très élevé |
| `hifz-app/.../HifzSessionActivity.java` | UI/runtime des deux compteurs, alternance 2/3 unités, acquisition hebdo, Entretien non bloquant | Très élevé |
| `hifz-app/.../GeometryRepository.java` | Générateur demi-page respectant page/sourate/verset | Élevé |
| `hifz-app/.../AnchoringQueue.java` | Queue de demi-pages, conservation LIGHT/FULL, migration des unités historiques | Élevé |
| `hifz-app/.../J10ReviewPlanner.java` | Capacité J10 fondée sur Entretien uniquement + réserve; rotation hybride; croissance | Très élevé |
| `hifz-app/.../J10ReviewObserver.java` | Crédit seulement après événements réels; plus de crédit prématuré de matière non acquise | Très élevé |
| `hifz-app/.../J10ReviewStore.java` | Réconciliation exact-set lors édition du corpus; aucune ligne fantôme | Élevé |
| `hifz-app/.../HifzSpeedStore.java` + `SpeedCalibration.java` | Migration 9 -> 7 seulement si non calibré; préserver mesures réelles | Moyen |
| `hifz-app/.../SettingsActivity.java` | Deux éditeurs: Corpus acquis / À ancrer; retirer Sourates difficiles | Élevé |
| `hifz-app/.../WeeklyDashboardPlanner.java` + accueil | Planning, charge du soir, forecast J10, réserve dimanche | Moyen/élevé |
| `RecentPromotionPolicy.java`, `ConsolidationAttendance.java` | Sortie du runtime; garder uniquement compatibilité/migration si nécessaire | Moyen |
| tests + `migration-fixture` | Couvrir tous les états 0.7.4 et nouveaux invariants | Très élevé |

Chemins `hifz-app/...` ci-dessus désignent `hifz-app/src/main/java/com/quransafeguard/hifz/preview/`.

## 9. Migration 0.7.4 -> schéma 6

La migration doit être atomique et idempotente.

Règles:
- `itqanRanges` historiques déjà visibles par Entretien -> **acquis**;
- `unconsolidatedPromotedRanges` -> **à ancrer**;
- promotions historiques déjà consolidées -> acquis;
- Ancrage individuel en cours: conserver compteur, protocole, masque, échecs et progression;
- unité commencée: ne pas la couper au milieu; finir avec son format historique puis passer aux demi-pages;
- unités non commencées: convertir en demi-pages;
- préserver dates J10 réelles;
- aucune date ne doit être « rafraîchie » par la migration seule;
- vitesse non calibrée 9 -> 7;
- vitesse calibrée -> intacte;
- compteurs individuels Sabqi/Ancrage -> intacts;
- renforcement groupé nouveau -> initialisé vide, sans inventer de répétitions historiques.

## 10. Risques principaux et protections

### R1 — Fausse acquisition
Risque: un groupe devient acquis alors qu’un compteur individuel ou groupé n’est pas fini.  
Protection: transaction unique de validation + tests négatifs + aucune acquisition sur simple ouverture/temps écoulé.

### R2 — Mélange des compteurs
Risque: le renforcement du soir augmente le ×37/×35/×40 individuel.  
Protection: clés persistantes/types différents; assertions et tests de non-interférence.

### R3 — Masquage incorrect
Risque: le quota réduit saute la phase 100 % ou modifie le masque individuel.  
Protection: fonction pure de progression de renforcement, tests exhaustifs pour 1/2/3 unités et LIGHT/FULL.

### R4 — Double charge du mardi/jeudi/samedi soir
Risque: renforcement Ancrage + Entretien 60 min devient lourd.  
Protection: affichage séparé des deux blocs, quota groupé plafonné, Entretien non bloquant, forecast J10 honnête.

### R5 — Crédit J10 fantôme
Risque: une ligne affichée/non récitée est marquée revue.  
Protection: crédit uniquement sur validation d’un parcours réel; IDs exacts; process-death tests.

### R6 — Corpus qui grossit plus vite
Risque: 1 h ×4 devient insuffisant.  
Protection: forecast quotidien, tension 80 %, réserve dimanche, NON TENABLE explicite.

### R7 — Migration destructive
Risque: perte de progression 0.7.4 ou réinitialisation de dates.  
Protection: fixtures de migration, tests idempotence, snapshot avant/après, aucun overwrite silencieux.

### R8 — Multi-tour incorrect
Risque: un petit corpus fait plusieurs tours mais le système n’en crédite qu’un, ou inversement.  
Protection: journal de traversal/IDs et compteur d’occurrences séparé du set unique J10.

### R9 — Régression BOOX/E-Ink
Risque: compteurs/masques de groupes provoquent davantage de clignotements.  
Protection: réutiliser les politiques E-Ink actuelles; stress test transitions de masque et compteur sans full refresh inutile.

## 11. Comparaison synthétique ancienne / nouvelle méthode

### Ancienne méthode — points forts
- prudente avant de déclarer la matière acquise;
- Ancrage individuel exigeant;
- sas long qui évite une acquisition trop rapide;
- J10 existe déjà et possède une logique d’anticipation utile.

### Ancienne méthode — limites
- parcours long et difficile à comprendre;
- 36 blocs + Consolidation + 90 jours + assiduité + promotion + Ancrage = beaucoup d’états;
- Sabqi récent peut rester longtemps hors Entretien;
- `itqanRanges` a une sémantique UI ambiguë;
- capacité J10 peut compter du temps déjà utilisé par d’autres séances;
- crédit début/fin insuffisant pour les multi-tours.

### Nouvelle méthode — points forts
- aucune baisse d’exigence sur les unités individuelles;
- renforcement espacé dans la même semaine;
- acquisition plus rapide mais prouvée;
- séparation claire acquis / à ancrer;
- J10 devient le mécanisme central de conservation longue;
- charge future mesurable et visible.

### Nouvelle méthode — limites / coût
- plus de données persistantes;
- migration plus complexe;
- soirées mardi/jeudi/samedi plus chargées;
- corpus acquis augmente plus vite;
- une erreur J10 a un impact plus critique;
- besoin de tests process death et multi-tour plus poussés.

## 12. Critères de GO avant implémentation finale

La mise à jour ne peut être déclarée prête que si:
- Sabqi individuel est toujours exactement ×37 avec le même masque;
- Ancrage individuel conserve LIGHT ×35 et FULL ×40 + bascule prévue;
- aucun renforcement groupé ne modifie ces compteurs individuels;
- quotas 1/2/3 unités sont déterministes et testés;
- demi-page ne traverse ni page ni sourate et ne coupe pas de verset;
- acquisition ne se produit qu’après validations individuelles + quota groupé;
- corpus acquis / à ancrer ne se chevauchent jamais;
- migration 0.7.4 est idempotente et sans perte;
- vitesse calibrée est préservée;
- 7 s/ligne n’est qu’une base Entretien/J10;
- Entretien 60 min n’est pas bloquant;
- aucune ligne non récitée n’est créditée J10;
- J10 n’utilise pas les minutes de Sabqi/Ancrage comme capacité libre;
- multi-tour et process death sont testés;
- les tests existants ne sont pas seulement verts: les nouveaux tests doivent démontrer les invariants.

## 13. Hors scope

Cette mise à jour ne doit pas modifier:
- Mushaf 604 pages / géométrie canonique autrement que pour sélectionner des demi-pages;
- audio / packs Al-Husary;
- Tafsir;
- mécanismes de signature/publication;
- package applicatif;
- cosmétique globale hors écrans nécessaires au nouveau parcours.

Aucun merge, aucune release et aucune publication ne sont inclus dans cette phase de conception.
