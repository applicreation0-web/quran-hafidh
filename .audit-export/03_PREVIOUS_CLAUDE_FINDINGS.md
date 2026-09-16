# Résumé du précédent NO-GO Claude à revalider

B1 : Consolidation non accessible depuis l'UI/MainActivity.
B2 : readyUnits levait sur états partiels ; mélange lignes touchées / lignes possédées pouvait bloquer la file.
M1 : projection semaine/Accueil utilisait ancien fractionnement, pas planPage.
M2 : legacy partial sans voie de sortie ; crash possible après migration 5/5/5 -> 7+8.
M3 : coalescence de ranges adjacents masquant frontière de sourate.
Mineur : renderSabqiTodayReview sans canonicalize.
Latent : édition manuelle des ranges n'invalidait pas une session Consolidation figée.

Le re-audit doit vérifier les corrections dans le code final, pas accepter ce résumé comme preuve.