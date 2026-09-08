# Améliorations sobres à partir de Quran Safeguard 0.10.8
Date : 2026-09-08
Base : 4a843b7f349ba567e57ef51c91d20c51216d043e
Branche : work/0.10.8-sober-reader-ux

## Règles impératives
- Texte du tafsir strictement intact : pas de réinterprétation, reformulation, correction éditoriale, ajout, suppression ou reconstruction. Préserver notes, références, signes, paragraphes et poésie. Les travaux portent sur l'interface.
- Garder l'identité construite : moderne, sobre, crème pour les lecteurs, vert mesuré, doré discret, motifs fins. Aucun décor derrière le texte.
- Aucun doublon ajouté par l'interface. Les répétitions de la source restent intactes.
- Barre d'état Android visible et gérée par Android dans toutes les fonctionnalités : aucun masquage, personnalisation ou recouvrement. Seules les commandes de Safeguard disparaissent.
- Fonctionnement actuel de protection accepté : conserver l'architecture et le périmètre. Suivi, comptage et interception limités aux applications cibles. Aucun suivi des applications bancaires, de sécurité ou autres applications hors cible. Aucun nouvel inventaire ou diagnostic de leur activité.
- Préserver les règles de lecture, de déblocage et de compteurs déjà convenues.

## Liste actualisée et exemples
| N° | Travail | Exemple | État |
|---|---|---|---|
| 1 | Préserver le texte source et les corpus | Notes et poésie identiques à la base | Invariant ; aucun changement de corpus dans le premier lot |
| 2 | Alléger les blocs haut et bas des lecteurs | Référence et actions sur une rangée compacte si possible | À poursuivre |
| 3 | Boutons plus petits visuellement, zones tactiles confortables | A− et A+ discrets avec cible de 48 dp | Premier lot : trois boutons du tafsir ramenés à 48 dp sans remplissage interne |
| 4 | Masquer les commandes Coran et tafsir, révéler au toucher | Commentaire visible lorsque sa barre disparaît | Coran existant ; tafsir à compléter |
| 5 | Garder le délai de 2,5 s sans affichage permanent | Barre effacée après inactivité | Existant à vérifier |
| 6 | Premier toucher réservé à la révélation | Un toucher ne révèle pas et n'ouvre pas simultanément le tafsir | À corriger/vérifier |
| 7 | Toucher une zone libre pour masquer | Toucher la marge rend la place au texte | À ajouter |
| 8 | Appui long sur un verset pour le tafsir Plus | Ouvrir le commentaire directement, accès visible équivalent | À implémenter et vérifier |
| 9 | Conserver les gestes droite/suivant et gauche/précédent | Défilement vertical sans changement accidentel de page | Existant à vérifier |
| 10 | Réutiliser la navigation par référence de page | Page 42 ouvre le panneau de navigation existant | Accès à la navigation déjà confirmé, parcours à vérifier |
| 11 | Suspendre le masquage pendant un réglage | Plusieurs pressions sur A+ sans disparition | Partiel à compléter |
| 12 | Commandes sans doublons | Une commande par fonction sur un écran | À harmoniser |
| 13 | Retour par niveau et restauration de position | Note, puis commentaire, puis lecteur ; même passage au retour | Partiel à vérifier |
| 14 | Changement d'auteur fiable | Même verset, bonne plage, aucune ancienne réponse sous le nouvel auteur | À vérifier |
| 15 | Crème unique et soleil réservé à la luminosité | Aucun changement de thème | Existant à préserver |
| 16 | Préserver la barre d'état Android partout | Heure et batterie restent visibles | Premier lot : retrait des deux personnalisations du thème ; audit global restant |
| 17 | Harmoniser le décor sans refaire l'identité | Contours fins, couleurs modérées | À examiner écran par écran |
| 18 | Uniformiser icônes, marges, titres et boutons | Même famille d'icônes, intitulés courts | À poursuivre |
| 19 | Simplifier accueil et menus | Un accès principal Reprendre, pas de cartes redondantes | À auditer puis corriger |
| 20 | Clarifier les sélections et leur fermeture | Nombre sélectionné et Terminer | À vérifier |
| 21 | État de protection réel et activation concise | État actualisé au retour d'Android | À vérifier sans refonte du moteur |
| 22 | Validation et retour cible fiables | Débloquer et ouvrir quand les conditions sont remplies | À vérifier, règles inchangées |
| 23 | Périmètre strictement cible et compteurs conservés | Aucune consommation de temps hors cible | Vérification de non-régression, aucun suivi supplémentaire |
| 24 | Permissions minimales | Aucun accès nouvellement ajouté pour cette interface | Vérification sans changement fonctionnel non nécessaire |
| 25 | Audit Light et Plus et contrôle sur appareil | Lecture, tafsir, notes, retour, validation, cible | Avant toute sortie |

## Premier lot réellement préparé
- Trois commandes de l'en-tête tafsir (agrandir/réduire le panneau, A−, A+) : dimensions 48 × 48 dp, padding nul. Aucun changement de police du commentaire ni de rendu du texte.
- Suppression de windowLightStatusBar et statusBarColor dans le thème applicatif pour laisser les valeurs héritées Android. Les autres réglages du thème restent en place.
- Contrôle du périmètre : aucune modification du moteur de protection, des compteurs, des permissions, des corpus, des parseurs ou des scripts de génération.
- Comparaison du code : toute la partie InteractiveTafsirText et les fonctions de rendu en dessous sont identiques à la base.

## Limites de validation
Pas de compilation Android ni de test visuel sur appareil effectué pour ce premier lot.
La largeur effective, les grandes polices, le comportement système de la barre d'état et les autres écrans doivent être vérifiés avant publication.
Ce document ne constitue pas une annonce de release ; la version 0.10.8 publiée n'est pas remplacée.
