# Quran Safeguard 0.10.10 — Contrat du socle Parcours Hifz

Ce document fixe les décisions déjà validées pour éviter qu'elles soient perdues ou réinterprétées pendant l'implémentation.

## Séparation des modes

- `Mémorisation` reste le mode libre et ponctuel hérité de 0.10.9.
- `Parcours Hifz` est un orchestrateur distinct avec sa propre persistance.
- Une session libre de Mémorisation ne modifie jamais le calendrier Hifz.
- Le moteur de lecture/mémorisation peut servir de surface d'exécution d'une tâche Hifz, mais il n'est jamais la source de vérité du planning Hifz.
- Les fichiers de préférences sont explicitement distincts : `reader109` / `free_quran_reader` pour le lecteur libre, `hifz_01010` pour le parcours structuré.

## Rythme hebdomadaire par défaut

- Lundi : Sabqi
- Mardi : Itqān
- Mercredi : Sabqi
- Jeudi : Itqān
- Vendredi : Sabqi
- Samedi : Murājaʿah
- Dimanche : Murājaʿah

## Séance manquée / replanification

- Une séance manquée n'est pas un échec.
- Elle passe en retard/replanification.
- Son identité, sa date d'origine, son curseur et son quota sont conservés.
- Aucun déplacement silencieux des bornes.
- Aucun doublement automatique du quota.
- Le système peut suggérer le prochain créneau disponible de la même piste, mais le report est explicite.
- Après redémarrage ou changement de jour, la normalisation peut uniquement passer une tâche `PLANNED` passée en `OVERDUE` ; elle ne modifie ni date, ni quota, ni progression d'entraînement, ni curseur.

## Curseur coranique Hifz

Le curseur du Parcours Hifz n'est plus une chaîne de texte libre. Il est typé et contient :

- verset de début (`sourate`, `āyah`) ;
- verset de fin (`sourate`, `āyah`) ;
- page Muṣḥaf de début ;
- page Muṣḥaf de fin.

Les références sont validées sur les 114 sourates et les 6236 āyāt canoniques. Les pages sont limitées au Muṣḥaf de Médine fixe de 604 pages. Une référence impossible, une plage inversée ou une page hors 1–604 est rejetée.

La cohérence `āyah ↔ page` est en plus vérifiée directement contre les éléments `ayahPolygon` des SVG du Muṣḥaf de Médine embarqué. Un couple individuellement valide mais incohérent (par exemple un verset associé à une mauvaise page) est refusé. Une page Muṣḥaf absente ou illisible fait échouer la lecture/écriture de l'état Hifz de manière fermée.

## Sabqi

Le Sabqi utilise l'audio et le masquage progressif.

Contrat actuel du socle :

1. écoute passive ;
2. écoute active ;
3. texte visible ;
4. masquage 25 % ;
5. masquage 50 % ;
6. masquage 75 % ;
7. masquage 100 % ;
8. test final entièrement masqué.

Le protocole structuré reste distinct du planning : terminer une étape d'entraînement ne change jamais à lui seul la date, le curseur ou le quota Hifz.

## Itqān

L'Itqān sert à consolider une page déjà apprise.

Contrat actuel du socle :

1. une page travaillée 30 fois avec texte visible ;
2. masquage 25 % ;
3. masquage 50 % ;
4. masquage 75 % ;
5. masquage 100 % ;
6. test final entièrement masqué.

L'audio n'est pas rendu obligatoire par le contrat Itqān actuel. Chaque palier masqué exige au moins une réussite non assistée avant le passage au palier suivant ; une tentative incorrecte ne valide jamais le palier.

## Révélations / aide

- Une révélation est comptabilisée.
- Une révélation ne compte jamais comme répétition réussie.
- Elle remet à zéro la série de réussites consécutives de l'étape.
- La première tentative après révélation ne peut pas prolonger artificiellement une série de réussites.
- Une étape assistée ne peut pas être validée tant qu'une tentative non assistée conforme n'a pas repris le contrôle.

## Murājaʿah

Le calendrier réserve samedi et dimanche à Murājaʿah, mais son protocole d'entraînement détaillé n'est volontairement pas inventé dans le socle actuel. Il sera verrouillé séparément avant implémentation.

## Persistance

Le Parcours Hifz utilise un stockage séparé (`hifz_01010`) avec schéma versionné **2**.

Sont persistés séparément du lecteur libre :

- tâches Hifz ;
- piste Sabqi / Itqān / Murājaʿah ;
- date d'origine ;
- date planifiée courante ;
- curseur coranique typé (début/fin + pages) ;
- quota ;
- statut ;
- étape d'entraînement courante ;
- répétitions ;
- réussites consécutives ;
- révélations ;
- état assisté ;
- fin du protocole d'entraînement.

Toute corruption du stockage Hifz doit échouer de manière fermée et ne doit pas écraser automatiquement l'état par un état vide. Un ancien état de schéma 1 à curseur texte libre n'est pas interprété silencieusement par le schéma 2.
