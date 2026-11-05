# Contre-revue indépendante — mandat du 5 novembre 2026

Périmètre relu : `a266b279e3f0c1a9684861aa703ca5e2125ef3d9..7a1ee9741e9c988462536dd089b14cefe740a87d`, lecture indépendante de l’ensemble du diff, sans mutation du checkout.

Aucun défaut critique démontré. Deux défauts importants relevés et corrigés dans le lot de réception suivant :

| Constat | Cause | Correction | Vérification requise |
|---|---|---|---|
| Accès Lecture A/B perdu | Le remplacement des en-têtes Tafsir avait supprimé leur clic ; les taps QCF compacts sélectionnent un verset | Rétablissement du clic sur chaque en-tête, référence active dans la description d’accessibilité | Tap visible A puis B, référence/page exactes dans Lecture et retour |
| Preuve frise insuffisante | `performClick` sur vues hors viewport, liens seulement trouvés et détails fermés avant capture | Défilement, taps injectés sur commandes visibles, liens Lire suivis pour les 45 occurrences, captures premier/dernier détail ouvert | Android téléphone et BOOX émulé |

## Décision de portée explicite

Les NOUVELLES sessions présentent les blocs ×7 non encore reçus avant le passage continu ×1 déjà reçu, y compris les semaines ordinaires. Cette règle donne la priorité au travail dû sans changer protocole, nombre de répétitions, identifiants ou crédits. Les sessions OPEN conservent exactement leur vecteur persisté. Conséquence : l’ordre visuel d’une nouvelle semaine ordinaire suit aussi cette priorité.

## Limites de cette contre-revue

- Elle ne certifie ni matériel BOOX, ni ghosting, ni touches E-Ink, ni migration réelle. Les photographies de référence ne sont pas jointes à cette session ; les contraintes écrites s’appliquent.
- Elle contrôle la portée déclarée des sources héritées ; elle ne constitue pas une nouvelle collation indépendante de chaque ouvrage.
- Les fixtures reproduisent les règles de recevabilité, pas la quantité exacte de travail due dans la sauvegarde personnelle absente.
- Le verdict de réception dépend des résultats Android et des captures sur le commit corrigé ; cette revue n’est pas une permission de fusion ou de release.

## Rectification du décompte documentaire initial

Le recomptage du JSON au HEAD de départ trouve **23 fiches avec au moins un localisateur non coranique et 37 avec les seules références coraniques**, et non 22/38. Parmi les 23, R070 contient seulement une synthèse adaptée d’Ibn ʿĀshūr qui atteste le contexte général, pas ses trois appariements. Ce décompte ne transforme pas cette source secondaire en attestation classique des couples. Les 60 notices sont conservées avec leur portée explicite.
