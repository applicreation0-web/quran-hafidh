# Pré-release 1.17.11 — audit de validations fantômes, 6 novembre 2026

**NO-GO SOURCE / NO-GO RELEASE. Travail partiel, non intégrable en l'état.**

La branche d'audit indépendante est `audit/haafidh-11711-phantom-validations-20261106`, issue de `6c9ce6f045310c905e18ecb6bd34014958395c78`. La PR n°9 et sa branche candidate restent inchangées ; sa base `216f4d4b26e048656bc224fe1ade42c99240184d` a été vérifiée comme ancêtre. Aucun merge, rebase, assemblage, signature, asset privé ou export personnel.

## Résultat utile et limite

Des contournements de persistance ont été reproduits par des assertions JVM RED, puis corrigés et retestés. Un P0 supplémentaire reste effectivement RED : `AuditLegacyItqanPayload11711Test.earnedItqanRepetitionsMustNotAuthenticateForeignLinesOrSkipToEnd`.

Cette ancienne clôture Itqān accepte des répétitions de 52:1–10 pour créditer des lignes de la page 604 et le curseur fourni `114:6`. Elle fabrique alors un certificat quotidien portant le mauvais matériel. Le garde du soir ne peut pas compenser une mauvaise preuve produite en amont. Ce défaut interdit de transférer les correctifs vers la candidate et interdit tout GO source. Les vérifications manquantes ci-dessous interdisent également un GO, même si cet échec était seul corrigé.

Le correctif restant exige de lier le lot physique, le protocole, le bloc, le curseur suivant et la date de l'ancienne séance Itqān à une preuve persistante, de conserver ses OPEN authentiques et de contrôler la méthode de clôture. Les écritures de répétitions doivent refuser un identifiant de séance périmé, comme le nouveau chemin Sabqi. Ne pas remplacer le test RED par un contrat de présence de méthode, ni désactiver le test.

## Correctifs proposés, réservés à la branche d'audit

- Ancienne clôture du soir par date seule : refuse sans écrire ni supprimer une OPEN.
- Chaque reçu du soir, y compris le premier : exige la session OPEN exacte, entièrement répétée et persistée, avec date/session/unités/protocoles concordants.
- Renforcement : exige un certificat de leçon daté et physique, écrit dans la même transaction que la leçon. Une date scalaire ne suffit pas. Les certificats restent durables et sont conservés par le codec de sauvegarde.
- Itqān : contrôle du prérequis sur ses jours ; maintien des rattrapages historiques hors jour Itqān. Une passe continue ×1 ne peut pas remplacer le premier reçu ×7 d'un bloc non acquis au protocole.
- Sabqi : lot canonique et date gelés avant répétition ; répétitions liées à un identifiant de séance et un compteur attendu ; refus d'une écriture tardive d'une ancienne fenêtre ; compteur complet et aide admissible exigés avant crédit ; payload et curseur vérifiés.
- Sabqi ancien à compteur seul, sans date authentifiable : garde son état et demande une reprise explicite, au lieu d'attribuer les répétitions à la date de mise à jour.
- Fragments Sabqi 1–4 lignes et Itqān 1–2 lignes : suppression des raccourcis vers Acquis sans protocole ; Sabqi borné à la fin de plage et protocole normal. Les anciens endpoints de crédit direct et Sabqi pré-V6 refusent. Aucun reçu légitime existant n'est effacé.

Ces propositions ne constituent pas une migration certifiée sur la sauvegarde personnelle. Le nouveau traitement des anciens compteurs Sabqi et des OPEN doit encore être reçu sur un clone de sauvegarde réelle et sur Android.

## Résultats de la dernière suite locale

**1 150 cas JUnit exécutés : 1 149 PASS, 1 FAIL, 0 ignoré.** Parmi eux, 297 contrats de source ; ils ne sont pas comptés comme des stress tests. Le seul échec final est le P0 legacy Itqān nommé ci-dessus. Résultats par cas : [JUnit XML](evidence/junit.xml), [CSV](evidence/cases.csv), [log intégral](evidence/verified-suite.log), [métriques](evidence/metrics.json). Les empreintes des trois sources de production effectivement recompilées sont dans [le manifeste](evidence/tested-production-sha256.json).

## Preuves et méthode d'exécution

Les tests exécutent réellement HifzPrefs, les moteurs, le codec JSON et SharedPreferences de test. La copie de préférences est synthétique et ne contient aucune sauvegarde personnelle. Les tests de source sont comptés séparément dans les résultats CSV ; leurs assertions ne sont pas des stress tests.

