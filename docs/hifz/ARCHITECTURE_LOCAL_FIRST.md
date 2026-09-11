# Quran Hifz — architecture locale-first

## FIGÉ

- Quran Hifz est une application distincte de Quran Safeguard.
- Aucun blocage d'applications, aucune Accessibilité Safeguard et aucune détection runtime smartphone/liseuse dans Quran Hifz.
- Runtime local-first : Mushaf, géométrie, Tafsir et futur audio sont lus depuis des données locales.
- Aucun pseudo-réseau, aucun faux hôte HTTPS et aucun WebView pour le lecteur Mushaf.
- Mushaf de Médine Hafs/KFQC : cible canonique de 604 pages, sans reflow.
- Le Tafsir reste réservé au mode Lecture/Étude ; aucun tap Tafsir en Mémorisation.
- Un seul moteur Hifz orchestre Mémorisation libre, Sabqi, Itqan et Murajaah.
- Persistance simple et versionnée.
- Optimisations BOOX/E-Ink séparées du domaine et du rendu canonique.
- Diagnostic et tests locaux avant toute CI. Aucun GitHub Actions ne sert au débogage.

## PARAMÈTRES DE TRAVAIL

- `applicationId`: `com.quransafeguard.hifz`.
- `minSdk`: 26 ; `targetSdk`: 36 ; `compileSdk`: 37.
- AndroidSVG 1.4 pour le rendu SVG natif, Brotli decoder 0.1.2 pour `.svg.br`.
- Le moteur historique pur Kotlin de `hifz-core` reste temporairement l'implémentation derrière les façades `domain/*`; il sera découpé sans changer ses invariants testés.

## À VALIDER

- Import local et contrôle d'intégrité des 604 fichiers `001.svg.br` à `604.svg.br` dans le module Hifz.
- Géométrie locale complète dérivée du même corpus KFQC.
- Corpus Tafsir local définitif et son format de stockage.
- Profil BOOX Go 10.3 Gen II : stratégie de refresh/ghosting après validation du lecteur natif.
- Audio local ultérieur : format, packaging et volume de données.

## Arborescence cible

```text
Quran Hifz
├── data
│   ├── MushafRepository
│   ├── GeometryRepository
│   ├── TafsirRepository
│   └── AudioRepository
├── domain
│   ├── HifzEngine
│   ├── SabqiEngine
│   ├── ItqanEngine
│   ├── MurajaahEngine
│   └── HifzState
├── storage
│   └── HifzStateStore
├── reader
│   └── MushafRenderer
└── ui
```

## Premier chemin de preuve

```text
001.svg.br local
→ décompression Brotli
→ validation SVG stricte
→ rendu natif de la page 1
→ navigation page 2
→ retour page 1
```

Ce chemin doit être prouvé avant de brancher géométrie, Tafsir, moteur Hifz ou optimisation BOOX au lecteur.
