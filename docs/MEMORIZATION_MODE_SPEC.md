# Cahier des charges — Mode Mémorisation

## 1. Objectif

Ajouter un mode de mémorisation du Coran fondé sur le Mushaf de Médine, avec apprentissage progressif, masquage visuel, répétitions guidées, audio optionnel et validations explicites de jalons.

Le mode doit rester simple à utiliser, sans Tafsir, sans reformatage du texte coranique et sans découpage artificiel des versets.

## 2. Unité de travail

- L'apprenant sélectionne des **versets consécutifs entiers**.
- L'application affiche le nombre de lignes du Mushaf couvertes par la sélection.
- La cible recommandée est **environ 5 lignes**.
- Il n'est pas nécessaire d'obtenir exactement 5 lignes : l'apprenant choisit les versets de manière à s'en rapprocher au mieux.
- **Ne jamais couper un verset** pour atteindre exactement 5 lignes.
- La géographie et la mise en page du Mushaf de Médine sont conservées à l'identique.

## 3. Règles d'interaction du mode Mémorisation

- Le tap sur un verset sert uniquement à la **sélection / focalisation de mémorisation**.
- **Aucun Tafsir** n'est accessible en mode Mémorisation, en Light comme en Plus.
- Aucun tap ne doit déclencher simultanément plusieurs fonctions.
- L'audio, s'il est intégré, possède ses propres commandes.
- Si l'audio Al-Husary Muʿallim est utilisé, la synchronisation visuelle se limite au **verset entier en cours de récitation** : aucune surbrillance mot à mot.

## 4. Prise de contact avec le bloc sélectionné

Avant le travail ligne par ligne :

1. **Lecture complète du bloc : 2 fois**, texte entièrement visible.
2. **Écoute passive du bloc : 2 fois**, en suivant le Mushaf des yeux.

Ces passages servent à prendre connaissance du rythme et de l'enchaînement général avant l'apprentissage détaillé.

## 5. Protocole standard par ligne

Pour chacune des lignes L1 à L5 :

### 5.1 Audio préparatoire

- **Écoute passive : 2 fois**.
  - L'apprenant écoute Al-Husary et suit visuellement.
  - Il ne récite pas avec l'audio.

- **Écoute active : 3 fois**.
  - L'apprenant récite avec Al-Husary.
  - Le Mushaf reste visible.

L'écoute de contrôle n'est pas obligatoire après chaque ligne. Elle reste disponible à la demande si l'apprenant doute de sa récitation ou souhaite vérifier un passage.

### 5.2 Répétitions actives

Ordre obligatoire :

| Phase | Affichage | Répétitions |
|---|---|---:|
| Visible | 100 % du texte visible | **10×** |
| Masquage léger | environ 25 % masqué | **5×** |
| Masquage moyen | environ 50 % masqué | **5×** |
| Masquage fort | environ 75 % masqué | **5×** |
| Rappel total | 100 % masqué | **7× minimum** |

Soit **32 récitations actives minimum par ligne**.

Pour 5 lignes : **160 récitations actives individuelles minimum**, hors enchaînements.

### 5.3 Validation du 100 % masqué

- Les **3 dernières récitations doivent être correctes consécutivement**.
- Ces 3 réussites peuvent être comprises dans les 7 répétitions prévues.
- Si les trois réussites consécutives ne sont pas obtenues à la septième répétition, la phase continue au-delà de 7 jusqu'à obtention du critère.
- Toute aide visuelle pendant une tentative invalide uniquement cette tentative et remet le compteur des réussites consécutives à zéro pour ce jalon.

## 6. Boutons de confirmation de jalon

Le parcours doit comporter des **boutons de validation explicites** afin que l'utilisateur sache exactement où il en est.

### 6.1 Jalons intermédiaires par ligne

À la fin de chaque phase, afficher un bouton principal :

- **Valider — Visible 10/10**
- **Valider — 25 % 5/5**
- **Valider — 50 % 5/5**
- **Valider — 75 % 5/5**
- **Valider — 100 % 7/7**

Le bouton devient actif uniquement lorsque le nombre minimum de répétitions de la phase est atteint.

Pour le jalon **100 % masqué**, le bouton ne devient réellement validable que si les **3 dernières récitations ont été déclarées correctes consécutivement**.

### 6.2 Boutons fonctionnels complémentaires

À côté du bouton principal, proposer seulement des actions sobres et distinctes :

