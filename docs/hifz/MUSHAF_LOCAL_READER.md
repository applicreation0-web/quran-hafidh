# Quran Hifz — lecteur Mushaf local natif

## Flux runtime

`NNN.svg.br` est lu exclusivement depuis les assets de Quran Hifz. `MushafRepository` valide la plage 1..604, décompresse Brotli en mémoire avec une limite de taille, puis `MushafSvgValidator` refuse les constructions SVG actives ou externes. `MushafRenderer` parse ensuite le SVG avec AndroidSVG et dessine directement sur un `Canvas` Android.

Il n'y a ni WebView, ni JavaScript, ni faux domaine local, ni interception d'URL, ni fallback réseau.

## Source canonique figée

- Hafs 'an 'Asim / KFQC.
- Source amont : `quranpedia/quran-svg`.
- Sous-module Git local : `third_party/quran-svg`.
- Commit figé : `1b427fab77aae1403fe7e1f0b8c794a5384d5605`.
- Corpus attendu : exactement `001.svg.br` à `604.svg.br`.

Chaque build Hifz exécute `verifyHifzMushafSource` avant la copie des assets. Le build échoue si le sous-module est absent, pointe sur un autre commit, contient des modifications suivies ou ne présente pas exactement les 604 pages KFQC attendues. Le sous-module sert uniquement de source de build locale et reproductible ; l'application installée ne télécharge rien.

## Premier gate local : 001 → 002 → 001

1. `git submodule update --init --recursive`
2. `./gradlew :hifz-app:verifyHifzMushafSource`
3. `./gradlew :hifz-app:assembleDebug -PhifzSmokeMushaf=true`
4. installer l'APK localement sur l'appareil de test ;
5. vérifier l'affichage natif de la page 1 ;
6. naviguer vers la page 2 ;
7. revenir à la page 1 ;
8. lancer les tests instrumentés avec le même mode smoke ;
9. répéter rapidement 1 → 2 → 1 pour vérifier qu'un ancien chargement asynchrone ne reprend jamais la main.

En mode smoke, seules 001 et 002 sont empaquetées dans l'APK de test, même si le sous-module local contient les 604 pages.

## Critères GO du gate

Le gate 001 → 002 → 001 est GO uniquement si : page 1 rendue sans WebView, page 2 rendue, retour page 1 correct, aucune page distante sollicitée, aucune reprise d'un chargement périmé, aucun crash/ANR et `LocalMushafAssetInstrumentedTest` + `MainActivityMushafNavigationInstrumentedTest` réussissent sur l'environnement Android local.

## Après validation du gate

- build normal sans `-PhifzSmokeMushaf=true` : empaquetage des 604 pages locales ;
- contrôle de navigation multi-pages, mémoire et stabilité ;
- seulement ensuite branchement de la géométrie ;
- ensuite seulement interactions Hifz et optimisation BOOX/E-Ink.

`scripts/vendor_hifz_mushaf.py` reste disponible pour produire une copie vendored des assets à partir du même sous-module piné. Il n'effectue aucun téléchargement.

Aucun GitHub Actions n'est requis pour cette boucle de débogage.
