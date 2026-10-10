# QURAN HAAFIDH — SECOND AUDIT CONTRADICTOIRE J0–J7

Date : 10 octobre 2026 · Branche exclusive : `work/haafidh-1171-ibn-kathir-integration-independent`  
Référence officielle : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`

## Verdict

**Audit de source + tests automatisés : VALIDÉ.**  
**Audit physique BOOX : NON EXÉCUTÉ (nécessite appareil/émulateur).**  
**Publication autorisée pour une APK debug isolée, pas pour une release officielle.**

Exécution CI de référence après les corrections : [38089619794](https://github.com/applicreation0-web/quran-hafidh/actions/runs/38089619794), conclusion `success`. Contrôles : comparaison octet pour octet des moteurs gelés et du Quiz 1.17.1, test JVM `:hifz-core:test` et `:hifz-app:testDebugUnitTest`, tests source `:hifz-app:sourceContractTest`, 20 vérifications contradictoires, vérification des 23 pictogrammes, inventaire de tous les écrans, corpus Ibn Kathīr (1 903 groupes / 6 236 versets), alignement Hafs/QCF (77 432 mots / 604 pages). **Aucun test en échec sur la révision validée.**

## Défauts réellement identifiés par le second audit

| Priorité | Défaut adversarial | Correction effectuée | Preuve indépendante |
|---|---|---|---|
| P1 | Si une semaine historique est *partiellement* Acquis, `extendedSnowballUnits` reproposait aussi les blocs déjà entièrement Acquis, car son filtre portait seulement sur la semaine entière | Filtrer **chaque bloc physique** déjà entièrement acquis, tout en conservant les blocs de la même semaine qui restent à promouvoir | `ContradictoryAuditTwoPersistenceTest.partlyAcquiredSundayDoesNotRepeatEntirelyCreditedBlocks` sur les deux familles |
| P1 | Une session OPEN créée avant `sourceSnapshot` pouvait, au premier checkpoint après migration, recevoir l'empreinte du **nouveau cumul**, comprenant des unités jamais travaillées durant la séance initiale | Reconstitution conservatrice du **préfixe exact de blocs réellement gelés** à partir des identifiants de cycle et des unités persistées. La famille Stabilisation 7× et le Renforcement 10× ont chacun leur règle de reconstitution | `missingOldSnapshotRecoverOnlyOldLearningPrefix`, `missingOldSnapshotRecoverOnlyOldStabilizationPrefix` |
| P1 défensif | Si une ancienne séance ne correspond plus du tout au cumul courant (semaine déplacée/corpus réorganisé), aucune reconstitution exacte n'est possible | Empreinte `LEGACY_OPEN` isolée : une validation ancienne ne peut **jamais** considérer le nouveau cumul comme consommé, lequel reste dû. Une répétition supplémentaire peut être proposée plutôt qu'une perte de travail | `oldUnrecognizableSnapshotFailsOpenForPendingRatherThanConsumeNewCorpus` |

Les corrections sont strictement limitées à la projection des finales et à la récupération d'un état de session préexistant. Les autres méthodes de progression ne sont pas modifiées. Le script `scripts/verify_authorized_program_preferences_delta.py` compare les trois deltas exacts aux sources précédemment certifiées.

## Contre-épreuves transversales reprises

- **Horloge/persistance** : séance du mardi rouverte mercredi, changement de semaine, double clic Valider, journal de validation et snapshots stockés en commit atomique, bascule Classique/Recommandé sans remise à zéro.
- **Renforcement et Consolidation** : bloc nouveau Consolidation ×7, anciens blocs en continu ×1 ; Renforcement existant ×10 ; finales ×3 avec promotion uniquement après validation complète, sans reprise des blocs déjà Acquis.
- **Itqān** : 20 répétitions visibles + quatre séries masquées de 5, ancien régime figé maintenu ; supplément +1/+2 seulement à une frontière physique valide, sans fusion Qāf/Adh-Dhāriyāt.
- **Apprentissage** : chaîne Sabqi et progression V6 protégées ; nouveau matériel Appris admissible à la Révision *passive* si verset entièrement couvert, jamais au Quiz Acquis par effet indirect.
- **Révision active/passive** : corpus et curseurs indépendants ; dimanche sans séance active ni passive dans les deux programmes, finales maintenues ; horaires indicatifs 05h30/20h00 modifiables, sans verrou.
- **Quiz officiel 1.17.1** : source code gelée, éligibilité `learned ∪ stabilized ∪ acquired` recalculée à chaque lecture, audio de correction et format conservés.
- **Ibn Kathīr / carte** : règles mécaniques des premiers mots réels du groupe, mêmes repères pour Lecture et Révision active, géométrie de 604 pages, aucun titre éditorial inventé.
- **BOOX UI** : hashes des 23 pictogrammes contractuels et appels d'icônes audités sur tous les écrans. *Le rendu des pixels, la rémanence E-Ink, le toucher et l'ergonomie réelle ne sont pas certifiés par les tests statiques.*

## Conditions imposées à l'APK debug

1. Build uniquement sur la branche autorisée, après réexécution des tests et des scripts de gel.
2. `assembleDebug` uniquement ; aucun build release, tag ou GitHub Release.
3. Package indépendant `com.quransafeguard.hifz.ibnkathirtest`, version `1.17.1-IbnKathir-DEBUG` : ne remplace pas les données de l'application officielle.
4. APK non vide, `aapt` conforme, vérification cryptographique de signature APK, SHA-256 publié à côté de l'APK.
5. Seul artefact de téléchargement attendu : APK debug + sa somme SHA-256, archivé dans GitHub Actions.

**Limites de migration** : la signature debug est celle du système de compilation Android ; si un futur APK debug utilise une autre clé, la réinstallation peut exiger la désinstallation du paquet de test, avec perte de ses données locales. Conserver une sauvegarde avant un tel remplacement. Ce risque ne concerne pas le package officiel isolé.

**Limites d'exécution** : les tests automatisés passent, mais la certification d'un appareil BOOX physique (affichage portrait, boutons, masques, performance, retour après mise en veille) exige encore l'installation de ce debug.