Environnement local : Java 21, compilation `-source 17 -target 17` des trois classes Android modifiées et de tous les tests Java. Dépendances et classes inchangées : runtime public exporté par le run historique 42206651132, artifact 13496079326, dont `workflow_run.head_sha` est `6c9ce6f045310c905e18ecb6bd34014958395c78`. Les classes recompilées priment sur cet export dans le classpath. Il s'agit de nouveaux tests JVM sur les sources modifiées, **pas** d'une nouvelle compilation Gradle/Android ni d'une réexécution des anciens jobs GitHub. Le corpus sémantique public généré et les coordonnées QCF ont été rematérialisés et vérifiés par les scripts du dépôt.

Commandes reproductibles, une fois le runtime public exporté et les classes compilées :

```sh
# Depuis hifz-app : plusieurs contrats historiques exigent ce répertoire.
java -Xmx1g -cp "$AUDIT_CLASSES:$PUBLIC_RUNTIME_CLASSPATH" \
  Audit11711JvmRunner "$AUDIT_RESULTS" <classes de test>
```

Le runner `scripts/audit11711/Audit11711JvmRunner.java` émet les vrais résultats par cas dans `junit.xml` et `cases.csv`. Les logs RED historiques sont conservés comme démonstrations, pas comme résultats du code final. Les tests de l'ancien crédit automatique ont été révisés parce que le mandat du 06/11 impose désormais le protocole complet ; les nouveaux tests comportementaux contrôlent effectivement l'absence de mutation.

- Oracle indépendant, seed **416886626065** : 10 000 traces, fenêtres de 1–8 semaines, 425 179 actions, sauvegardes/restaurations typées et redémarrages d'objets, états incomplets, preuve non persistée, rejeux. Cet oracle couvre le Renforcement Sabqi ; ce n'est pas un oracle complet de tout Hifz ou des pannes disque Android.
- Concurrence réelle : 500 paires de threads, soit 1 000 tentatives de clôture ; exactement un gagnant par paire, zéro double crédit. Ces tentatives ne simulent pas 1 000 pannes de commit.
- Réexécution des suites existantes : 512 calendriers d'absence/rattrapage, 128 variantes paramétrées de finales et 16 familles/reprises OPEN à 1–8 semaines, plus parcours de 180, 365 et 548 jours. Les nombres de scénarios ne s'ajoutent pas au nombre de cas JUnit.

## Cas du vendredi 06/11

Deux types de fixtures sont explicitement distingués :

1. Reconstitution exacte des champs décrits dans le mandat : mercredi appris et renforcé, reçu physique du mercredi et empreinte correspondante, aucun reçu vendredi. Le test `T01toT05ExactFridayLessonThenRealEveningAndNewSameDayBlock` vérifie l'absence de crédit avant Sabqi, l'état dû après vraie leçon, les répétitions complètes persistées, l'unicité du reçu puis un nouveau lot du même vendredi. Il s'agit d'une fixture synthétique basée sur les faits fournis, **pas** du replay de l'export personnel original.
2. États adversariaux avec anciennes unités, marqueur seul ou preuve absente : les tests RED montrent les contournements de l'API avant correction. Ils ne prouvent pas que chacun était exploitable sur l'export réel déjà renforcé mercredi. L'ancien correctif d'affichage était déjà présent à la base ; la nouvelle partie concerne les écritures.

Aucune capture Android nouvelle du cas réel n'a été prise dans cette exécution.

## Matrice T01–T30

PASS signifie ici un scénario métier JVM effectivement exécuté, dans le périmètre précisé. NON EXÉCUTÉ signifie que la ligne complète du mandat n'est pas couverte ; une preuve partielle n'est pas promue en PASS. Aucun PASS de cette table ne certifie Android physique.

