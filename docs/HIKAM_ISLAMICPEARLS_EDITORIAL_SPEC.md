# Al-Hikam — cahier éditorial Islamic Pearls / al-Sharnubi / Ibn ʿAbbad

## Objectif

Faire évoluer le module Al-Hikam de Quran Safeguard Plus sans remplacer les auteurs classiques par une synthèse IA.
Chaque Hikma reste rattachée à son numéro canonique 1–264 et doit pouvoir afficher :

1. le matn arabe vérifié ;
2. une traduction française contrôlée ;
3. le commentaire arabe classique d'Abd al-Majid al-Sharnubi al-Azhari ;
4. une traduction française indépendante de ce commentaire ;
5. le commentaire correspondant d'Ibn ʿAbbad al-Rundi lorsqu'il est identifié et vérifié ;
6. une traduction française indépendante de ce second commentaire ;
7. les termes techniques reliés à un lexique ;
8. les références documentaires exactes.

## Interaction obligatoire

Sur la fiche d'une même Hikma, l'utilisateur doit pouvoir passer d'un commentaire à l'autre sans quitter la fiche :

- **al-Sharnubi** : commentaire plus condensé ;
- **Ibn ʿAbbad al-Rundi** : commentaire plus développé.

Le sélecteur doit rester visible au-dessus du commentaire. Le changement de commentateur ne modifie ni la Hikma affichée ni sa traduction. Il conserve autant que possible la position générale de lecture dans la fiche.

Si l'un des deux commentaires n'est pas encore vérifié pour une Hikma donnée, son option reste visible mais désactivée avec un statut explicite du type « non vérifié / indisponible » ; aucun texte ne doit être inventé pour remplir l'espace.

## Références de contrôle

- Islamic Pearls : https://islamicpearls.net/ibnattaillah2.html
  - référence de contrôle pour la correspondance des 264 Hikam ;
  - structure Hikma / sharh / arabe / traduction ;
  - conventions de termes techniques et présence d'un glossaire ;
  - pour l'édition personnelle Plus, peut servir de référence de travail et de comparaison, tout en conservant l'attribution et la provenance de chaque texte.
- Maktabat Ibn al-Arabi : https://www.ibnalarabi.com/books/hikam-ataiya.php
  - texte arabe du Sharh d'al-Sharnubi ;
  - chaque entrée doit être recoupée avec une édition paginée avant statut « vérifié ».
- Édition imprimée de contrôle : Sharh al-Hikam al-Ata'iyya, Abd al-Majid al-Sharnubi al-Azhari.
- Commentaire d'Ibn ʿAbbad al-Rundi : Ghayth al-Mawahib al-ʿAliyya fi Sharh al-Hikam al-ʿAta'iyya, à contrôler par numéro, passage et édition.

## Contrat de données par Hikma

Chaque enregistrement éditorial doit contenir au minimum :

- `source_number` : 1..264 ;
- `canonical_arabic` ;
- `french_translation` ;
- `commentaries` : collection indépendante de commentaires.

Chaque commentaire contient :

- `commentator_id` ;
- `commentary_author` ;
- `commentary_work` ;
- `commentary_arabic` ;
- `commentary_french` ;
- `commentary_source_url` ;
- `commentary_print_locator` ;
- `translation_status` ;
- `commentary_status` ;
- `technical_terms` ;
- `rich_spans`.

Aucune entrée ne doit être présentée comme « commentaire classique vérifié » si le numéro, le texte arabe, l'attribution et le locator n'ont pas été contrôlés.

## Mise en forme riche

La mise en forme n'est pas décorative : elle porte du sens et doit être stockée par rôles, pas par HTML arbitraire.

Rôles prévus :

- `technical_term` : terme du lexique, accent/couleur secondaire et graisse renforcée ;
- `author_emphasis` : emphase conservée en gras ;
- `commentator_emphasis` : emphase conservée en italique ;
- `quran_quote` : citation coranique visuellement distincte avec référence ;
- `hadith_quote` : citation de hadith avec source ;
- `editorial_bracket` : ajout explicatif du traducteur, distinct du texte de l'auteur ;
- `source_note` : note documentaire discrète.

Les couleurs doivent utiliser les rôles du thème Quran Safeguard afin de rester lisibles en clair/sombre et accessibles.

## Lexique

Un terme technique rencontré dans une Hikma ou l'un de ses commentaires doit être sélectionnable et ouvrir une fiche courte comprenant :

- forme arabe ;
- translittération normalisée ;
- sens littéral ;
- sens technique dans le contexte ;
- source de la définition ;
- renvois vers les Hikam et vers le commentateur dans lesquels le terme apparaît.

Termes prioritaires à contrôler à partir de la référence : `arif`, `abd`, `adab`, `aghyar`, `al-Haqq`, `hadra`, `mureed`, `qabd`, `bast`, `salik`, `shawq`, `yaqin`, `dhikr`, `zuhd`.

## Règles d'authenticité

- pas de résumé IA présenté comme commentaire d'un auteur ;
- pas de fusion de plusieurs commentateurs ;
- 1 commentaire = 1 auteur + 1 ouvrage + 1 source ;
- les deux commentaires restent deux objets documentaires distincts même lorsqu'al-Sharnubi reprend Ibn ʿAbbad ;
- la traduction française peut être interne, mais doit être identifiée comme telle ;
- les ajouts du traducteur doivent être visuellement distingués du texte classique ;
- toute différence de numérotation entre éditions doit être documentée ;
- le texte arabe demeure la référence en cas d'ambiguïté de traduction.

## Contrôles avant release personnelle

- couverture 1–264 sans doublon ;
- correspondance Hikma/al-Sharnubi contrôlée ;
- correspondance Hikma/Ibn ʿAbbad contrôlée ;
- aucun commentaire sans auteur, ouvrage et source ;
- test du sélecteur entre les deux commentateurs sur une même Hikma ;
- aucun mélange d'attribution lors du changement de commentateur ;
- validation des références coraniques et des hadiths cités lorsque présents ;
- tests de rendu gras/italique/couleurs/RTL ;
- tests du lexique et des renvois ;
- audit contradictoire éditorial avant release personnelle.