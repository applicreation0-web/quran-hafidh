# Al-Ḥikam — audit contradictoire de traduction 0.10.3

Date : 2026-09-04

## Périmètre

Les 264 traductions françaises internes de Quran Safeguard ont été recroisées avec :

1. le matn arabe vérifié, qui reste l'autorité textuelle ;
2. la traduction anglaise numérotée publiée sur Islamic Pearls : https://islamicpearls.net/ibnattaillah2.html ;
3. les sources françaises déjà utilisées comme contrôles bibliographiques et terminologiques.

La traduction anglaise est un **contrôle contradictoire secondaire** : elle ne remplace jamais l'arabe et n'est pas recopiée dans le corpus de l'application.

## Contrôle automatique 264/264

- corpus local : 264 entrées ;
- référence anglaise retrouvée : 264 entrées ;
- rupture de numérotation : aucune ;
- alignement arabe moyen avec la référence : 0,992 ;
- alignement arabe minimum : 0,8684 ;
- échec dur d'alignement : aucun.

Un second classement multilingue anglais ↔ français a été utilisé uniquement pour prioriser la relecture humaine. Sa similarité moyenne est de 0,7276. Les scores faibles ne sont pas traités comme des erreurs automatiques : chaque cas retenu est arbitré par l'arabe.

## Relectures contradictoires ciblées

Les Ḥikam 9, 51, 60, 111, 168 et 252 ont été contrôlées après signalement automatique. Leur français reste fidèle à l'arabe ; aucune modification n'a été retenue simplement pour se rapprocher stylistiquement de l'anglais.

Les Ḥikam 174–176 ont, en revanche, révélé un affaiblissement réel du terme `الفاقات` : le rendu « besoins » ne transmettait pas suffisamment le sens de privation/détresse porté par le contexte et confirmé par le commentaire anglais. La métaphore `بسط المواهب` de la Ḥikma 176 devait également conserver l'image des « tapis ».

Corrections retenues :

- 174 : « La survenue des privations est une fête pour les aspirants. »
- 175 : « Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière. »
- 176 : « Les privations sont les tapis des dons. »

## Règles maintenues

- aucune traduction française n'est remplacée par une traduction automatique de l'anglais ;
- aucune notion absente de l'arabe n'est ajoutée pour imiter un commentaire ;
- les termes techniques doivent être traités par le lexique lorsque la traduction littérale seule écrase leur portée ;
- les commentaires d'al-Sharnūbī et d'Ibn ʿAbbād restent séparés du matn et séparés l'un de l'autre ;
- aucune synthèse IA n'est attribuée à un auteur classique.

## Traçabilité intégrée

Chaque entrée 1–264 porte désormais une métadonnée `translation_cross_audit` indiquant :

- le statut de contrôle anglais ;
- l'URL de la référence ;
- la date du contrôle ;
- la règle explicite selon laquelle l'arabe vérifié demeure l'autorité.
