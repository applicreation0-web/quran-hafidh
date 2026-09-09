# Quran Safeguard 0.10.10 — Parcours Hifz
## Premier audit contradictoire indépendant de conception

Date: 2026-09-09
Branche de travail: `work/0.10.10-hifz`
Base: `release/0.10.9-work`

## Verdict provisoire

**NON PRÊT À EXPOSER COMME FONCTIONNALITÉ UTILISATEUR.**

Le socle 0.10.9 fournit un bon lecteur de mémorisation libre, mais son état ne doit pas devenir le calendrier du Parcours Hifz. La première implémentation 0.10.10 doit séparer strictement le planning Hifz du moteur d'exercice Mémorisation.

## Exigences déjà verrouillées

1. Le mode **Mémorisation** 10.9 reste disponible comme mode libre et ponctuel.
2. Le **Parcours Hifz** est distinct et sert au suivi structuré Sabqi–Itqān–Murājaʿah.
3. Jours par défaut:
   - lundi, mercredi, vendredi: Sabqi;
   - mardi, jeudi: Itqān;
   - samedi, dimanche: Murājaʿah.
4. Une séance manquée n'est jamais considérée comme un échec.
5. Une séance manquée ne déplace jamais silencieusement ses bornes.
6. Le curseur et le quota sont conservés à l'identique.
7. Le report est explicite et souple.
8. Aucun doublement automatique de quota pour « rattraper » une séance.

## Audit contradictoire du socle 0.10.9

### A — Séparation Mémorisation / Parcours Hifz

**RISQUE BLOQUANT si réutilisé tel quel.**

Le lecteur libre persiste actuellement dans un même document JSON:
- le mode UI (`READING` / `MEMORIZATION`);
- les sélections;
- les sessions de mémorisation;
- la session active;
- les statuts et dates `due`.

Conclusion: le Parcours Hifz ne doit pas être ajouté dans ce document comme une nouvelle variante de session. Il lui faut sa propre source de vérité native.

### B — Sémantique de `due`

**INCOMPATIBLE avec un calendrier Hifz structuré.**

Les sessions de mémorisation 10.9 utilisent une échéance locale liée au moteur pédagogique et permettent une modification de statut depuis l'interface. Cette échéance n'exprime pas les jours fixes Sabqi–Itqān–Murājaʿah et ne peut pas représenter correctement une tâche manquée/replanifiée.

### C — Identité et reprise des sessions

**À DURCIR.**

Le moteur libre peut créer/recréer des sessions et déplacer `state.active`. Cette mécanique est adaptée à un exercice volontaire, pas à un parcours où une tâche doit conserver une identité stable, son curseur, son quota, sa date d'origine et son historique de replanification.

### D — Séance manquée

**ABSENT dans 10.9.**

Aucune politique dédiée ne garantit qu'une séance manquée:
- passe en retard sans échec;
- conserve son curseur;
- conserve son quota;
- ne soit pas fusionnée avec la séance suivante;
- ne double pas automatiquement la charge du jour.

### E — Tests

**COUVERTURE INSUFFISANTE pour 10.10.**

Les tests 10.9 couvrent fortement le lecteur, le Mushaf, les budgets, les exclusions et plusieurs politiques, mais il n'existe pas encore de suite dédiée au calendrier Hifz.

Le premier lot 10.10 ajoute des tests de contrat pour:
- le rythme hebdomadaire fixe;
- le passage en retard sans mutation du travail;
- le report explicite;
- l'absence de doublement automatique;
- la priorité déterministe au plus ancien retard;
- l'exclusion des tâches terminées.

### F — Nommage UI

**RISQUE UX.**

Le hub Qur'an 10.9 contient déjà:
- `Mémorisation`;
- `Parcours Juz / Hizb`.

Le futur `Parcours Hifz` doit être présenté comme une troisième fonction clairement distincte afin d'éviter que l'utilisateur confonde le parcours de lecture quotidien et le suivi de mémorisation.

### G — Monolithe de persistance du lecteur

**RISQUE À NE PAS ÉTENDRE.**

Le lecteur libre sauvegarde un document JSON complet via le bridge WebView. Ajouter l'historique Hifz, les retards et replanifications dans ce même document créerait un couplage inutile et augmenterait le risque de corruption ou de migration difficile.

## Décision d'architecture après audit

Le Parcours Hifz 10.10 sera construit avec:

1. une **politique de calendrier native** et testable;
2. une **persistance Hifz séparée** du `reader109`;
3. des tâches à identité stable;
4. un statut explicite `PLANNED / OVERDUE / COMPLETED`;
5. un report explicite qui conserve curseur et quota;
6. un seul travail proposé à la fois pour empêcher le doublement automatique;
7. une intégration ultérieure avec le lecteur Mémorisation uniquement comme surface d'exercice, jamais comme source de vérité du planning.

## Travail déjà démarré sur la branche 0.10.10

Fichiers ajoutés:

- `app/src/main/java/com/quranunlock/guard/HifzSchedulePolicy.kt`
- `app/src/test/java/com/quranunlock/guard/HifzSchedulePolicyTest.kt`

Le premier moteur verrouille les invariants de calendrier sans modifier le comportement 0.10.9 existant.

## Points encore bloquants avant première UI Hifz

1. Définir la structure persistée des tâches et son schéma/version.
2. Définir précisément l'unité de quota utilisée par chaque piste sans la déduire du moteur libre.
3. Définir l'identité stable d'un passage Hifz et sa correspondance avec le Mushaf/versets.
4. Implémenter la persistance et les transitions atomiques.
5. Ajouter les tests de redémarrage, changement de date, changement de fuseau, replanification et backlog.
6. Ajouter l'écran Parcours Hifz seulement après ces garanties.
7. Ajouter le contrat d'échange contrôlé avec le lecteur Mémorisation.
8. Faire ensuite un audit contradictoire runtime et migration avant toute release.

## Conclusion

La bonne stratégie n'est pas d'« étendre Mémorisation » en lui ajoutant un calendrier. Le Parcours Hifz doit être un orchestrateur séparé qui utilise éventuellement le lecteur Mémorisation comme outil d'exécution. Cette séparation réduit les collisions de persistance et protège le mode libre 10.9 contre les régressions 10.10.
