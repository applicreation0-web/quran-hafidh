# J4 — Décisions mémovisuelles et contrat d'affichage QCF (20 octobre 2026)

Statut : **contrat de conception / contrôles à réaliser**, PAS validation Android, certification de relation, ni catalogue activé.
HEAD vérifié avant rédaction : `22983438ec8749385c295da0aa6bd0c1f49322de`.

## Invariants existants (ne pas rouvrir)

- Atlas = carte des **Unités uniquement** ; les Passages/Amorces restent dans Lecture. Jalalayn reste dans le panneau Lecture, mushaf visible.
- Parcours bidirectionnel **Atlas → Lecture → Correspondances → retour au même verset/page/Unité**, ou accès contextuel depuis Sabqi sans modifier son moteur.
- Correspondances = **repère mémovisuel et réflexion**, jamais quiz, score, contrôle de récitation, nouveau crédit ou nouvelle progression.
- Seules les **huit catégories et huit icônes SVG/VectorDrawable originales** ; aucun neuvième type et aucun retrait/altération des 23 anciennes icônes.
- BOOX : portrait, monochrome, sans pavés, sans miniatures floues, sans animation, sans scroll vertical global ; le mushaf QCF réel garde sa géométrie et sa typographie.

## Architecture UI optimisée : un lecteur, trois agencements

Un même composant de feuillet QCF (images réelles, coordonnées authentiques, masque fade-away) avec :
1. **Comparaison** : deux pages côte-à-côte quand leur taille reste lisible ; toucher un feuillet pour le voir seul en grand, puis retour aux deux pages en gardant page/position. Ce n'est PAS un zoom arbitraire des glyphes.
2. **Série** : une page principale et une frise compacte d'occurrences (refrains, répétitions, alternances prouvées). La frise touche seulement au focus courant et ne génère pas de nouvelles bornes.
3. **Structure interne / macrostructure** : une petite frise A—B—X—B′—A′ indiquant les correspondances prouvées ; toucher un segment ouvre directement ses vraies pages QCF. Ne pas créer une seconde carte sémantique dans Atlas. La carte totale d'une sourate ne devient pas une miniature mushaf géante.

L'interface conserve uniquement les commandes pertinentes : Retour, Sources et agrandissement contextuel d'un feuillet ; la frise et le swipe servent au déplacement. Toujours texte court : **type + références + relation factuelle brève**.

## Données : trois niveaux à ne jamais confondre

1. **`originals[]`** : limites/IDs historiques immuables des 110 dossiers.
2. **`anchors[]`** : foyers éditorialement et géométriquement démontrés (versets, sous-plages ou mots), avec locateurs de preuves ; regroupements discontinus conservés séparément.
3. **Contexte de lecture** : voisin, section, unité ou page utile au repérage, sans surlignage qui prétend prouver la relation. Un foyer hors d'une plage originale n'est **jamais injecté silencieusement dans `anchors[]`** ; corriger le dossier après preuve ou rendre le contexte sans l'assimiler à un ancrage.

Une étiquette de type ne certifie ni le foyer ni le rendu. Source, classification, ancrages, visuel, droits et QA forment des portes distinctes. Sans preuve géométrique, ne pas atténuer artificiellement ou afficher le faux verset.

## Cas d'acceptation qui bloquent J4

- **R012** : 19:52 est hors de la référence historique terminant à 19:51. Interdiction de présenter 19:51 comme l'appel divin ; contexte séparé ou réparation éditoriale traçable.
- **R026** : seulement l'inversion jour/nuit locale attestée ; pas de chiasme global inventé.
- **R032** : `CENTRE` au niveau de toute al-Baqarah, pas un faux centre **interne à 2:142–152**. Le modèle actuel `Type.CENTRE` exige `INTERNAL` + mots exacts ; il faut un rendu de structure macro qui ne contourne pas cette restriction sans preuve.
- **R033** : segment central exact et rectangles de mots QCF sourcés ; pas hachure sur tout 2:255.
- **R038** : écho thématique début–fin != inclusio formelle prouvée.
- **R078** : alternance de 109:2–5 avec quatre rôles distincts ; une plage 2–5 uniformément surlignée ne la démontre pas.
- **R065 / R067 / R103 / R105** : la paire historique et les foyers effectifs divergent ; ne pas afficher deux passages trompeusement homologues.

