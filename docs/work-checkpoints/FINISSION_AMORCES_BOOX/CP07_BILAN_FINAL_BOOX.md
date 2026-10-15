# CP-07 — Validation BOOX et clôture

Identifiant : `CP-07`

Date : 2026-10-15

État : **PASS côté développement — NON EXÉCUTÉ sur BOOX physique**

## GitHub réel

- HEAD avant CP-07 : `c022cac71d448394a8df68de0063c11e6583a51f`.
- HEAD fonctionnel après CP-07 : identique, aucun code applicatif modifié.
- Source de l'APK : `6db0e342684344634892d7da4b3c695a3138a232`.
- Arbre applicatif gelé :
  `92cb64b26c43d61db4e4605139fb6a725781fec6`.
- CP-07 ajoute seulement la présente clôture et met à jour les documents de
  reprise ; aucune CI n'est déclenchée.

## Fichiers modifiés

- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP07_BILAN_FINAL_BOOX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

## Travail terminé

- Consolidation des preuves CP-00 à CP-06 sans rejouer M7-B, M8 ni la matrice
  Android 98/98 déjà validée.
- Séparation explicite des validations par code/tests, émulateur Android, APK
  privé et appareil BOOX physique.
- Checklist d'installation et de recette physique fournie sans annoncer de GO
  matériel fictif.
- Références finales du commit, des CI, des captures, de l'APK, du certificat et
  des rapports consignées pour une reprise immédiate.

## Bilan par niveau de preuve

### Validé par code et tests

| Domaine | Preuve | État |
|---|---|---|
| Titres GitHub chiffrés et reconstruction | CP-01, AES-256-GCM, 1 903/1 708/195 | PASS |
| Promotions Sabqi/Itqān du dimanche | CP-02, corpus actif réel et redémarrage | PASS |
| Carte sémantique graphique | CP-03, modèle/parcours/QCF/retour | PASS |
| 23 pictogrammes et contrat visuel | CP-04 | PASS |
| Mémorisation libre indépendante | CP-04, préférences Hifz inchangées | PASS |
| Non-régression ciblée | CP-05, arbre applicatif gelé | PASS |
| Moteurs Hifz, M7-B, M8 et QUL non modifiés | gardes et diffs ciblés | PASS |

### Validé par émulateur Android

| Parcours | Preuve | État |
|---|---|---|
| Accueil et programme sept jours | CI Android `38690267503` | PASS |
| Sabqi, Itqān, Renforcement, Consolidation | 98 tests Android | PASS |
| Révisions active/passive et promotions | CP-02 + Android CP-04 | PASS |
| Carte sourate → bloc → miniature → Lecture → retour | CI `38689001924` | PASS |
| Bloc court, multipage et Al-Baqara | cinq captures CP-03 | PASS |
| Mémorisation libre, masque 75 %, compteur, reprise | CI `38690267503` | PASS |
| Fermeture forcée et reprise | deux reprises Android externes | PASS |
| Portrait de référence et absence de troncature testée | captures CP-03/04 | PASS |

### Vérifié sur l'APK privé

| Contrôle | État |
|---|---|
| APK réel, ZIP valide et aligné | PASS |
| Signature APK v2/v3 et certificat M9 attendu | PASS |
| Package privé et version corrects | PASS |
| 1 708 titres exacts dans huit chunks | PASS |
| 195 groupes sans titre | PASS |
| Titre unique stable sur dix lectures de trois cas | PASS |
| Aucun ajout QUL ou mot-à-mot | PASS |
| Aucune clé ou secret embarqué | PASS |
| Aucune permission réseau déclarée | PASS |
| Installation, lancement et reprise de ce fichier sur BOOX | NON EXÉCUTÉ |

### Restant à tester sur BOOX physique

Tous les éléments de cette section sont **NON EXÉCUTÉ**, car aucun BOOX n'est
connecté à l'environnement de travail :

