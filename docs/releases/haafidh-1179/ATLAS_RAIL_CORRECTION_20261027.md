# Correction visuelle 1.17.9 — frise Atlas, sans pavés

Retour utilisateur du27octobre : les grandes surfaces rectangulaires ne reprennent pas Atlas.
Référence réelle relue : `MawdudiMapScreen.drawAtlas` : trait1dp, points5dp/9dp, sélection double cercle14dp/18dp.

Correction native, limitée à MainActivity :
- Sept jours sur le même axe graphique ; vrais compteurs et états sous les points, jours au-dessus ; sélection double cercle. Cibles tactiles indépendantes du petit point dessiné.
- Retrait des cadres de séances et des fonds crème hérités de Ui.column. Toutes les surfaces de ces écrans restent blanches.
- Activités sur lignes compactes, intitulé et état puis un résumé court ; même détail intégral au toucher et même action protégée.
- Reprises alignées à gauche, date d'origine affichée une seule fois.
- Historique incomplet : dénominateur inconnu `n/?`, sans total inventé.

Aucun changement de planificateur, moteur, crédit, fichier de données ou icône. Les tests de l'ancienne candidate ne valent pas validation de ce nouveau commit. Les captures natives et le résultat CI courant feront foi. Aucune publication officielle.
