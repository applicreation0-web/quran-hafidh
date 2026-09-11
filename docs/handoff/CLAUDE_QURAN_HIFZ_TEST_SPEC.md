# Quran Hifz — cahier des charges fonctionnel de handoff Claude

Date: 2026-09-11
Produit: Quran Hifz
Cible finale: BOOX Go 10.3 Gen II, test provisoire autorisé sur téléphone Android.
Principe global: OFFLINE-FIRST. Le fonctionnement essentiel ne dépend d'aucun serveur.

## 1. Frontière produit
Quran Hifz est une application séparée de Quran Safeguard. Elle ne contient aucun service Accessibility de blocage d'applications, aucune allowlist d'apps externes, aucun compteur de temps d'apps cibles, aucun joker Safeguard et aucune logique de déblocage.

## 2. Mushaf
- Mushaf de Médine, 604 pages, format canonique sans reflow.
- Les 604 pages sont embarquées localement dans l'APK sous `assets/mushaf/hafs/kfqc/svg-br/%03d.svg.br`.
- Une page demandée est décompressée localement puis injectée directement dans le lecteur. Aucun téléchargement runtime, aucune permission INTERNET.
- Géométrie locale versets/lignes dans `reader109/geometry.json`.
- UI: page centrée, proportions conservées, palette crème/noir, commandes sobres.
- Test obligatoire: ouvrir page 1, prouver que le vrai SVG local est injecté et visible; page suivante/précédente également.

## 3. Lecture / Étude
- Lecture libre du Mushaf.
- Tafsir accessible uniquement ici via bouton explicite après sélection d'un verset.
- Le tap sur un verset sélectionne le verset; le Tafsir s'ouvre via commande explicite.
- Panneau Tafsir max environ demi-écran, scroll interne, A-/A+, fermeture explicite/tap extérieur, texte non sélectionnable.

## 4. Tafsir
- Corpus local SQLite compressé, reconstruction locale.
- Corpus de test actuel: Al-Jalalayn anglais, 6236 versets, SHA-256 validé `26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56`.
- Le corpus français final reste à valider; ne pas inventer de traduction.
- Test: mode avion, ouvrir Tafsir d'un verset connu, contenu local visible.

## 5. Mémorisation libre
- Indépendante du parcours structuré.
- Sélection libre d'un verset/passage.
- Masquage optionnel et compteur manuel.
- Ne modifie jamais Sabqi, Itqān, Murājaʿah ni les curseurs structurés.
- Tafsir inaccessible dans ce mode.

## 6. Sabqi
- Unité métier primaire: verset. Les lignes ne servent qu'à la géométrie interne.
- Bloc = exactement 5 lignes coraniques consécutives à partir du curseur courant, sans rééquilibrage artificiel.
- Si la cinquième ligne coupe un verset, le bloc est marqué partiel et la prochaine séance reprend à la ligne suivante du même verset.
- Protocole figé = 37 répétitions: 15 visibles + 5 à 25% masqué + 5 à 50% + 5 à 75% + 7 à 100%.
- Si interruption, conserver bloc exact, masque, répétition suivante, aides et temps actif.
- Promotion vers Itqān: uniquement les versets entièrement terminés; une fin partielle ne promeut pas le verset complet.

## 7. Itqān
- Cycle infini sur corpus éligible, jamais de fin à An-Nās.
- Référence initiale: intervalle bas `2:1..2:74`, intervalle haut `49:1..114:6`, curseur initial `49:1`.
- Traversée: `49:1 -> ... -> 114:6 -> 2:1 -> ... -> frontière promue -> 49:1 -> ...`.
- Le curseur saute les gaps non éligibles.
- Une extension du corpus après Sabqi ne téléporte jamais le curseur Itqān.
- ×30 total. Masquage obligatoire. Révélations = signaux de difficulté.
- Reprise exacte: ex. interruption à 49:7 rep18/30 -> retour à rep19/30.

## 8. Murājaʿah
- Curseur indépendant `murajaahItqanCursor` distinct du curseur ×30 `itqanCursor`.
- Une séance Murājaʿah ne modifie jamais le curseur Itqān ×30.
- Deux flux: Sabqi récent en attente de révision + corpus Itqān ancien cyclique.
- Valeur de travail: 45 min, vitesse initiale 1 juz / 45 min = 9 s/ligne, à recalibrer prudemment.
- La borne affichée est une prévision; le résultat réel prévaut.

## 9. Planning Hifz
- Lundi / mercredi / vendredi = Sabqi.
- Mardi / jeudi = Itqān.
- Samedi / dimanche = Murājaʿah.
- Absence: report souple, jamais d'échec, jamais de double quota automatique, curseur conservé.
- `programStartDate` interdit de créer des absences rétroactives avant la création du programme.

## 10. Persistance
Sauvegarder localement au minimum: bornes versets/pages/lignes, indication partielle, curseurs, répétition, masque, aides, temps actif, progression récente, planning.
- Le temps actif doit utiliser un temps monotone et s'arrêter en arrière-plan/écran éteint.
- Force-stop puis relance doit restaurer l'état exact.

## 11. Audio
- Hifz uniquement.
- Reciter visé: Mahmoud Khalil Al-Husary Muʿallim.
- Pas de mot-à-mot: surbrillance du verset entier seulement.
- Audio séparé des répétitions: écouter ne doit jamais incrémenter rep ni déplacer un curseur.
- Architecture finale souhaitée: pack audio local/offline installé une fois.
- Ne pas redistribuer de corpus audio tant que source/licence/redistribution ne sont pas validées.

## 12. E-Ink
- Détection appareil uniquement pour rendu/performance, jamais pour activer des fonctionnalités métier.
- Changements compteur = rafraîchissement local; masques = zone concernée; nettoyage complet occasionnel.
- Chemin Onyx REGAL/GU pour partiel, GC pour nettoyage si API disponible; fallback Android sinon.
- Validation ghosting/flash requiert BOOX physique; le téléphone ne valide que le fonctionnel.

## 13. Tests bloquants
NO-GO si: écran blanc/page erreur, Mushaf absent, mauvais verset après reprise, perte de progression, Itqān s'arrête, Murājaʿah modifie Itqān, Tafsir s'ouvre en mémorisation, audio incrémente les reps, crash/ANR, APK non installable, service Safeguard présent.

## 14. Principe de preuve
Pour chaque fonction: fichier local -> moteur -> écran -> action utilisateur -> persistance/reprise. Une CI verte seule n'est pas suffisante; conserver captures/runtime pour les parcours critiques.
