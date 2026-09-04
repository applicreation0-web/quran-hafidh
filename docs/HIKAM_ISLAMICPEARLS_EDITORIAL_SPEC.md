# Al-Hikam — cahier éditorial Islamic Pearls / al-Sharnubi

## Objectif

Faire évoluer le module Al-Hikam sans remplacer les auteurs classiques par une synthèse IA.
Chaque Hikma reste rattachée à son numéro canonique 1–264 et doit pouvoir afficher :

1. le matn arabe vérifié ;
2. une traduction française contrôlée ;
3. le commentaire arabe classique d'Abd al-Majid al-Sharnubi al-Azhari ;
4. une traduction française indépendante de ce commentaire ;
5. les termes techniques reliés à un lexique ;
6. les références documentaires exactes.

## Références de contrôle

- Islamic Pearls : https://islamicpearls.net/ibnattaillah2.html
  - référence de contrôle pour la correspondance des 264 Hikam ;
  - structure Hikma / sharh / arabe / traduction ;
  - conventions de termes techniques et présence d'un glossaire ;
  - ne pas recopier en masse la traduction anglaise du site dans l'APK sans droit explicite.
- Maktabat Ibn al-Arabi : https://www.ibnalarabi.com/books/hikam-ataiya.php
  - texte arabe du Sharh d'al-Sharnubi annoncé comme relevant du domaine public par le site ;
  - chaque entrée doit être recoupée avec une édition paginée avant statut « vérifié ».
- Édition imprimée de contrôle : Sharh al-Hikam al-Ata'iyya, Abd al-Majid al-Sharnubi al-Azhari.

## Contrat de données par Hikma

Chaque enregistrement éditorial doit contenir au minimum :

- `source_number` : 1..264 ;
- `canonical_arabic` ;
- `french_translation` ;
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

Un terme technique rencontré dans une Hikma ou son commentaire doit être sélectionnable et ouvrir une fiche courte comprenant :

- forme arabe ;
- translittération normalisée ;
- sens littéral ;
- sens technique dans le contexte ;
- source de la définition ;
- renvois vers les Hikam dans lesquelles le terme apparaît.

Termes prioritaires à contrôler à partir de la référence : `arif`, `abd`, `adab`, `aghyar`, `al-Haqq`, `hadra`, `mureed`, `qabd`, `bast`, `salik`, `shawq`, `yaqin`, `dhikr`, `zuhd`.

## Règles d'authenticité

- pas de résumé IA présenté comme commentaire d'un auteur ;
- pas de fusion de plusieurs commentateurs ;
- 1 commentaire = 1 auteur + 1 ouvrage + 1 source ;
- la traduction française peut être interne, mais doit être identifiée comme telle ;
- les ajouts du traducteur doivent être visuellement distingués du texte classique ;
- toute différence de numérotation entre éditions doit être documentée ;
- le texte arabe demeure la référence en cas d'ambiguïté de traduction.

## Contrôles avant release

- couverture 1–264 sans doublon ;
- correspondance Hikma/commentaire contrôlée ;
- aucune entrée sans source ;
- validation des références coraniques et des hadiths cités lorsque présents ;
- tests de rendu gras/italique/couleurs/RTL ;
- tests du lexique et des renvois ;
- audit contradictoire éditorial avant publication.
