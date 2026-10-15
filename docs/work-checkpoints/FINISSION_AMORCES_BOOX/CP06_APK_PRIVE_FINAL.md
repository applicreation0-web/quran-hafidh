# CP-06 — APK debug personnel BOOX

Identifiant : `CP-06`

Date : 2026-10-15

État : **PASS**

## GitHub réel et source gelée

- HEAD avant CP-06 : `6db0e342684344634892d7da4b3c695a3138a232`.
- HEAD après correction du seul emballage CI :
  `6b3dbd4278124d87029b3b27bf09a010979a39c4`.
- Source compilée explicitement par le workflow :
  `6db0e342684344634892d7da4b3c695a3138a232`.
- Arbre applicatif compilé :
  `92cb64b26c43d61db4e4605139fb6a725781fec6`.
- Identité Android : `com.quransafeguard.hifz.ibnkathirtest`, versionCode 39,
  versionName `1.17.1-IbnKathir-DEBUG`.
- Aucun fichier applicatif ni moteur Hifz n'a changé pendant CP-06.

## Fichiers modifiés dans Git

- `.github/workflows/haafidh-ibn-kathir-debug.yml`
- `.github/workflows/ibn-kathir-engine-frozen.yml`
- `.github/workflows/diagnose-quiz-launch-android.yml`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP06_APK_PRIVE_FINAL.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

Les livrables privés ci-dessous sont hors Git.

## Travail terminé

- Construction publique d'un seul APK intermédiaire sans titres privés, depuis
  le HEAD CP-05 exact, avec JDK 17, Gradle 9.5.0, AGP 9.3.0, compileSdk 37 et
  build-tools 36.0.0.
- Reconstruction locale authentifiée AES-256-GCM du paquet CP-01 : huit chunks,
  1 903 identifiants, 1 708 titres exacts et 195 absences.
- Comparaison SHA exacte des huit chunks reconstruits avec le paquet M9
  approuvé, puis injection strictement locale.
- Alignement ZIP, signature par la JKS M9 existante et vérification des schémas
  APK Signature v2 et v3.
- Inventaire de l'APK : delta limité aux huit chunks privés et aux métadonnées
  de signature ; aucune entrée source supprimée, aucun ajout QUL ou mot-à-mot,
  aucune clé AEAD/JKS ni conteneur chiffré embarqué.
- Sauvegarde privée récupérable de l'APK réel et d'une archive de rapports.

## Tests exécutés

| Contrôle | Résultat |
|---|---|
| Workflow public final, run `38695806767` | PASS |
| Commit source CP-05 et arbre applicatif exacts | PASS |
| Garde gel Hifz, audit Quiz exclu, 23 icônes | PASS |
| Contrats source CP-06 | PASS |
| Assemblage d'un seul debug | PASS |
| Intermédiaire public : zéro titre privé | PASS |
| Archive GitHub, intégrité ZIP et manifeste | PASS |
| Reconstruction AEAD 1 903/1 708/195/8 | PASS |
| Huit chunks identiques au paquet approuvé | PASS |
| `zipalign -c -p -v 4` sur l'APK signé | PASS |
| `apksigner verify --verbose --print-certs` | PASS |
| Certificat SHA-256 attendu | PASS |
| Identité/version Android privée | PASS |
| Inventaire embarqué 1 708 titres, 195 absences | PASS |
| Trois titres représentatifs lus dix fois, chaîne unique stable | PASS |
| Absence de permission réseau déclarée | PASS |
| Aucun ajout QUL ou mot-à-mot | PASS |
| Ouverture/reprise de l'arbre applicatif identique | PASS, preuve Android CP-04 |
| Installation et dix appuis tactiles sur APK final physique | NON EXÉCUTÉ, BOOX absent |

Le test des dix lectures porte sur les données réellement embarquées et le
chargeur à chaîne unique ; le geste tactile matériel est volontairement laissé
à CP-07 et n'est pas présenté comme exécuté.

## Incident corrigé

Le premier artefact public PASS (`38694876701`) exportait `zipalign` sans sa
bibliothèque dynamique `libc++.so`. L'échec local était reproductible avant
toute signature. Le workflow a seulement ajouté cette dépendance et son
empreinte au manifeste ; le second run `38695806767` est PASS. Aucun contenu
privé n'a transité par GitHub.

## Problèmes restants et raisons

- Aucun blocage logiciel ou de paquetage connu.
- Les tests tactiles, le rafraîchissement E-Ink, l'installation/mise à jour et
  la reprise sur BOOX physique restent **NON EXÉCUTÉ** faute d'appareil.
- L'APK complet ne doit jamais être publié sur GitHub ; il reste dans l'espace
  privé récupérable.

## SHA-256 des livrables

- APK privé signé :
  `117c7e8a432803d3f1f75c97230bc3e116da0a72336f99563a1d815a7a759a09`.
- Archive privée complète locale :
  `0ea6a9b03007101b86c19832efda84324e3624a40d30b68d78c3dea779d2d311`.
- Archive privée des rapports :
  `446b554ce93b5aeea651a54a22e8f3ee14c4c8c85fc4fbfb63f188ffe61d4ce6`.
- JKS M9 :
  `fc5506f17e4dc5335be49d715900198ed0c3780d76bff7ae650d1fc6ab7a56cd`.
- Certificat M9 :
  `b75ba0a3dbe7c8f00c80799460d890864142b585f669dad5db468b8e2edb7aa4`.

## Références récupérables

- Run GitHub final : `38695806767`, job `116291995203`.
- Artefact public temporaire sans titres : `11954083887`, SHA-256
  `b8e3150de21f8521f161b8b3e37e6d99940d7ca4c926975b829adb1b948bb1d`.
- APK privé : `libfile_56aaac625d148191973705fc2f8a2918`.
- Rapports privés : `libfile_d395df20f9a88191aa157f32d7b0b6d6`.
- Contenu des rapports : signature, identité Android, inventaire des titres,
  empreintes et notice d'installation BOOX.
- Clé AEAD et JKS : sauvegardes privées séparées, jamais ajoutées au dépôt.

## Reprise exacte

Première action : ouvrir `CP07_BILAN_FINAL_BOOX.md`, reporter les contrôles déjà
validés par code/Android/APK et marquer chaque essai exigeant le BOOX physique
`NON EXÉCUTÉ`, sans simuler de GO matériel.
