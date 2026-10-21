# Correspondances — reclassement sourcé R001–R110 (21 octobre 2026)

## Autorité et méthode
Priorité donnée aux rapprochements précis de Jalāl al-Dīn al-Suyūṭī, *Asrār Tartīb al-Qurʾān*, lorsqu'ils existent, sans lui attribuer artificiellement les relations observées directement dans le Coran.
- 24 dossiers portent une provenance historique al-Suyūṭī ; six ont des localisateurs ciblés repris des audits J2/J3, notamment R004/R005/R007/R008/R009/R010.
- 84 dossiers proviennent principalement de comparaisons du texte arabe coranique ; 2 de lectures structurales Farrin/Azaiez.
- Al-Suyūṭī peut attester un tanāsub sans attester nécessairement l'une des huit catégories graphiques de l'application.
- Chaque dossier conserve une explication, un lien à la source historique et, s'ils existent, des repères bibliographiques particuliers ; la provenance et la force de preuve restent distinctes.

## Livrable privé additif, jamais publié au dépôt
- Original `historical_110_v1.json` SHA256 : `b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2` (intouché).
- Nouveau `assets/correspondences/reviewed_110_v2.json` SHA256 : `d9867ba3ade9c8902fa98cad76cfe92662a7bfc0a9a30946cdd1707c230c6bb7` (chargé par `CorrespondenceMushafActivity`; strictement privé).
- Le pack hors dépôt contient les neuf actifs initiaux *plus* le dixième `reviewed_110_v2.json`; 110 dossiers, **277 plages historiques inchangées**.
- Statuts documentaires J3 maintenus: 80 documentés, 24 en attente, 6 écartés ; quatre ID historiquement approuvés R030/R037/R047/R048. Aucun statut augmenté par simple reclassification.

## Cas corrigés
- **R004**: PROGRESSION → RESSEMBLANCE/TANĀSUB : 4:58 ↔ 5:1, al-Suyūṭī, *Asrār*, section al-Māʾidah, pagination Islamweb p.94 (connexion des dépôts et contrats, sans chronologie). URL historique Baqarah erronée remplacée pour l'affichage.
- **R005**: PROGRESSION maintenue au sens *ijmāl → tafṣīl*, non succession d'événements, 5:120 ↔ 6:1.
- **R007**: PROGRESSION par récit bref → développé, 10:71–73 ↔ 11:25–49, al-Suyūṭī, *Asrār*, section Hūd.
- **R026**: INVERSION limitée à 91:3–4 ↔ 92:1–2. 91:1–2 n'est qu'un contexte des plages historiques, non foyer surligné.
- **R040**: répétition littérale dans les 8 versets 26:9,68,104,122,140,159,175,191, pas dans les huit blocs de deux versets.
- **R053**: motif `ومن يتق الله` en 65:2,4,5 ; 65:3 contexte hors motif. Ne pas colorier un ayah entier comme identité de la formule.
- **R032**: proposition de type CENTRE explicitement attribuée à Farrin 2010, cinq membres internes à 2:142–152, centre proposé 2:147–148 ; géométrie QCF non certifiée.
- **R033**: proposition CENTRE pour 2:255 attribuée Farrin/Azaiez ; **aucune géométrie intra-ayah activée sans preuve**.
- **R038/R097**: retrait des catégories graphiques non démontrées ; dossiers conservés en archives privées.
- **R078**: ALTERNANCE grammaticale je/vous sur 109:2–5 ; une voix, pas deux dialogues.
- **R011/R053**: pour des motifs infraverset non géométriquement prouvés, interdiction d'un faux surlignage d'ayah entière.

## Intégration Android
`CorrespondenceMushafActivity` attend strictement le SHA du nouveau privé V2 ; refuse ancien V1 ou contenu incomplet ; vérifie que chaque foyer de lecture appartient aux limites d'une plage historique. Les six exclus et dossiers sans type démontré restent accessibles par ID mais ne sont pas suggérés comme relation structurée sur le mushaf. Aucun moteur Hifz/écran Atlas n'a été édité.

## Livraison conditionnée
Aucune release, aucun nouvel APK signé déclaré prêt par ce document. CI à vérifier sur le dernier HEAD, puis assemblage hors ligne avec le **nouveau pack de dix ressources**, tests Android instrumentés et BOOX physique. Le script de finalisation précédent, épinglé au pack de neuf ressources, doit être remplacé par la version privée v2. Aucun fichier privé ni certificat n'est versé dans le dépôt.
