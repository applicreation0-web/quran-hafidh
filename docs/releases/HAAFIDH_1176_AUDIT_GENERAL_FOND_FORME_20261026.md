# Quran Haafidh 1.17.6 — Audit général de fond, forme et compatibilité

**Statut : NO-GO tant que CI complète + émulateur Android + signature officielle + vérification de l'update-in-place ne sont pas verts.** Réf. 1.17.5 signée : versionCode 43 ; candidate 1.17.6 versionCode 44.

## Périmètre réellement contrôlé

- Branche indépendante : `work/haafidh-1176-full-audit-20261026` depuis le checkpoint 1.17.5 officiel `611144547a3b186042faabebfcee9b6441b343fb`.
- Inventaire des ~107 classes Java de production, 72 références `R.drawable`, 87 ressources `drawable`, 23 icônes figées.
- Tests sources Hifz/Atlas/masquage, révision active/passive, QCF 604 pages, reprises depuis dimanche, transitions V6.
- Séance nouvelle : finale ordinaire clôturée **avant** lecture cumulative, au plus 20 pages anciennement Acquises + pages validées de la semaine, 1 seule récitation masquée à 100 % avec amorces vérifiées, aucune attribution de crédit.
- Stress : cas 0, 1, 19, 20, 21 et 50 pages ; fusion de pages partielles, ordre, doublons ; scénarios 180/365/548 jours, 80 par durée ; test réel Android d'ouverture, interruption, relance de la lecture post-finale.
- Export/restore : la nouvelle lecture utilise exclusivement la preference `quran_hifz_preview_v1`, déjà intégralement incluse dans `HifzBackup` v3 ; pas de modification du schéma de progression ni d'initialisation.

## Corrections appliquées

1. Supprimé l'ancien terme **« boule de neige »** des surfaces de planning, « Aujourd'hui » et historique affiché. Noms techniques `snowball` et états sauvegardés volontairement conservés pour compatibilité.
2. Titres de finales homogénéisés : **Finale Sabqi**, **Finale Itqān** ; conversion à l'affichage des anciennes chaînes sauvegardées, sans réécriture de données.
3. Actions de lecture cumulative reliées aux vecteurs figés `ic_ui_revision` et `ic_ui_validate`, sans toucher aux 23 fichiers contractuels.
4. Bug détecté/réparé : une page QCF contenant des lignes anciennement Acquises ET des lignes validées cette semaine conserve **toutes** les lignes admissibles et n'est pas réintroduite une seconde fois parmi les anciennes pages.
5. Lecture cumulative séparée des moteurs : snapshot de pages préparé avant promotion Acquis, reprise de `PREPARED` après décès du processus suivant le commit, cursor page, statut `DONE` idempotent, no double tap / double credit.
6. Quatre tests obsolètes adaptés à la nomenclature approuvée et à la nouvelle version 1.17.6 (code 44) ; assertions de sécurité du dimanche non supprimées.

## Inspection de « code mort »

**Aucune suppression destructrice sans preuve de non-référence et absence de rôle historique.**

- `AtlasSourcePdfActivity` : son nom paraît non-référencé dans les autres classes Java, mais l'activité est déclarée explicitement dans `AndroidManifest.xml`; il serait dangereux de l'effacer.
- `HifzRecoveryApplication` : référencée par l'attribut `android:name` du manifeste ; indispensable à la réparation atomique des imports interrompus.
- `FreeMemActivity` : voie historique désactivée du menu principal mais activité/données d'ancien format encore présentes ; préserver la lecture des sauvegardes `free_mem` et les migrations.
- `ENTRETIEN`, `snowball`, `sourceSnapshot`, `FinalV1` : identifiants de compatibilité **non morts**, ne jamais les renommer dans les sauvegardes ni les états OPEN.
- Les ressources contractuelles et anciens tests ne sont jamais « morts » du seul fait d'être absents d'un menu.

## Invariants non négociables

- Reprise historique de la finale Sabqi du 25/10 et de la finale Itqān OPEN ; aucune nouvelle finale Itqān après An-Nās.
- Aucune conversion automatique d'une ligne Apprise vers Acquis sans preuve de Renforcement du soir, quota de finale, pages exactes et validation normale.
- Aucun crédit Acquis supplémentaire dans la lecture post-finale, aucune modification du cycle `ConsolidationCycleEngine` ou de ses snapshots historiques.
- Pas d'apprentissage forcé, de rattrapage inventé, de nouvelle initialisation ni d'horodatage fictif.
- Préserver les données lors de l'installation par-dessus l'APK 1.17.4/1.17.5, même package et certificat SHA256 officiel.
- BOOX : pictogrammes grands, titres au-dessus, sous-titres distincts et lisibles ; masque préparé avant premier paint e-ink ; sourates/pages/Amorces QCF réelles.
- Après An-Nās : maintien 1 hizb ×3, Consolidation des Maintiens réels de la semaine ×1 masquée ; la nouvelle lecture post-finale ne crée pas de septième séance ordinaire.

## Critères de GO pour publier une 1.17.6

1. Contrôle des fichiers moteur figés = PASS.
2. Tests JVM/sources **sans échec**, y compris suites de stress et tests récents.
3. Instrumentation Android réels + restauration/process death = PASS.
4. Captures Accueil/Parcours issues de **cet exact HEAD**, pas anciennes captures.
5. APK release avec 7/7 Atlas privés signés hors dépôt, chaque empreinte SHA256 égale à la version officielle.
6. APK Android v2 signée par le certificat historique `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`, versionCode 44, package `com.quransafeguard.hifz`.
7. Preuve d'installation par-dessus 1.17.5 sans suppression de données et restitution des finales OPEN.

Tout échec laisse le statut **NO-GO**. Aucune APK debug/incomplète ne doit être distribuée comme officielle.
