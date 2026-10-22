# Quran Haafidh 1.17.3 — Rapport d'audit et préparation de release

Date : 2026-10-22. Branche source : `work/haafidh-1171-atlas-only-pre20-mask`.
HEAD compilé : `a6837f3fd3ed3c4ab22cb7288824b7771a59ce49`.
Mode : **Atlas + Hifz seulement**, pas de Correspondances.

## Modifications vérifiées
- Numéro de version **1.17.3**, `versionCode 41`, APK privé BOOX `1.17.3-AyahThemes114-PRIVATE`.
- Lecture libre anti-ghosting : premier cadre SVG masqué à 100 % avant révélation; vérification des ancrages QCF.
- Révision active et Entretien passif : même calcul de durée acquise (30 min sous 15 juz ; 45 min et paliers de 15 min ensuite), avec curseurs et corpus indépendants.
- Itqān post-première An-Nās : exactement un hizb par séance, **×3 = 2 lectures visibles puis 1 rappel à masque 100 % avec ancrages**; séances déjà ouvertes en ×4 non raccourcies; premier passage ×40 inchangé.
- Écrits de progression et schéma de sauvegarde `HifzPrefs` maintenus; aucun effacement ni re-crédit prévu.

## Résultats obtenus
| Contrôle | Résultat | Preuve |
| --- | --- | --- |
| Blocage des moteurs Hifz, des 23 icônes, des titres Atlas non approuvés | PASS | https://github.com/applicreation0-web/quran-hafidh/actions/runs/39847144224 |
| Régressions JVM Hifz/Atlas/QCF et contrats source | PASS | https://github.com/applicreation0-web/quran-hafidh/actions/runs/39847144224 |
| Compilation staging Android 1.17.3 code-only | PASS | https://github.com/applicreation0-web/quran-hafidh/actions/runs/39847144342 |
| Instrumentation Android 35, WebView, sauvegardes | **4/4 PASS** | https://github.com/applicreation0-web/quran-hafidh/actions/runs/39847144170 |
| APK signé localement, ZIP CRC, signature Android v2/v3 | PASS | contrôle statique hors GitHub |
| Certificat BOOX de référence historique | `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e` | vérifié sur binaire signé |
| Paquet privé Android | `com.quransafeguard.hifz.ayahthemes114private` | `aapt dump badging` |
| Versions de référence **1.17.1 → 1.17.3** et **1.17.2 → 1.17.3** | PASS de compatibilité statique (même paquet/signataire, code 39/40 < 41) | `aapt` + `apksigner` sur trois APK |
| Corpus privés approuvés (7), pages QCF 604 | PASS, contrôles SHA-256 | extraction et vérification ZIP hors GitHub |
| Ghosting écran BOOX / rapidité réelle | **À VALIDER SUR APPAREIL** | aucun accès à la liseuse |
| Installation par-dessus l'app réellement installée, acquis intacts | **À VALIDER SUR APPAREIL** | aucune écriture à l'appareil |
| Permission de diffuser publiquement 7 ressources privées | **NON ÉTABLIE** | GitHub du dépôt est public |

## Binaire privé à conserver hors GitHub
Nom local : `QURAN_HAAFIDH_BOOX_ATLAS_1173_OFFICIAL_RELEASE_PRIVATE_CANDIDATE.apk`.
Taille : 91 830 152 octets.
SHA-256 : `e7a87ee346d98e249edd3a3f5ac5cd5ca0f4db580beaf32a84f2bc18678b653a`.
Version : `41`, `1.17.3-AyahThemes114-PRIVATE`.
Signataire : SHA-256 ci-dessus.
Aucun corpus privé ni keystore ni mot de passe dans le dépôt public.

## Recette BOOX avant la release
1. Sauvegarder les acquis dans la 1.17.1/1.17.2 privée déjà installée. Ne pas désinstaller.
2. Installer le candidat 1.17.3 **par mise à jour directe**, pas en réinstallation.
3. Vérifier que Sabqi, Itqān, Révision active et Entretien ont les mêmes curseurs, acquis, anciennes validations, sessions à reprendre et durée active/passive correcte.
4. Lecture libre : activer le gommage avant de voir la page; tourner la page plusieurs fois; revenir et vérifier la disparition des empreintes/ghosting.
5. Itqān après An-Nās : 1 hizb, exactement visible-visible-masqué; un ancien ×4 déjà engagé doit rester ×4.
6. Confirmer le rendu 604 pages Atlas/QCF et l'absence de régression du mode lecture.
7. Confirmer que les données personnelles sont conservées après redémarrage de la liseuse.

**Décision d'audit automatisé : PASS. Décision de distribution publique : NO-GO tant que la validation physique BOOX et les droits de diffusion des corpus privés ne sont pas confirmés.** Une release publique sur ce dépôt rendrait les données de tout APK attaché librement téléchargeables. Une diffusion officielle strictement privée du binaire peut être décidée séparément une fois la recette faite.
