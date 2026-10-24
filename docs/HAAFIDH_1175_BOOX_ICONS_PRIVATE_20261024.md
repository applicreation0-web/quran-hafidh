# Private BOOX icon integration – 2026-10-24

Source: 12 raster-drawn pictograms from the conversation's Quran Haafidh icon grid, traced automatically to Android VectorDrawable XML. These are **not original artist-authored vector paths**. The reconstruction preserves coarse silhouettes and 2-tone appearance but may change small fine details.

Code integration:
- Only MainActivity uses the new large icon mapping (Accueil header, primary destinations, progression, six Hifz path actions).
- The 23 frozen small action icons in Ui.iconFor are untouched.
- No change to Hifz scheduling, attribution, verses, corpus, storage, or signing.
- No more small compact home rows; large icon sizing remains driven by booxIconDp(3,112) and booxIconDp(2,124).
- Removed black ColorFilter from large ImageViews and compound Drawable tint from Hifz main tiles so grey shapes remain visible.

Release gate: PRIVATE. Android Gradle compilation, source contracts, emulator startup, and physical BOOX visual QA are REQUIRED and **not implied by this commit**. Existing CI failures at parent commit must be resolved separately before a release.
