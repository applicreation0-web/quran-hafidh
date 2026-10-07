# Carte sémantique — plan du pilote

**But :** atlas indépendant sourate → axe documenté → sections → Mushaf réel.
**Base et branche :** voir l'audit technique ; aucune écriture hors branche autorisée.
**Spécification :** mission utilisateur du 7 octobre 2026, sections 0–27.

## Étapes et conditions de passage

- [x] Vérifier HEAD, ascendance, version et arbre propre ; auditer les dépendances.
- [x] Retrouver les volumes arabes et les articles anglais ; distinguer scans/OCR.
- [ ] Constituer les dix sourates, avec références imprimées et contrôle visuel.
- [ ] Vérifier les traductions publiées titre par titre ; sinon traduction, comparaison
  terminologique, rétrotraduction et contrôle de sens consignés séparément.
- [ ] Valider le corpus complet avant toute activation UI.
- [ ] Écrire les tests de bornes, erreurs et versets débordant sur deux pages ;
  implémenter `scripts/semantic_map/validate.py`, lecture seule des données.
- [ ] Implémenter une Activity privée et le corpus `semantic-map/` seulement après
  validation scientifique. Liste pilote, axe, sections ; états manquants explicites.
- [ ] Vérifier un contexte de préférences isolé pour MushafView, y compris application
  context et migrations. Si cette isolation échoue : arrêter l'intégration.
- [ ] Ajouter une ligne d'accueil et une déclaration Manifest, aucune autre modification
  de fichier existant ; réemploi des pages SVG sans toucher au renderer.
- [ ] Navigation section précédente / carte / section suivante ; page conservée à la
  rotation ; bornes début/fin distinctes ; aucun état Hifz ni activité chronométrée.
- [ ] Exécuter JVM, contrats, compilation debug puis tests visuels téléphone/tablette.
- [ ] Publier le diff et les limites matérielles, puis décision au checkpoint pilote.

## Tests de risque prioritaires

Corpus invalide fermé ; NULL jamais converti en texte savant ; doublons détectés ;
trous/chevauchements signalés sans réparation ; niveaux hiérarchiques distincts ;
dernière page d'un verset débordant conservée ; préférences Hifz inchangées après
ouverture/navigation/rotation ; retour Carte ne lance ni quiz ni session.

## Méthode d'exécution

Exécution dans cette session, sans agents délégués. Tests du validateur avant
implémentation ; points d'arrêt scientifiques effectifs, sans les contourner avec
des titres générés. La recherche documentaire récupérée n'est pas une validation.
