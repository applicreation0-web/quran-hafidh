# Quran Hifz — Construction intensive 6 h

Branche de travail: `work/hifz-6h-build`
Base: `recode/quran-hifz-local-first`

## Règle
Aucune nouvelle fonctionnalité. Les références Tarteel, Quran for Android, Daily Quran, KOReader et OnyxAndroidDemo servent uniquement à fiabiliser l'implémentation existante.

## Ordre d'exécution
1. Renderer Mushaf: ratio strict, fit-center, chargement local, cache borné.
2. Géométrie: page/ligne/verset, transformation déterministe vers l'écran.
3. BOOX E-Ink: couche isolée, refresh partiel/REGAL, GC uniquement quand nécessaire.
4. Lecture/Étude: navigation RTL, reprise page, commandes discrètes.
5. Tafsir: Lecture/Étude uniquement, jamais en Mémorisation.
6. Mémorisation libre: 5 lignes, focus, masquage/révélation, persistance.
7. Parcours Hifz: Sabqi, Itqan ×30, Murajaah, planning et report souple.
8. Tests: 604 pages, persistance, kill/restart, navigation rapide, collisions de gestes.
9. Audit contradictoire, correction des blockers, build APK, SHA-256.

## Gates
- G1: 604 pages locales vérifiées et rendu sans déformation.
- G2: géométrie stable et tap/masquage cohérents.
- G3: comportement E-Ink sans animations inutiles ni ghosting excessif.
- G4: séparation stricte Lecture/Tafsir/Mémorisation.
- G5: état Hifz persistant et compteurs exacts.
- G6: stress tests et audit indépendants verts.