- installation/mise à jour sans désinstallation dangereuse ;
- coexistence avec l'application officielle ;
- lecture du Muṣḥaf et taille réelle en portrait ;
- affichage des titres, dix appuis sur le même bloc et cas sans titre ;
- carte sémantique, miniatures, surlignage, Lecture et retour ;
- masques, focus, contrastes, icônes et textes sur dalle E-Ink ;
- rafraîchissement E-Ink et rémanence ;
- Mémorisation libre et maintien de son indépendance ;
- validation d'une séance sans perte ni double crédit ;
- fermeture forcée, reprise et sauvegarde/restauration sur l'appareil.

## Checklist physique à suivre

1. Vérifier le SHA-256 de l'APK avant copie.
2. Sauvegarder toute progression debug existante ; ne pas désinstaller à
   l'aveugle.
3. Installer par canal privé et confirmer que l'application officielle reste
   intacte.
4. Parcourir Lecture et la carte sémantique en portrait.
5. Contrôler un bloc court, un bloc multipage et une grande sourate.
6. Appuyer dix fois sur le même titre et vérifier qu'il ne tourne pas ; ouvrir
   aussi un groupe sans titre.
7. Contrôler les cinq masques, le focus, les surlignages, les zones tactiles et
   la lisibilité E-Ink.
8. Reprendre une Mémorisation libre après fermeture et vérifier qu'elle n'a pas
   crédité la progression ni les révisions.
9. Finaliser une séance autorisée, fermer de force, rouvrir et vérifier
   l'absence de perte, double crédit ou séance réapparue.
10. Exporter une sauvegarde avant toute opération corrective.

## Tests exécutés à ce jalon

- Aucun test logiciel rejoué : **NON EXÉCUTÉ**, car aucun code concerné n'a
  changé depuis les preuves PASS CP-02 à CP-06.
- Relecture des statuts, empreintes et références récupérables : **PASS**.
- Tests BOOX physiques : **NON EXÉCUTÉ**.

## Problèmes et limites restants

- Aucun défaut logiciel connu dans le périmètre de la mission.
- Aucun GO BOOX matériel n'est donné avant exécution de la checklist physique.
- Les quatre pictogrammes historiques restent intègres mais leur silhouette ne
  peut pas être certifiée contre une référence graphique indépendante absente.
- Le test tactile dix appuis et le rendu réel des titres privés n'ont pas été
  exécutés sur un écran physique ; la stabilité des données et du chargeur est
  néanmoins vérifiée dans l'APK.

## SHA-256 des livrables concernés

- APK privé signé :
  `117c7e8a432803d3f1f75c97230bc3e116da0a72336f99563a1d815a7a759a09`.
- Archive privée complète locale :
  `0ea6a9b03007101b86c19832efda84324e3624a40d30b68d78c3dea779d2d311`.
- Archive privée des rapports :
  `446b554ce93b5aeea651a54a22e8f3ee14c4c8c85fc4fbfb63f188ffe61d4ce6`.
- Certificat M9 :
  `b75ba0a3dbe7c8f00c80799460d890864142b585f669dad5db468b8e2edb7aa4`.
- Preuves Android CP-03 :
  `c9170cf1e374d929728ddaf9b44a61f4b93daef7febef88a695b870b46788e90`.
- Preuves Android CP-04 :
  `ca42d4a32b95e8b346acb5c0cf54888047a5e92f30962ed07fa8461dadf1ea8f`.

## Références récupérables

- APK privé : `libfile_56aaac625d148191973705fc2f8a2918`.
- Rapports APK : `libfile_d395df20f9a88191aa157f32d7b0b6d6`.
- Captures/rapports carte CP-03 :
  `libfile_90ebbaa18b0c81919fe690f11cf04e21`.
- Captures/rapports visuels CP-04 :
  `libfile_5a9485b8e3908191be0dcf0e18d932ae`.
- CI source/Android : `38689001952`, `38689001924`, `38690267562`,
  `38690267503` et build APK `38695806767`.
- Checkpoints GitHub : `STATUS_CURRENT.md`, `INDEX.md` et
  `HANDOVER_REPRISE.md` dans ce dossier.

## Reprise exacte

Première action : sur le BOOX physique, ouvrir la notice CP-06, vérifier
l'empreinte `117c7e8a…`, sauvegarder l'éventuelle installation debug existante,
puis exécuter la checklist ci-dessus. Ne modifier le code qu'en présence d'un
défaut matériel reproductible et documenté.
