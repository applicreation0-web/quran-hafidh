# Quran Safeguard 0.10.10 — Contrat du socle Parcours Hifz

Ce document fixe les décisions déjà validées pour éviter qu'elles soient perdues ou réinterprétées pendant l'implémentation.

## Séparation des modes

- `Mémorisation` reste le mode libre et ponctuel hérité de 0.10.9.
- `Parcours Hifz` est un orchestrateur distinct avec sa propre persistance.
- Une session libre de Mémorisation ne modifie jamais le calendrier Hifz.
- Le moteur de lecture/mémorisation peut servir de surface d'exécution d'une tâche Hifz, mais il n'est jamais la source de vérité du planning Hifz.
- Les fichiers de préférences sont explicitement distincts : `reader109` / `free_quran_reader` pour le lecteur libre, `hifz_01010` pour le parcours structuré.

## Bornes et intervalles du parcours

La référence officielle du Parcours Hifz est toujours coranique : **verset X → verset Y**.

Le réglage rare conserve séparément :

- un intervalle Sabqi en sourate + verset ;
- une **liste ordonnée d'intervalles Itqān** en sourate + verset.

Exemple valide :

- Itqān 1 : Al-Baqarah `2:1 → 2:286` ;
- Itqān 2 : Al-Ḥujurāt `49:1 → An-Nās 114:6`.

Le trou entre les intervalles reste hors Itqān. Safeguard ne fusionne jamais silencieusement deux intervalles séparés. Les intervalles Itqān sont ordonnés, non chevauchants et persistés individuellement.

Sabqi et Itqān restent indépendants : aucune borne de l'un ne déplace implicitement les bornes de l'autre.

## Versets, pages et lignes

Ordre de référence obligatoire :

1. **verset X → verset Y** = cible canonique et unité officielle de progression ;
2. **ligne A → ligne B** = sous-découpage pédagogique interne seulement si nécessaire.

La page du Muṣḥaf est une surface d'affichage, pas une frontière pédagogique. Si une sourate commence au milieu d'une page, la page entière reste affichée sans reflow, mais seuls les versets de la cible sont travaillés, masqués, comptés et validés.

Pour une āyah très longue, Safeguard peut la diviser temporairement par lignes. Exemple : une āyah de 15 lignes peut être travaillée en `1–5`, `6–10`, `11–15`, puis assemblée. Toutes ces sous-étapes gardent **la même cible canonique de verset**. Aucune tranche de lignes ne peut être enregistrée seule comme verset acquis ; le curseur ne progresse qu'après validation du passage canonique complet.

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
- La capacité calculée reste fractionnaire en équivalent-page ; une capacité partielle doit être traduite en passage exact en versets, jamais en fausse validation d'une fraction de verset.
- Pour Murājaʿah seulement, la référence initiale acceptée est `1 juz ≈ 20 pages ≈ 45 min`, soit 2,25 min/page. Cette référence est remplacée/ajustée par la vitesse observée dès qu'elle existe.

## Curseur coranique Hifz

Le curseur est typé et contient :

- verset de début (`sourate`, `āyah`) ;
- verset de fin (`sourate`, `āyah`) ;
- page Muṣḥaf de début ;
- page Muṣḥaf de fin.

Les références sont validées sur les 114 sourates et les 6236 āyāt canoniques. Les pages sont limitées au Muṣḥaf de Médine fixe de 604 pages.

La cohérence `āyah ↔ page` est vérifiée directement contre les éléments `ayahPolygon` des SVG du Muṣḥaf de Médine embarqué. Un couple individuellement valide mais incohérent est refusé. Une page Muṣḥaf absente ou illisible fait échouer la lecture/écriture de l'état Hifz de manière fermée.

## Sabqi

Le Sabqi utilise l'audio et le masquage progressif : écoute passive, écoute active, texte visible, masquage 25 %, 50 %, 75 %, 100 %, puis test final entièrement masqué.

Un long verset peut être travaillé en segments de lignes, mais ces segments ne déplacent jamais le curseur canonique. Le passage complet doit être validé avant progression.

## Itqān

L'Itqān consolide un passage déjà appris, y compris lorsqu'il commence ou finit au milieu d'une page.

Contrat d'entraînement actuel : texte visible ×30, puis masquage 25 %, 50 %, 75 %, 100 %, puis test final entièrement masqué. L'audio n'est pas obligatoire. Chaque palier masqué exige au moins une réussite non assistée avant passage au suivant ; une tentative incorrecte ne valide jamais le palier.

La partie de page située hors cible peut rester visible pour préserver le Muṣḥaf intact, mais elle est hors exercice et ne compte jamais comme Itqān travaillé.

## Révélations / aide

- Une révélation est comptabilisée.
- Une révélation ne compte jamais comme répétition réussie.
- Elle remet à zéro la série de réussites consécutives de l'étape.
- La première tentative après révélation ne peut pas prolonger artificiellement une série de réussites.
- Une étape assistée ne peut pas être validée tant qu'une tentative non assistée conforme n'a pas repris le contrôle.

## Murājaʿah

Murājaʿah couvre à la fois le Sabqi récent et l'Itqān consolidé, sans ratio fixe entre les deux.

Sa composition doit pouvoir s'adapter au volume, à l'âge de dernière révision, à la fragilité, aux erreurs, à la vitesse observée et au temps réellement disponible. Aucune pondération arbitraire n'est figée tant qu'elle n'a pas été définie.

Une session Murājaʿah utilise une récitation par page/portion et la correction reste locale au passage concerné.

## Persistance

Le Parcours Hifz utilise un stockage séparé (`hifz_01010`) avec schéma versionné **4**.

Sont persistés séparément du lecteur libre : configuration, intervalle Sabqi, liste ordonnée des intervalles Itqān, vitesses observées, tâches, piste, dates, curseur canonique, quota, statut et progression d'entraînement.

Les lignes pédagogiques ne remplacent pas le curseur canonique et ne deviennent pas une unité d'acquisition autonome.

Toute corruption du stockage Hifz échoue de manière fermée et n'écrase pas automatiquement l'état. Les anciens états de schéma 1, 2 ou 3 ne sont pas interprétés silencieusement par le schéma 4.