## Remembering : rappel naturel, sans fonction quiz

Le chemin de mémoire demeure **repérer sur la page réelle → reconnaître le lien typé → évoquer le passage associé → le retrouver sur sa vraie page → revenir à l'Unité**. Aucun bouton « tester », aucune bonne/mauvaise réponse, aucun score ni stockage Hifz supplémentaire. La même position QCF visible à la lecture reste l'ancrage mémovisuel ; la frise montre le plan, pas une paraphrase du texte.

## Performance / vérification

- Conserver les décompressions Brotli hors UI, garde-fous d'annulation et WebView locale sans scripts ni réseau ; plafonner les feuillets simultanément chargés et n'ajouter un cache limité qu'après mesure.
- Vérifier affichage monochrome des huit icônes, hachure Centre, contraste/fade-away, lisibilité deux pages, pages 1/604, navigation/multifeuilletage, restauration exacte après rotation/recréation, aucun clignotement excessif, aucun impact sur l'acquis Hifz.
- Les simulations JVM, tests SVG et tests d'intégrité des références ne remplacent pas Gradle/instrumentation Android ni BOOX physique.
- Aucun `approved_v1.json`, SHA d'activation ou APK signé tant que les dossiers individuels et les vérifications requises ne sont pas validés. Aucun ajout des N001–N008 avant décision de fusion post-J5.

## Ordre des travaux

1. Classer par **capacité réelle de rendu**, pas par popularité du type ; préserver l'ensemble des 110 en audit.
2. Raccorder comparaison et série au lecteur existant avec foyer exact + retour au contexte.
3. Étendre **un seul lecteur** pour les ancres discontinues et les structures internes/globales démontrées ; tests négatifs pour les raccourcis trompeurs.
4. Auditer rendu QCF/BOOX, restauration/no Hifz writes et droits, puis autoriser uniquement le sous-ensemble complètement conforme.
5. Un APK privé complet après J5 seulement. Aucune publication publique sans autorisation.

## Rectificatif ferme — refrains Ar-Raḥmān / Al-Qamar

- **Style Atlas obligatoire : cinq pastilles visibles et coulissantes pour les 31 occurrences d'Ar-Raḥmān** (55:13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77). Le point actif est agrandi et **doublement cerclé** ; swipe horizontal ou commandes discrètes permettent de parcourir toute la série sans 31 boutons simultanés.
- Sélection d'une pastille : afficher **le mushaf QCF authentique** au verset choisi, avec **le refrain grisé, les versets précédent et suivant entièrement normaux, le reste en fade-away**. Si le voisin est sur une autre page, permettre le feuilletage réel au lieu de fabriquer une page fusionnée.
- **Al-Qamar : quatre occurrences 54:17,22,32,40**. Même règle contextuelle verset précédent/refrain/verset suivant, mais frise de quatre pastilles ; la chaîne narrative crée le rappel Nūḥ → ʿĀd → Thamūd → Lūṭ → Pharaon (chaque refrain encadre la transition au récit suivant). Ne pas convertir cette règle en point de progression Hifz.
- **Remembering** : sous la frise, un repère facultatif d'une seule ligne, constitué de deux *mots-balises arabes attestés*, pris dans le verset précédent et le suivant, avec leurs initiales et un **mot-clé court** indiquant le contexte (*étiquette mnémotechnique proposée*, non titre de tafsīr). Exemple Ar-Raḥmān 55:21 : **برزخ ↔ لؤلؤ** ; 55:63 : **جنتان ↔ مدهامتان**. Les initiales seules se répètent trop souvent : ne jamais les présenter comme identifiant unique.
- Conserver une même frise graphique que les Unités Atlas (légèrement réduite pour les répétitions), sans ajouter une carte graphique alternative, une grille de 31 boutons, un quiz, un score, ni des textes explicatifs longs.
- Prototype autonome vérifié 31/31 références et fenêtre coulissante 5/5 ; présente le vrai texte arabe voisin mais **PAS les vrais tracés QCF**. Fichier privé transmis séparément : `QURAN_HAAFIDH_RAHMAN_5_POINTS_REMEMBERING_31_OCCURRENCES.html`. Le contrôle QCF réel reste obligatoire en J4/J5 ; aucune relation activée.
