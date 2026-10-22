# Quran Haafidh — version officielle 1.17.3, édition BOOX privée

Version privée signée : 22 octobre 2026. GitHub Release publique **NON publiée (HTTP 403)**. Variante : **Atlas + Hifz**, hors Correspondances.
Dépôt public source : `applicreation0-web/quran-hafidh`, branche `work/haafidh-1171-atlas-only-pre20-mask`.

## Vérifications avant publication
- Moteurs gelés Hifz/Atlas/QCF : **PASS** — https://github.com/applicreation0-web/quran-hafidh/actions/runs/39851925354
- Suite Android renforcée : **24/24 PASS, 0 échec** — https://github.com/applicreation0-web/quran-hafidh/actions/runs/39851925203
- Staging QCF/Android : **PASS** — https://github.com/applicreation0-web/quran-hafidh/actions/runs/39850979986
- Source du binaire signé : `3db1f577d060f5cb543baf81323161d0c46a0cc0`; les seuls commits ultérieurs modifient la documentation, les tests instrumentés et leur configuration, sans changer le code applicatif.
- Comparaison signée avec le précédent candidat 1.17.3 : seules 4 entrées APK changées (code DEX, AndroidManifest de provenance, titre invisible JS, métadonnées de version-control); **699 autres entrées identiques**, dont les corpus privés et les 604 pages authentiques QCF.

## Corrections incluses
- Révision active : même durée évolutive que l'Entretien (30 min initiales, puis paliers acquis), correction aussi dans le menu.
- Apprentissage recommandé : frise `1/3` lundi, `2/3` mercredi et `3/3` vendredi ; planning descriptif sans crédit anticipé.
- Amarces/rappels Ayah Themes : suppression des cadres gris autour des zones QCF, trous exacts au masque à 100 % et tap du titre/verset conservés.
- Itqān après première arrivée à An-Nās : 1 hizb, **×3 (2 visibles, 1 masquée avec ancrages)**. Les séances ×4 ouvertes restent ×4; première stabilisation ×40 inchangée.
- Premier rendu masqué anti-ghosting : protections des lignes et de la géométrie exacte préservées, pas de flash de texte attendu ; à confirmer sur appareil BOOX réel.
- Conservation des curseurs distincts, acquis et sauvegardes V6 ; Atlas limité aux Unités et Lecture.

## Distribution du binaire BOOX
Le binaire privé signé est délivré séparément au propriétaire. **La création de la page GitHub Release est actuellement bloquée par l'autorisation du jeton GitHub Actions (HTTP 403).** Même après déblocage, l'APK intégrant les sept ressources privées ne doit pas être joint à une release publique sans validation des droits de diffusion.

| Propriété | Valeur |
| --- | --- |
| Nom | `QURAN_HAAFIDH_BOOX_ATLAS_1.17.3_RELEASE_FINALE_SIGNEE_20261022.apk` |
| SHA-256 APK | `bd9b53cf5d5ffd6ce6cfd809ec6dbccb90bec199d890c07d9df94a6427245d91` |
| Nom Android | `1.17.3-AyahThemes114-PRIVATE` |
| versionCode | 41 |
| Identifiant Android | `com.quransafeguard.hifz.ayahthemes114private` |
| Certificat SHA-256 | `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e` |
| Signature | APK v2 et v3 vérifiées |
| Mise à jour depuis versions privées | 1.17.1 code 39 ou 1.17.2 code 40 : **identifiant + certificat + numéros compatibles statiquement** |
| Ressources | 604 pages QCF et 7 ressources privées SHA-256 vérifiées |
| Tests physiques BOOX | **Non exécutés dans CI**, à confirmer sur l'appareil |
| Correspondances | Non incluse |

Le PDF et les corpus privés intégrés sont **exclus du dépôt public et des assets GitHub Release** afin de ne pas redistribuer sans vérification de leurs droits les sources annexes. La clé historique et son mot de passe restent hors GitHub.

**Instructions d'installation** : sauvegarder ses acquis, installer l'APK signé directement par-dessus la version BOOX privée déjà installée, sans désinstaller ni vider les données ; vérifier immédiatement les acquis, Sabqi, Itqān, Révision active/passive, masquage et absence de ghosting sur BOOX.

**Attention :** cette édition privée est une lignée Android distincte des anciennes releases publiques `quran-hifz-1.17.1-release.apk` : ne pas supposer que le binaire BOOX privé met à jour leur identifiant ou certificat.

## État de la publication GitHub
Le workflow a tenté de créer la release **source-only** `quran-haafidh-v1.17.3-boox-private-source` le 22 octobre 2026 ; GitHub a répondu `HTTP 403: Resource not accessible by integration`, après validation des gates CI. Aucune release GitHub 1.17.3 n'a donc été créée et aucun APK ni secret n'a été transmis au dépôt.
Preuve : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39854948639.
