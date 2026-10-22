# Quran Haafidh 1.17.2 — Audit ciblé Itqān post-An-Nās ×3
Date : 2026-10-22. Branche : `work/haafidh-1171-atlas-only-pre20-mask`.
**Statut de publication : PAUSE, AUCUNE RELEASE PUBLIQUE.**

## Nouvelle règle arbitré par l'utilisateur
- À partir de la PREMIÈRE arrivée à An-Nās **en Itqān**, une séance = **un hizb**, sur ses limites exactes réelles et tronquée à la frontière Sabqi, sans saut ni anticipation.
- **×3** : 2 lectures visibles (0 % de masque), suivies de 1 récitation sous 100 % de masque avec amorces vérifiées (trous QCF exacts).
- Premier parcours d'Itqān : protocole FULL ×40 et sauvegarde inchangés.
- Une unité de maintenance **déjà commencée** sous ×4 conserve ses répétitions sauvegardées et se termine en ×4 : le nouveau profil `POST_NAS_THREE` n'est attribué qu'aux unités nouvellement ouvertes.

## Surface technique délibérément minimale
- `ItqanMaintenancePolicy` : compte ×3, passage 1–2 visible, 3 masqué; vérification uniquement au passage masqué, et relecture ×4 ancienne si profil antérieur.
- `ItqanRegimeStore` : sérialisation de la variante dans le champ de profil déjà utilisé, sans migration destructive, sans changer les noms des clés ni la structure de la sauvegarde ; les anciens 4/5 champs continuent d'être lus.
- `ItqanBalancedMaskSchedule` : dispatch à partir du profil sauvegardé.
- `HifzSessionActivity` : cible de répétitions, masques, validation dérivés du profil de l'unité en cours.
- `WeeklyDashboardPlanner` : ×4 si séance existante déjà ouverte ; ×3 sur futures unités.
- `HifzPrefs` : empreinte binaire inchangée depuis `ecbe005a79e5d89e77b9e9ac6968e6916cc8bdb8`.

## Vérifications documentées
- Régression JVM / source contract : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39844315263
- Staging privé SANS corpus ni signature : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39844315253
- Audit Android instrumenté candidat : **PASS**, JVM + source contracts et **4/4 tests émulateur API 35**, https://github.com/applicreation0-web/quran-hafidh/actions/runs/39844315344. Ces quatre tests natifs portent sur WebView masquage premier rendu et persistance/sauvegarde, pas sur un appareil BOOX.
- Tests Itqān spécifiques : découpage d'un hizb, absence de franchissement Sabqi, alternance [0,0,100], validation sous masque, après An-Nās, force-stop et reprise des plans ×3, non-troncature d'une unité ×4 ouverte, absence de double crédit/Sabqi, préservation consolidation.
- Audit exact de code `scripts/verify_post_nas_three_pass_delta.py`, `verify_itqan_rotation_atomicity.py`, `verify_authorized_hifz_session_delta.py`.

## Artefact privé de vérification (NON PUBLIÉ)
- Candidat signé produit **hors GitHub** à partir du staging source `0b17c83ed3c12ed2ff0f857c00466a39f22f519a`.
- Identifiant Android : `com.quransafeguard.hifz.ayahthemes114private`; versionCode 40, 1.17.2; signature v2/v3 avec certificat historique BOOX `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e`.
- 7 ressources privées SHA-256 contrôlées, jamais publiées dans le dépôt.
- SHA-256 du candidat privé : `aa57ae46c0bc3b206ce95eb34861595e1d4a0488c946c2180e227cdc0a37d565`.
- Tests statiques APK PASS ; pas d'installation, pas de validation physique BOOX.

**Décision : AUDIT CI PASS, NO-GO release publique tant que la recette BOOX physique de l'application privée et l'accord utilisateur ne sont pas confirmés. Ne pas désinstaller la 1.17.1, ne pas supprimer les acquis.**