- **Refaire** : recommencer la phase courante sans perdre les jalons déjà validés.
- **Afficher brièvement** : révéler temporairement le passage ; invalide la tentative en cours et remet à zéro la série de réussites consécutives du jalon actuel.
- **Réécouter** : rejouer l'audio du verset concerné ; ne valide jamais une récitation.
- **Écoute de contrôle** : disponible à la demande, sans être imposée après chaque ligne.

Aucun de ces boutons ne doit valider automatiquement un jalon.

### 6.3 Validation d'une ligne

Quand toutes les phases d'une ligne sont terminées et que le 100 % masqué satisfait la règle des 3 réussites consécutives :

- bouton principal : **Valider L1** / **Valider L2** / etc.
- après validation, la ligne affiche un état **✓**.

## 7. Enchaînement cumulatif des lignes

La mémorisation ne doit pas produire cinq lignes apprises séparément sans maîtrise des transitions.

Après chaque nouvelle ligne validée :

- Après L2 : **L1 + L2 × 5**, puis jalon **Valider L1–L2**.
- Après L3 : **L1 → L3 × 5**, puis jalon **Valider L1–L3**.
- Après L4 : **L1 → L4 × 5**, puis jalon **Valider L1–L4**.
- Après L5 : **L1 → L5 × 7**, puis jalon **Valider les 5 lignes**.

Pour chaque enchaînement, les **3 dernières récitations doivent être correctes consécutivement** avant activation du bouton de validation du jalon.

Si une erreur se concentre sur une transition, l'application peut proposer une reprise ciblée de la ligne défaillante et de la jonction avec la ligne précédente, sans remettre le bloc entier à zéro.

## 8. Validation finale du bloc

Après validation des 5 lignes et de l'enchaînement complet :

1. **1 écoute passive complète** du passage.
2. **2 écoutes actives complètes** avec Al-Husary.
3. **3 récitations complètes texte visible**.
4. **3 récitations complètes à 50 % masqué**.
5. **5 récitations complètes à 100 % masqué minimum**.

Le jalon final n'est validable qu'après **3 récitations intégrales correctes consécutives, sans texte et sans audio**.

Bouton final : **Valider le bloc mémorisé**.

Après validation : afficher **Bloc mémorisé ✓**.

## 9. Règles en cas d'erreur

- Ne jamais remettre automatiquement les 5 lignes à zéro.
- Revenir uniquement sur la ligne ou la transition qui pose problème.
- Si nécessaire : **2 écoutes actives supplémentaires**, puis reprise au niveau de masquage approprié.
- Pour une phase masquée déjà commencée, conserver le nombre brut de répétitions mais remettre à zéro uniquement le compteur des réussites consécutives après utilisation d'une aide.
- Une fois la difficulté corrigée, reprendre l'enchaînement depuis la ligne précédente afin de renforcer la jonction.

## 10. Affichage de progression

La progression doit être immédiatement lisible, par exemple :

`L1 ✓  L2 ✓  L3 en cours  L4 —  L5 —`

Puis :

`Enchaînement : L1–L2 ✓ | L1–L3 en cours`

Pour la phase courante, afficher un compteur du type :

`50 % masqué — 3 / 5`

Pour le rappel total :

`100 % masqué — 6 / 7 • Série correcte : 2 / 3`

## 11. Principes UX

- Un seul bouton principal de validation visible à la fois.
- Les boutons **Valider**, **Refaire**, **Afficher brièvement** et **Réécouter** ont des fonctions exclusives et non ambiguës.
- Aucun jalon déjà validé n'est perdu lors d'une erreur ultérieure, sauf action volontaire de l'utilisateur pour recommencer.
- Les compteurs et jalons doivent persister si l'utilisateur quitte puis reprend la séance.
- Le Mushaf reste l'élément visuel dominant ; les contrôles ne doivent pas réduire excessivement la surface de lecture.
- Aucun texte d'explication éditoriale ou de Tafsir ne doit apparaître dans ce mode.

## 12. Résumé du protocole standard

Par ligne :

**2 écoutes passives + 3 écoutes actives + 10 visible + 5 à 25 % + 5 à 50 % + 5 à 75 % + 7 à 100 % + 3 réussites consécutives obligatoires.**

Enchaînements :

**L1–L2 ×5 → L1–L3 ×5 → L1–L4 ×5 → L1–L5 ×7**, chaque jalon exigeant **3 réussites consécutives**.

Le passage est définitivement validé par le bouton **Valider le bloc mémorisé** après **3 récitations intégrales correctes consécutives sans aide**.