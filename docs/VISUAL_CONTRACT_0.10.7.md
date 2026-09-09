# Quran Safeguard 0.10.7 — contrat visuel de publication

Statut : bloquant pour la publication 0.10.7.

## Identité visuelle figée

- L’application doit avoir un rendu sobre, chaleureux, premium et apaisant, sans aspect « application laboratoire ».
- Le crème / ivoire domine. Le vert structure et identifie. Le doré reste un accent discret ; il ne sert pas de couleur de petit texte.
- Palette de référence : fond général `#FBF7EF`, surface de lecture `#F7F2E8`, cartes/surfaces `#FFFDF8`, texte principal `#18392E`, texte secondaire `#514A43`, vert identitaire `#214B3B`, doré `#B0823F`.
- Aucun mode noir : les écrans de lecture restent clairs et chauds, y compris en lecture du soir.
- Les ombres restent faibles ou nulles ; les séparations reposent sur l’espace, de fines bordures et la hiérarchie typographique plutôt que sur une accumulation de grosses cartes.
- L’inspiration islamique/orientale reste discrète et ne doit jamais réduire la lisibilité.

## Structure et ergonomie

- Le contenu de lecture est prioritaire sur le chrome et les commandes.
- Les barres système Android sont respectées ; aucune information ne doit passer sous l’heure, l’encoche ou la barre de navigation.
- Les commandes hautes et basses restent compactes et se replient lorsque cela améliore réellement la lecture sans dupliquer une autre fonction.
- Les boutons conservent des cibles tactiles accessibles sans devenir de gros blocs décoratifs.
- Les écrans Qur'an utilisent des listes/sections simples plutôt qu’une accumulation de cartes élevées.
- L’interface doit rester compréhensible en niveaux de gris afin de préparer une adaptation liseuse/e-ink.

## Lecture du Qur'an

- La lecture libre conserve un curseur horizontal permanent de navigation 1–604, placé immédiatement au-dessus des commandes basses.
- Pendant le glissement du curseur, le numéro de page prévisualisé est visible ; la page n’est chargée qu’à la fin du glissement.
- Le panneau de navigation exacte par numéro de page reste disponible sans dupliquer le rôle du curseur.
- Le mode de lecture épurée masque le chrome, y compris le curseur, afin de laisser le Mushaf dominer l’écran.

## Tafsîr

- Le Tafsîr conserve comme appellations principales Jalalayn, Qurtubi et Qushayri.
- Le Tafsîr reste en anglais.
- Le panneau Tafsîr reste compact à l’ouverture et peut être agrandi pour la lecture longue.
- `A− / A+`, le sélecteur de Tafsîr, les références coraniques interactives et le retour après navigation restent fonctionnels.
- La traduction coranique fournie par la source reste en gras italique.
- La prose du commentaire est justifiée ; la poésie reste en italique, alignée à gauche, avec uniquement les retours à la ligne vérifiés dans la source.
- Un passage en prose ne reçoit jamais artificiellement une mise en page de poésie.
- Les doubles interlignes artificiels sont supprimés dans la prose.
- Les appels de notes Qushayri ne sont retirés que lorsqu’ils sont réellement orphelins après contrôle de source ; aucun nettoyage aveugle des nombres n’est autorisé.
- Les références et notes restent visuellement secondaires et ne doivent pas polluer le corps du commentaire.
- La sélection de verset est neutre, sans gros encadrement orange.

Ce contrat est figé pour 0.10.7 : toute nouvelle fonctionnalité non nécessaire à sa stabilité est reportée à 0.10.8.
