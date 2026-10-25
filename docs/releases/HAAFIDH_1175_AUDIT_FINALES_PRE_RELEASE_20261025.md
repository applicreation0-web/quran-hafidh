# Pré-release 1.17.5 — audit correctif moteurs, 25 octobre 2026

Branche : work/haafidh-1175-boox-ux-hifz-atlas.
Référence contrôlée avant ce correctif : 8c5eb464b557a420e9abbc1e811435d2b36001d4.

## Anomalies démontrées

1. **Sabqi, finale regroupée par page** : les blocs historiques `recentSabqi`
   ont des limites d'origine (souvent cinq lignes), différentes de celles des nouvelles
   pages. L'ancienne soustraction par égalité exacte ne supprimait pas les blocs
   entièrement Acquis. Correctif : soustraction par indices de lignes **réellement
   créditées**, en conservant date, streak, ordre et segments non acquis.
2. **Reprise d'une finale ouverte** : `unfinishedConsolidationSessionDate` lisait
   uniquement l'emplacement des séances ordinaires, même pour le préfixe de
   finale. Correctif : relire l'emplacement propre aux finales et migrer les
   anciens états sans perdre les répétitions.
3. **Forte accumulation historique** : 29 sous-blocs peuvent couvrir plus de
   30 pages QCF. Le moteur de finale accepte au maximum 30 unités; l'excédent
   ne doit pas provoquer d'exception. Correctif : trente pages maximum par
   finale, en conservant les portions non acquises dans l'historique pour un
   dimanche ultérieur.
4. **Pages partiellement acquises** : une page reconstituée d'après d'anciens
   blocs pouvait inclure des lignes déjà Acquises. Correctif : exclure les
   lignes déjà Acquises de la nouvelle finale, tout en préservant les unités
   historiques des sessions OPEN.

## Invariants conservés

- Apprentissage/Sabqi ordinaire ×37; Stabilisation/Itqan initiale ×40.
- Après An-Nas : 1 hizb ×3, deux lectures visibles + une recitation masquée
  à 100 % avec ancrages.
- Finale nouvelle : pages/portions exactes ×3, 1 visible + 2 masquées avec
  **les amorces de Lecture Ayah Themes** et la géométrie authentique QCF.
- Sessions OPEN sous SNOWBALL_EXTENDED : protocole persistant inchangé.
- Pas de crédit sans justificatif de répétition de la séance ordinaire.
- Sauvegarde atomique et absence de double crédit.
- Aucun changement de textes, numérotation, cartes ou titres Atlas.

## Limites des preuves à ce stade

L'audit source et les tests GitHub servent de preuves techniques, mais ne
constituent ni une validation physique BOOX, ni une preuve de signature compatible,
ni une autorisation de release. La préparation hors ligne du corpus privé reste
indispensable. Aucune compilation effectuée par ce document.

## Mise en conformité à exécuter

Exécuter : garde-fous Hifz, sourceContractTest, testDebugUnitTest, tests
d'instrumentation de persistence/process death, test des finales partielles
et restauration v3. Captures Android marquées « émulateur » exclusivement.
Toute erreur doit rester NO-GO et être documentée.
