# Correspondances — contrat des huit relations

Mandat courant : `Work_Quran_Haafidh_Complet (1).zip`, SHA-256 `e6138652111df555c7059c5c0d34c5c75ce09349c3506055055c0c49506e3431`. Les huit SVG de `icons_contractual_8/` sont les originaux inchangés de ce pack.

| Code | Nom public | Critère |
|---|---|---|
| REPETITION | Répétition | Retour d'un verset, d'une formule ou d'un motif; distinguer reprise exacte, partielle et refrain |
| RESSEMBLANCE | Ressemblance | Correspondance documentée de sens, fonction ou organisation; pas une association vague |
| OPPOSITION | Opposition | Contraste réel de conduites, idées, groupes ou conséquences |
| PROGRESSION | Progression | Développement orienté, précision, suite ou annonce/accomplissement; aucune supériorité supposée |
| ALTERNANCE | Alternance | Retour régulier A–B–A–B de fonctions ou éléments effectivement identifiés |
| INVERSION | Inversion | Ordre A–B / B′–A′ avec appariements prouvés; centre non obligatoire |
| CENTRE | Centre | Disposition A–B–X–B′–A′ dont le segment central et les correspondances sont attestés |
| DEBUT_FIN_INCLUSIO | Début–Fin (inclusio) | Reprise significative entre ouverture et clôture du même ensemble; pas une simple jonction de sourates |

Un type principal par dossier. Les autres motifs restent des qualifications documentaires, sans neuvième catégorie. Un ancien verdict à cinq types est conservé comme historique, pas converti aveuglément en approbation.

## Tracés et conversion Android

- Centre : carré hachuré et exactement quatre traits horizontaux; aucun cercle ni arc. Le hachurage d'un segment du Muṣḥaf est un traitement distinct, conditionné à une preuve éditoriale et une géométrie du segment.
- Progression : quatre points de rayon strictement croissant. Alternance : plein, creux, plein, creux. Les orientations et nombres des autres dessins restent ceux des SVG.
- `scripts/correspondence_icon_vectors.py` produit les huit `ic_corr_relation_*.xml` depuis les SVG. Le dessin 72×48 est centré dans un viewport72×72, taille intrinsèque24dp, sans étirement ni modification du tracé.
- Le carré central utilise un clip-path Android explicite. Les traits de hachure restent dans le carré.
- Les 23 vecteurs gelés de l'application et leur manifeste restent inchangés. Les nouveaux noms sont additifs et ne réassignent aucune icône Atlas/Hifz.

## Preuves et limites

Cinq tests Python vérifient le manifeste8/8, les pathData conservés, le centrage, le carré/clip/quatre traits, et les quatre rayons croissants. Le générateur peut être contrôlé avec `--check`. Ces tests sont ajoutés au workflow source/JVM.

La comparaison source/équivalent reconverti en SVG couvre24/28/36px et deux couleurs simulées, avec rendu statique sharp/librsvg. L'écart moyen RGB maximal observé est0,0154 sur255. Cette comparaison n'est pas une capture Android ni BOOX et n'atteste pas les états natifs d'un bouton.

**Piège d'intégration à traiter :** `Ui.setButtonIcon` applique une teinte globale. Appliquée au nouveau dessin Centre, elle recolorerait aussi son fond blanc et pourrait masquer la hachure. Le futur contrôle de relation doit préserver les couleurs internes, les états lisibles et la cible tactile, sans changer le comportement des 23 anciens boutons.

État : géométrie et conversion préparées, contrôles statiques réussis; rendu natif Android, différenciation avec les23 icônes en conditions réelles et recette BOOX NON EXÉCUTÉS. Les nouveaux vecteurs ne sont pas encore branchés dans une interface Correspondances.
