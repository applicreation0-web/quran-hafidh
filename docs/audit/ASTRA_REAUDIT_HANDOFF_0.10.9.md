# Quran Safeguard 0.10.9 — handoff de re-audit Astra

## Identité exacte

- Branche : `release/0.10.9-work`
- `candidate_code_sha` : `df0be3ce9521e0bf0ecb6a62bbb4e393f1261a4b`
- `validated_handoff_sha` : `568564b7dd0b091330067ba35e2a1f6e6c594987`
- Parent audité NO-GO : `9846de48794e34eefc66d947e7f18991ad1c3342`
- Pull request de re-audit : [#78](https://github.com/applicreation0-web/quran-unlock-android/pull/78)

Le commit qui contient ce document est, par construction, postérieur au SHA validé ci-dessus et ne modifie que ce fichier Markdown. Le HEAD courant exact de la branche est celui affiché par la PR #78 ; le SHA applicatif reste `df0be3ce9521e0bf0ecb6a62bbb4e393f1261a4b`.

## Diff depuis le candidat applicatif

Le compare GitHub `df0be3ce9521e0bf0ecb6a62bbb4e393f1261a4b...568564b7dd0b091330067ba35e2a1f6e6c594987` ne contient aucun fichier produit net. Il contient uniquement des tests, gates, outils de reconstruction fail-closed, workflows et ce handoff.

| Commit | Classification | Contenu |
|---|---|---|
| `e089191d` | Documentation | Création du handoff Astra |
| `4114f6a6` | Gate + documentation | Gate Tafsir adaptée à l’architecture 10.9 |
| `5ad7951b` | Gate + documentation | Gate d’identité 0.10.3 modernisée |
| `845d4b65` | Tests/gates | Diagnostics et contrats 0.10.7 ; parseur Qurtubi fail-closed |
| `9463c0b2` | Outil de gate | Marqueurs de versets Qurtubi avec ellipse |
| `53b9d1c3` | Produit, annulé | Réintroduction transitoire de couleurs 0.10.7 |
| `4340ae86` | Outil de gate | Wrapper de reconstruction Tafsir 0.10.6 |
| `90fef2cd` | CI | Reconstruction Tafsir source-authoritative |
| `1c272b20` | CI | Diagnostics visuel/Tafsir séparés |
| `4bf52d4c` | CI | Conservation des diagnostics 0.10.7 |
| `0382feab` | Gate | Correction de l’assertion de newline |
| `d555d5b9` | CI | Vérificateur APK Plus 0.10.6 à deux fragments Qushayri |
| `21572165` | Produit, restauration exacte | Annule intégralement `53b9d1c3` et restaure le fichier depuis le candidat |
| `568564b7` | Gate | Palette 10.9 crème/noir exigée par le contrat modernisé |

La paire `53b9d1c3` / `21572165` s’annule intégralement. Le diff produit net depuis `candidate_code_sha` est donc nul : aucun fichier sous `app/` ne diffère au SHA de validation.

## Corrections Astra incluses dans le candidat

1. Nettoyage E-Ink différé garanti après `Afficher brièvement`, y compris lorsque le délai minimal bloque le refresh immédiat ou qu’un refresh survient pendant la révélation.
2. Fallback E-Ink générique visible au-dessus de la WebView opaque, avec fallback natif, sans modifier le SVG et avec STANDARD strictement no-op.
3. Toute aide active invalide la série locale ; `2 réussites → Premier mot → tentative correcte` ne valide jamais ; `Refaire` efface l’aide ; `Afficher brièvement` invalide la tentative.
4. Reprise exacte Lecture → Mémorisation → progression → kill/recreate avant affichage, sans flash initial non masqué.
5. Preuves APK, manifestes, permissions, migration, persistance, Safeguard OFF/ON et isolation Light/Plus produites par CI et runtime instrumenté.

## CI du SHA validé

Toutes les exécutions applicables au SHA `568564b7dd0b091330067ba35e2a1f6e6c594987` sont terminées avec succès :

| Workflow | Run | Résultat |
|---|---:|---|
| Quran Safeguard 0.10.9 build and audit | [34335381151](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34335381151) | PASS build + runtime |
| Android CI | [34335385394](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34335385394) | PASS |
| Android 0.10.5 Tafsir contradictory audit | [34335385387](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34335385387) | PASS |
| Android 0.10.5 sensitive-app scope audit | [34335385433](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34335385433) | PASS |
| Android 0.10.5 canonical Juz/Hizb audit | [34335385450](https://github.com/applicreation0-web/quran-unlock-android/actions/runs/34335385450) | PASS |

Le workflow 0.10.9 a exécuté les gates 10.8 modernisées, les vérificateurs 10.9, le protocole Mémorisation (786 transitions), les unitaires Light/Plus, l’isolation des éditions et les compilations debug/release Light/Plus.

## Runtime instrumenté réel

Artifact : `quran-safeguard-0.10.9-runtime-evidence`, ID `10097882321`.

- Light : 5/5 tests PASS, 0 échec, 0 erreur, 0 ignoré, 13.211 s.
- Plus : 5/5 tests PASS, 0 échec, 0 erreur, 0 ignoré, 9.947 s.
- Scénarios : restauration Mémorisation sans frame non masquée, fallback E-Ink sur WebView opaque, STANDARD no-op, Safeguard OFF→ON→OFF et migration versionCode 27→28.
- Les recherches statiques ne sont pas comptées comme tests Android runtime.

## APK candidats exacts

Artifact : `quran-safeguard-0.10.9-astra-re-audit`, ID `10097860965`, produit par le run `34335381151` au SHA `568564b7dd0b091330067ba35e2a1f6e6c594987`.

| Édition | APK | Application ID | Version | SHA-256 |
|---|---|---|---|---|
| Light | `app-light-release-unsigned.apk` | `com.applicreation0.quransafeguard` | `28 / 0.10.9` | `0c3a7a9af6ebc65a844ec55f89daed80e9c8b588fa0bf5db3f228aa03ebe691d` |
| Plus | `app-plus-release-unsigned.apk` | `com.applicreation0.quransafeguard.plus` | `28 / 0.10.9-plus.1` | `db3b97bb41b76484e812a38aeca700bda85e7b15db2903c1eea0fdfd1f205173` |

Les hashes ont été vérifiés à la fois par `candidate-evidence/SHA256SUMS` dans l’artefact CI et par recalcul indépendant après extraction.

Manifestes/permissions extraits :

- Light et Plus : `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` et permission dynamique interne non exportée propre à l’applicationId.
- Aucun élargissement de permissions post-candidat.
- Light : 604 pages, 0 asset Tafsir.
- Plus : 604 pages, 10 assets correspondant aux trois corpus Tafsir approuvés.
- Gate audio : `redistributionApproved=false` dans les deux éditions.

## Invariants préservés

- Mushaf exact de 604 pages, sans recomposition.
- Protocole 10/5/5/5/7 et aucun Tafsir en Mémorisation.
- Safeguard 60 secondes, budgets et exclusions banque/sécurité/identité.
- STANDARD no-op ; un seul APK Plus contient STANDARD et EINK.
- Aucune requête audio réseau lorsque la gate est fermée.
- Aucun merge, signature, publication ou audit Astra lancé par Sol.

## Limites matérielles

Le chemin logiciel générique et les invariants pédagogiques sont couverts sur émulateur Android. L’efficacité physique du full refresh, le ghosting résiduel et les API propriétaires facultatives restent à vérifier sur dalles E-Ink/BOOX réelles 7,8 et 10,3 pouces. Ces points ne sont pas déclarés PASS.
