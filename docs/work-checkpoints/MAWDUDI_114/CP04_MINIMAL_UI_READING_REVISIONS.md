# CP04 — Fluidité visuelle et clés d’amorce (lecture/révisions)

Décision utilisateur : supprimer les pavés, ne pas surcharger la carte, maintenir les clés d'amorce dans **Lecture** ET **Révisions**.

Correctifs UI sans modification des moteurs Hifz :

- `BaqaraPilotScreen` : proportion initiale 18 % carte / 82 % Mushaf, focus 9 % / 91 %. Nœuds d'axe compacts (46dp contre 49/51dp), en-tête et légende sur une ligne elliptique sans ajout de cadres ; les deux flèches supplémentaires de pages n'apparaissent **que si le passage occupe plusieurs pages**. Les gestes de changement de page et la navigation de passage sont conservés.
- `SemanticTitlePopup` : aucun panneau long; au maximum deux lignes, typographie 14sp, boîte non opaque, non modale, sans animation, largeur plafonnée à 390dp, description accessible intégrale pour les titres longs. En Lecture, l'ancrage exact au-dessus des icônes reste `StudyReaderActivity.positionAboveIcons`. En Révisions, le même popup s'ancre désormais automatiquement au-dessus de la dernière barre de commandes réellement mesurée dans l'activité, sans modifier `HifzSessionActivity`. Aucune popup persistante.
- Sources : les clés du lecteur et des révisions restent branchées à leur corpus actuel jusqu'à validation du remplacement Ibn Kathīr → Ayah Themes. Ce changement est **seulement cosmétique et de lisibilité** ; la migration éditoriale et l'emplacement exact des 1 053 nouvelles clés ne sont pas annoncés comme terminés.
- Aucune altération des lignes QCF, sessions, acquis, planification, sauvegarde, source Jalalayn ou icônes figées.

Régressions requises : J6 pictogrammes, moteur Hifz gelé, rendu Android, pilote al-Baqarah, vérification manuelle BOOX du positionnement du titre au-dessus des barres et des deux modes Lecture/Révisions. Aucun APK final ou PASS matériel prétendu.
