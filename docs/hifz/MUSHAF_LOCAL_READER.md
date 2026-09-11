# Quran Hifz — lecteur Mushaf local natif

## Flux

`NNN.svg.br` est lu exclusivement depuis les assets de Quran Hifz. `MushafRepository` valide la plage 1..604, décompresse Brotli en mémoire avec une limite de taille, puis `MushafSvgValidator` refuse les constructions SVG actives ou externes. `MushafRenderer` parse ensuite le SVG avec AndroidSVG et dessine directement sur un `Canvas` Android.

Il n'y a ni WebView, ni JavaScript, ni faux domaine local, ni interception d'URL, ni fallback réseau.

## Source canonique

- Hafs 'an 'Asim / KFQC.
- Source amont : `quranpedia/quran-svg`.
- Commit figé : `1b427fab77aae1403fe7e1f0b8c794a5384d5605`.
- Arbre `svg-br` figé : `fe267b844a17750e57fbe2dd5e9c8ca81d59c8f9`.

Le script `scripts/vendor_hifz_mushaf.py` est volontairement un outil de préparation locale. L'application installée ne télécharge rien.

## Test local par étapes

1. `python3 scripts/vendor_hifz_mushaf.py --smoke`
2. construire/installler `:hifz-app` localement ;
3. vérifier page 1 ;
4. page suivante vers 2 ;
5. page précédente vers 1 ;
6. exécuter le test instrumenté `LocalMushafAssetInstrumentedTest` ;
7. seulement ensuite vendoriser les 604 pages avec `python3 scripts/vendor_hifz_mushaf.py` ;
8. exécuter `python3 scripts/verify_hifz_mushaf_assets.py` avant de brancher géométrie/Hifz/BOOX.

Aucun GitHub Actions n'est requis pour cette boucle de débogage.
