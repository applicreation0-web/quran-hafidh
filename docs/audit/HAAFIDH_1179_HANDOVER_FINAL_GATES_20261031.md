# Quran Haafidh 1.17.9 — état d'intégration et portes finales
Date : 31 octobre 2026, après les corrections P0-P2 du présent chat. **AUCUNE RELEASE**, **AUCUN APK SIGNÉ**.

## Reprise sans duplication
- Dépôt : `applicreation0-web/quran-hafidh`.
- Base fonctionnelle (NE PAS ÉCRASER) : `work/haafidh-1179-p1-279-20261031`.
- Dernier correctif fonctionnel enregistré au moment de ce handover : `d08a97cbe307219628125431af701e6cbc40e795`. Lire le HEAD réel avant toute reprise.
- **Work a un correctif local postérieur à ses commits visibles le matin du 31/10** (SVG QCF : viewport, clipping, dimension glyphes, garde workflow). Il n'était **ni poussé sur GitHub ni accessible dans la Library** au moment des vérifications. Récupérer et comparer son `git diff`/checkpoint, conserver les modifications locales, intégrer uniquement les différences non déjà corrigées. Ne pas réinitialiser, ne pas recommencer un audit historique.
- Le catalogue n'a pas besoin d'un nouvel arbitrage avec l'utilisateur : les dix cas ont déjà été étudiés ; cas 62 explicitement admis, neuf restent hors catalogue faute de certification technique/editoriale de leurs données. Ne pas inventer de nouvelle admission.

## Catalogue certifié en CI
- 294 dossiers éditoriaux documentés : **279 admis, 9 hors catalogue, 6 exclus**. La paire **13:18 ↔ 39:47 (ID 62)** a été intégrée avec texte canonique et indices QCF `[20]` / `[13,14,15]` pages 251/463.
- **604 pages QCF** authentiques, **499 points Jalalayn originaux** vérifiés, index et intégrité des mots/coordonnées protégés.
- **8/8 shards Android complets PASS** (4 smartphone, 4 BOOX simulé) : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41210439759. Les shards sont sur la base catalogue initiale de la branche `audit/haafidh-1179-catalogue-279-android-20261031` SHA `94d1ab0210715539a1e3f70626a45d65ba23395e`. Les changements ultérieurs concernent surtout l'interface/timer et les tests, pas les entrées du catalogue.

## Correctifs P0 réalisés dans ce chat
- Tests de validation active : reconnaître le bouton réel `VALIDER QUAND MÊME`, préserver le replay après dévoilement; fixture de sauvegarde schéma 6 correctement initialisée en test isolé.
- Finale Itqān : fixture du lundi 26/10 initialisée avec la vraie cadence de référence ; ni HifzPrefs ni moteurs/initialisations de production modifiés.
- Révision passive : lecture et comparaison imbriquées doivent suspendre puis reprendre exactement le chronomètre. Le test attend toujours au moins 900ms de reprise, avec une fenêtre raisonnable de 5s contre les retards d'émulateur.
- Tafsīr des deux côtés : sélection sur les vrais `path.ayahPolygon` des SVG QCF, résolution du verset exact, chargement de l'ancien corpus Jalalayn en local, persistance après `recreate` et côté/tabs synchronisés.
- BOOX émulateur : récupération *uniquement* du dialogue système `Pixel Launcher isn't responding` en choisissant `Wait`; une erreur de l'application n'est jamais masquée.
- Interface paysage smartphone : diagnostic du défaut réel **ScrollView hauteur 0 / Tafsīr portrait résiduel** : le manifest gère lui-même `orientation|screenSize`, empêchant Android de recréer automatiquement l'activité. Correction `d08a97cb` : `onConfigurationChanged` détruit proprement les anciennes WebView QCF, `build()/render()` réinstalle la disposition orientée, tout en conservant le cas et les versets. Un layout latéral reçoit Tafsīr si assez large.
- Les deux pages restent des SVG source proportionnés (345:550) ; les tests de visibilité complète ne sont pas supprimés.

## Épreuves GitHub et état à rafraîchir
- Attestation d'identité officielle 1.17.9/code47, signer SHA-256 `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`, sept garde-fous des ressources et intégrité Hifz : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/41209120644
- Les 52 tests instrumentés de finales, restauration, schéma et migrations : **PASS** sur plusieurs versions précédentes dont https://github.com/applicreation0-web/quran-hafidh/actions/runs/41211861169 (la version immédiatement avant la correction d'orientation).
- **Contrôle dernier correctif `d08a97cb` en cours au moment de la rédaction**, ne pas supposer succès : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41212783170. JVM de ce commit déjà PASS.
- Test Android ciblé orientation et horloge sur les deux profils : https://github.com/applicreation0-web/quran-hafidh/actions/runs/41212816297. Le résultat exact doit être relu.
- Les anciens échecs P0 de paysage ne prouvent PAS un échec des 279 comparaisons ; les 8 shards du catalogue sont indépendamment verts.

## Porte finale de release
**NO GO jusqu'à :** Android rapide BOOX + téléphone + 52 tests Hifz PASS sur le même HEAD final ; revue visuelle BOOX/phone et tests limites après rebase du patch Work ; récupérations/migrations/backups/crédits strictement inchangés ; identité officielle de l'APK vérifiée ; compatibilité `adb install -r` réellement testée avec APK existant et vraie clé historique, ressources privées 7/7 ; contrôle physique de BOOX et absence de rémanence/ghosting. Si une preuve manque, ne pas déclarer GO et ne pas produire de release. Jamais d'APK public, pas de désinstallation, pas d'écrasement des acquis.
