# Contrat fonctionnel — lecture cumulative masquée après finale Sabqi / Itqān

Décision utilisateur du 26 octobre 2026. **SPÉCIFICATION APPROUVÉE, NON ENCORE IMPLÉMENTÉE DANS L'APK OFFICIELLE 1.17.5 (versionCode 43).**

## Déroulé exact

1. Conserver intégralement la finale de la semaine courante, sa date historique éventuelle, son protocole de répétition et ses preuves.
2. Lors de la validation normale, inscrire les crédits `Acquis` et clore cette finale **avant** de commencer la lecture cumulative.
3. Ouvrir ensuite une **séance de lecture cumulative distincte et sans crédit**.
4. Corpus: **au maximum les 20 dernières pages précédemment acquises** dans le parcours concerné, **PLUS les pages de la semaine qui vient d'être validée**. Les pages sont uniques; une page du lot hebdomadaire ne peut pas compter également dans les 20 précédentes. Si moins de 20 pages précédentes sont admissibles, ne prendre que celles disponibles. Ne jamais ajouter de pages non apprises pour atteindre 20.
5. Parcourir le corpus **une fois dans l'ordre du parcours**, sans seconde boucle ni répétition par page, avec **texte masqué à 100 % et amorces exactes préservées** (géométrie QCF), y compris aux changements de page.
6. Le parcours de cette lecture se sauvegarde séparément (date/type, pages et lignes exactes, indice courant); reprise au bon endroit si fermeture, lendemain, interruption ou redémarrage.
7. Aucune écriture vers les crédits, files de lignes, initialisations, compteurs de finales, ou `sourceSnapshot` des séances historiques; la lecture cumulative n'est pas une condition préalable à la promotion `Acquis` déjà validée.
8. Avant An-Nās, cette règle s'applique aux finales Sabqi et Itqān. Après An-Nās, **aucune nouvelle finale Itqān**, mais une finale historique encore OPEN conserve sa logique originelle; toute lecture additionnelle ne peut jamais modifier son protocole gelé.
9. La version signée 1.17.5 existante ne contient pas cette étape supplémentaire. Ne pas déclarer cette fonction livrée avant tests Android et nouvelle signature officielle.

## Contrôles obligatoires

- 0, 1, 19, 20, >20 pages antérieures: bornage exact, sans gonflement ni doublon.
- Semaine courante sur page partielle ou frontière de sourate: seules lignes admises; aucun ajout de texte non acquis.
- Lecture coupée à mi-parcours, changement de date, reprise une semaine plus tard; aucune relecture des pages déjà effectuées.
- Finals Sabqi/Itqān indépendantes; dates historiques OPEN et originelles préservées.
- Passage après An-Nās ne crée ni Itqān finale ni crédit artificiel.
- Replay de validation / double-tap / crash après commit Acquis avant création de lecture: séquence idempotente.
- E-ink BOOX 100 % masque préparé avant premier affichage: absence de texte fantôme; amorces géométriques exactes.
- Lecture mensuelle potentiellement longue: aucun timeout qui valide fictivement le reste.
- Export/restauration v3/schéma6: lecture cumulative conservée, aucune migration destructive.
- APK nouveau code, 7/7 Atlas privés, même certificat officiel SHA256, code version de mise à jour, installation par-dessus 1.17.4/1.17.5.

## Portée

La logique existante de sélection des pages de finale n'est **pas** la nouvelle règle: elle exclut les blocs déjà acquis. La nouvelle révision cumulative est obligatoirement une **seconde étape post-validation**, jamais une modification du regroupement de la finale.
