# J6 — 23 icônes contractuelles : contrôle par écran (2026-10-10)

Portée : `Ui.java`, contrat de tests `UiIconMappingTest.java`, nouveau `scripts/verify_j6_icon_roles.py`, CI. Aucun moteur Hifz/Quiz ni fichier Mushaf modifié.

## Corrections réelles
- Les commandes d'icône seules n'affichent plus de symbole Unicode de secours : rôle non défini = erreur explicite, détectée en amont par la CI sur les usages connus.
- **Supprimer une plage**, **retirer un repère**, **effacer**, **annuler** utilisent désormais les vecteurs exacts contractuels `trash-2` ou `undo-2`, selon le rôle ; il n'y a aucune icône réinventée.
- Le test C22 valide chaque association de libellé-vers-vecteur, y compris les corrections.
- Contrat de 48 dp, description d'accessibilité, infobulles et absence de libellés sous icônes conservés.

## Audit exhaustif traçable
`scripts/audit_all_screen_icons.py` continue de recenser toutes les actions compactes, les écrans et les ressources directes. Le nouveau J6 interdit de nouvelles actions compactes à libellé littéral non mappé, vérifie 23 fichiers XML et 23 associations, vérifie les deux exceptions dynamiques de Hifz et les appels indirects de ses actions. `scripts/verify_contractual_ui_icons.py` vérifie toujours les 23 SHA de blobs gelés.

Des pictogrammes historiques **distincts des 23 rôles contractuels** subsistent là où ils représentent des fonctions sans équivalent sémantique parmi ces 23 (flèches page suivante/précédente, sourate/hizb, ajouter/enlever un compte, catégories Hifz, réglages). Les remplacer arbitrairement par l'une des 23 formes ferait perdre l'information et risquerait une régression. Ces exceptions sont inventoriées ; ce jalon **ne prétend pas** que tous les boutons de toutes les pages sont l'un des 23.

## Validation restante
Les audits sources et les tests JVM ne constituent pas des captures réelles des écrans BOOX. Une réception visuelle sur appareil reste nécessaire avant J7/J8. Aucun APK.