| Cas | Statut | Preuve / limite |
|---|---|---|
| T01 | PASS JVM | ExactFriday + EveningMutationGate : aucune clôture du vendredi avant Sabqi |
| T02 | PASS JVM | Reçu/empreinte mercredi distincts du vendredi ; marqueur seul refusé |
| T03 | PASS JVM | Après leçon réellement enregistrée : dû, sans reçu du soir automatique |
| T04 | PASS JVM | ExactFriday, persistance complète, rejeu sans mutation |
| T05 | PASS JVM | Nouveau bloc le même vendredi et nouvelle empreinte due |
| T06 | PASS JVM | SnowballValidationIdempotence : lundi/mercredi ; ExactFriday : vendredi ; preuves datées |
| T07 | FAIL | P0 du payload de clôture legacy Itqān, voir test RED |
| T08 | NON EXÉCUTÉ | Refus et calendriers testés séparément ; matrice intégrale des chemins non reçue |
| T09 | PASS JVM | Curseur Sabqi décalé refusé + calendriers d'absences et changement de semaine |
| T10 | PASS JVM | CarryoverEightWeekStressTest : 512 calendriers, semaines 1–8 |
| T11 | NON EXÉCUTÉ | Reprises et n-1 testés sur JVM ; crash réel à chaque frontière non reçu |
| T12 | NON EXÉCUTÉ | Ancienne OPEN mardi/jeudi testée ; parcours complet mercredi/vendredi non reçu |
| T13 | PASS JVM | 16 reprises longues avec franchissement de mois/année et dates gelées |
| T14 | NON EXÉCUTÉ | 1 000 clôtures concurrentes + token Sabqi testés ; double tap BOOX et crash non reçus |
| T15 | NON EXÉCUTÉ | Ancien test d'échec de commit exécuté ; toutes les écritures et sémantique mémoire/disque Android non injectées |
| T16 | NON EXÉCUTÉ | Intents, notifications et navigation Android du nouveau code non exécutés |
| T17 | NON EXÉCUTÉ | Politique d'affichage JVM testée ; réception complète des écrans manquante |
| T18 | NON EXÉCUTÉ | Corruption et marqueurs seuls couverts partiellement ; toute la matrice future/import non exécutée |
| T19 | NON EXÉCUTÉ | Codec typé et Sabqi à date inconnue testés ; mise à jour réelle/clone complet non reçus |
| T20 | NON EXÉCUTÉ | Politiques/calendriers/parcours longs réexécutés ; mutations sur chaque configuration 0–6 non exhaustives |
| T21 | PASS JVM | PartialFinalEightWeekStressTest : 128 variantes, dont absence/zéro admissible |
| T22 | FAIL / partiel | Récupération At-Tūr et ×7 passent ; payload legacy Itqān non fiable |
| T23 | NON EXÉCUTÉ | Tests existants post-Nās passés ; nouveaux parcours complets Android non reçus |
| T24 | NON EXÉCUTÉ | Dates gelées et parcours longs couverts partiellement ; minuit/DST/horloge sur Android non reçus |
| T25 | FAIL / partiel | Fragments sans crédit et Sabqi canonique testés ; payload legacy Itqān permet encore des lignes étrangères |
| T26 | PASS JVM | SundayPromotionActiveCorpusIntegrationTest et séparation corpus/promotions |
| T27 | NON EXÉCUTÉ | Round-trips/rejeux présents ; série exacte 2/10/100 restaurations non exécutée |
| T28 | NON EXÉCUTÉ | Compteurs incomplets et stages du soir testés ; matrice complète page non vue/masquage non reçue |
| T29 | PASS JVM | Historique durable et parcours >120 jours des suites réexécutées |
| T30 | PASS JVM | Finales selon matériel réellement admissible et variantes à zéro |

## Portes restantes

1. Corriger le P0 legacy Itqān, avec preuve RED/GREEN et contrôle des séances authentiques existantes.
2. Refaire la contre-revue des nouveaux chemins Sabqi/Itqān, de la concurrence, des écritures et de l'import. Vérifier notamment le comportement réel de SharedPreferences lorsqu'un commit retourne false : le double de test atomique ne prouve pas à lui seul l'état mémoire Android.
3. Adapter les anciennes fixtures Android qui créaient directement des compteurs Sabqi par l'API scalaire ; elles doivent soit déclarer explicitement un état legacy, soit enregistrer le vrai protocole avec token. Cela ne doit pas réintroduire de crédit artificiel.
4. Exécuter toutes les lignes NON EXÉCUTÉES, compiler sous Gradle/SDK et instrumenter les deux profils exacts téléphone 1080×1920/dpi420 et BOOX émulé 1404×1872/dpi226, inspecter leurs vrais PNG QCF. Aucune ancienne CI n'est une preuve du nouveau code.
5. Après GO source seulement : transférer des correctifs validés vers la candidate par filiation traçable. Puis porte privée séparée : ressources privées, signature historique du nouvel APK exact, vérification APK v2, sauvegardes et installation BOOX physique sans désinstallation. **Aucune de ces étapes privées n'est déclarée faite ici.**
