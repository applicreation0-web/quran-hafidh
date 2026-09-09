# Quran Safeguard 0.10.10 — Contrat du socle Parcours Hifz

Ce document fixe les décisions déjà validées pour éviter qu'elles soient perdues ou réinterprétées pendant l'implémentation.

## Séparation des modes

- `Mémorisation` reste le mode libre et ponctuel hérité de 0.10.9.
- `Parcours Hifz` est un orchestrateur distinct avec sa propre persistance.
- Une session libre de Mémorisation ne modifie jamais le calendrier Hifz.
- Le moteur de lecture/mémorisation peut servir de surface d'exécution d'une tâche Hifz, mais il n'est jamais la source de vérité du planning Hifz.
- Les fichiers de préférences sont explicitement distincts : `reader109` / `free_quran_reader` pour le lecteur libre, `hifz_01010` pour le parcours structuré.

## Bornes du parcours

Le réglage rare du Parcours Hifz conserve séparément :

- début et fin Sabqi en sourate + verset ;
- début et fin Itqān en sourate + verset.

Sabqi et Itqān restent indépendants : aucune borne de l'un ne déplace implicitement la borne de l'autre. Ces bornes font partie de la configuration Hifz persistante et survivent au redémarrage.

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
- Après redémarrage ou changement de jour, la normalisation peut uniquement passer une tâche `PLANNED` passée en `OVERDUE` ; elle ne modifie ni configuration, ni date, ni quota, ni progression d'entraînement, ni curseur.

## Quotas et temps réel

Les quotas sont dimensionnés sur le temps réellement disponible et sur une vitesse distincte pour Sabqi, Itqān et Murājaʿah.

- Les vitesses Sabqi et Itqān ne sont pas inventées : tant qu'elles ne sont pas mesurées, le moteur ne fabrique pas de valeur de remplacement.
- Les vitesses observées font partie de la configuration persistante du parcours.
- La capacité calculée reste fractionnaire en équivalent-page ; une capacité partielle peut ensuite être traduite en portion exacte sans arrondir silencieusement à une page supplémentaire.
- Pour Murājaʿah seulement, la référence initiale acceptée est `1 juz ≈ 20 pages ≈ 45 min`, soit 2,25 min/page. Cette référence est remplacée/ajustée par la vitesse observée dès qu'elle existe.

## Curseur coranique Hifz

Le curseur du Parcours Hifz n'est plus une chaîne de texte libre. Il est typé et contient :

- verset de début (`sourate`, `āyah`) ;
- verset de fin (`sourate`, `āyah`) ;
- page Muṣḥaf de début ;
- page Muṣḥaf de fin.

Les références sont validées sur les 114 sourates et les 6236 āyāt canoniques. Les pages sont limitées au Muṣḥaf de Médine fixe de 604 pages. Une référence impossible, une plage inversée ou une page hors 1–604 est rejetée.

La cohérence `āyah ↔ page` est en plus vérifiée directement contre les éléments `ayahPolygon` des SVG du Muṣḥaf de Médine embarqué. Un couple individuellement valide mais incohérent est refusé. Une page Muṣḥaf absente ou illisible fait échouer la lecture/écriture de l'état Hifz de manière fermée.

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

Murājaʿah couvre à la fois le Sabqi récent et l'Itqān consolidé, sans ratio fixe entre les deux.

Sa composition doit pouvoir s'adapter aux facteurs déjà retenus :

- volume disponible de Sabqi / Itqān ;
- âge / dernière révision ;
- fragilité ;
- erreurs ;
- vitesse observée ;
- temps réellement disponible.

Aucune pondération numérique ou ordre lexicographique arbitraire entre ces facteurs n'est figé dans le socle tant qu'il n'a pas été défini. Le moteur de fondation conserve ces entrées et accepte un ordre adaptatif explicite sans injecter lui-même un ratio Sabqi/Itqān.

Une session Murājaʿah utilise une récitation par page/portion et la correction reste locale au passage concerné. Le moteur de masquage Sabqi/Itqān n'est donc pas réutilisé artificiellement pour Murājaʿah : Murājaʿah possède sa politique de récitation propre.

## Persistance

Le Parcours Hifz utilise un stockage séparé (`hifz_01010`) avec schéma versionné **3**.

Sont persistés séparément du lecteur libre :

- configuration du parcours ;
- bornes Sabqi et Itqān ;
- vitesses observées séparées ;
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

Toute corruption du stockage Hifz doit échouer de manière fermée et ne doit pas écraser automatiquement l'état par un état vide. Les anciens états de schéma 1 ou 2 ne sont pas interprétés silencieusement par le schéma 3.
