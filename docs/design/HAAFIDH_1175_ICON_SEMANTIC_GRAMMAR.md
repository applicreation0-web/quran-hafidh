# HAAFIDH 1.17.5 — convention visuelle Hifz (BOOX)

**Modèle définitif pour le candidat iconographique, sous réserve des captures BOOX.**

## Grammaire basée sur la fonction, pas sur la décoration
- **Sabqi : un livre.** Apprentissage = un livre avec + (nouvelle matière). Renforcement = livres repris, avec le retour circulaire (corpus appris récent, révision cumulative). Le livre ne signifie jamais « Acquis ».
- **Itqān : des feuillets délimités.** Stabilisation = **un** feuillet et retour circulaire (répétitions intensives). Consolidation = **plusieurs** feuillets et exactement le même retour circulaire (révision cumulative des unités Itqān). Les deux partagent le motif de répétition et non une coche de réussite.
- **Révision : visibilité du Mushaf.** Œil barré = active avec texte masqué. Œil ouvert = passive avec texte visible. Même corpus d'Acquis, méthodes différentes. Aucune flèche de « promotion » entre les deux.
- **En-têtes des trois groupes** et paires physiquement adjacentes dans Parcours : Sabqi, Itqān, Révision.
- **Statuts Appris/Stabilisé/Acquis** : conservent leurs labels et la logique de données réelle. Les icônes de travail ne les représentent jamais. « Acquis » reste conditionné à la vraie validation.
- **Finales** : icône de finalisation existante, distincte de la coche et des six modes; la finale peut autoriser une promotion seulement par le moteur et les preuves de validation.
- **Rattrapage** : calendrier + horloge, date et catégorie détaillées; ne signifie ni faute ni acquis. Garde-fous chronologiques inchangés.

## Navigation générale (même famille)
Accueil: dashboard ; Parcours: route ; Lecture: book-open ; Atlas: compass ; Progression: colonnes croissantes ; Paramètres: engrenage historique inchangé.

## Sources
Pictogrammes de base **Lucide** (https://lucide.dev ; licence ISC) tirés du dépôt lucide-icons/lucide, fichiers SVG publics. Convertis en Android VectorDrawable sans bitmap, traits uniformisés à 2,2 unités noires sur fond transparent. Les flèches de reprise dans les trois modes composites sont des éléments locaux de conception construits avec la même grammaire des tracés.
Les 23 icônes d'actions figées ne sont ni remplacées ni modifiées. Les fichiers ic_palace_*.xml conservent leur nom historique pour préserver les références de code mais ne contiennent plus d'architecture stylisée.

## Validation
1. Tests de compilation/Mapping des neuf anciennes ressources remplacées plus les trois nouveaux vecteurs; ne pas considérer le succès des tests Java comme une validation graphique.
2. Captures **réelles Android** Accueil/Parcours, contrôle des six icônes ensemble à 24/62/124 dp, placement et contraste. Compte des chemins et vérification visuelle.
3. Contrôle BOOX physique obligatoire pour le ghosting et le contraste. **Ne pas publier une release sans ce contrôle.**
