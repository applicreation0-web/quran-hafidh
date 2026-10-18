# Quran Haafidh — appellations et icônes pour le prochain APK privé

Source de vérité pour les libellés **d'interface** (pas pour les titres religieux éditoriaux).
Branche unique : `work/haafidh-1171-ibn-kathir-integration-independent`.
Le contrat graphique des 23 pictogrammes dans `docs/FROZEN_UI_ICONS_13_PLUS_10.tsv` reste intact.

| Contexte | Appellation visible / accessible | Pictogramme réel | Règle |
|---|---|---|---|
| Accueil | **Atlas** | `ic_ui_semantic_map` (waypoints) | Ancienne dénomination « Carte » gardée comme alias technique, pas le nom du bouton d'accueil |
| Atlas — niveau supérieur | **Unité Atlas** | aucun pictogramme ajouté au titre | Synthèse globale, titre plus grand et gras |
| Atlas — niveau détaillé | **Passage Ayah Themes** | aucun pictogramme ajouté au titre | Titre source distinct, plus petit et italique ; original consultable |
| Lecture — marqueurs | **Ancres** | `ic_ui_semantic_anchor` (ancre) | « Afficher/Masquer les ancres » ; sélection sans gros pavé sombre |
| Indices de rappel | **Amorces** | `ic_ui_recall_key` (Lucide key-round) | Ne jamais les représenter avec l'icône Ancre ; icône approuvée inchangée |
| Ouvrir la carte | **Atlas** | `ic_ui_semantic_map` | Icône seule dans la barre, description « Atlas » |
| Ouvrir le Muṣḥaf | **Lecture** | `ic_ui_reading` | Icône seule |
| Commentaire | **Tafsir** | `ic_ui_tafsir` | Jalalayn disponible ; ne pas modifier le texte du tafsīr |
| Parcours Hifz | **Apprentissage** | `ic_hifz_new_lesson` | Ne pas changer le moteur Sabqi |
| Parcours Hifz | **Stabilisation** | `ic_hifz_anchor` (symbole du parcours, distinct de l'Ancre du lecteur) | Ne pas changer le moteur Itqān |
| Parcours Hifz | **Consolidation** | `ic_hifz_consolidation` | Ne pas changer le moteur |
| Parcours Hifz | **Révision** | `ic_ui_revision` / `ic_hifz_maintenance` selon le contexte existant | Le vocabulaire de la reprise historique est traduit à l'affichage |
| Commandes courantes | **Retour, Focus, Difficulté, Annoter, Progression, Paramètres** | pictogrammes déjà figés dans `Ui.iconFor` | Icônes sans libellé visible sous le bouton ; description accessible conservée |

## Titres éditoriaux : traitement séparé
- **366 unités Atlas** : 120 titres privés déjà approuvés (dont 7 corrections prêtes à être embarquées), et 246 propositions **non certifiées**. Aucun titre privé versé au dépôt public.
- **1 053 passages Ayah Themes** : originaux immuables, alias d'affichage uniquement quand justifiés et approuvés.
- **89 passages traversant plusieurs unités** : une seule identité de source, un libellé contextuel par portion uniquement après justification.
- Le chargeur privé reste verrouillé à 120 titres approuvés ; ne pas marquer « 366 titres intégrés ».

## Critères de validation
- `Ui.iconFor("Atlas")` == `Ui.iconFor("Carte")` (compatibilité) ;
- `Ui.iconFor("Afficher les ancres")` == `ic_ui_semantic_anchor` ;
- `Ui.iconFor("Afficher les amorces")` == `ic_ui_recall_key` ;
- Contrôle J6, tests Android et régression Hifz obligatoires après les commits ;
- Pas d'APK généré avant instruction explicite, pas de promesse de compatibilité BOOX sans essai physique.
