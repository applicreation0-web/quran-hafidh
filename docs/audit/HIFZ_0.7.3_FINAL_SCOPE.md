# Quran Hifz 0.7.3 final scope

Frozen base: `release/hifz-0.7.2-official` at `0d1e94779b0a85eda0cfe189272788efddfde9e1`.

Only two product changes are authorized in this final pass:

1. BOOX/tablet reader must fit the complete Mushaf viewport with no top/bottom cropping. The first and last Quran lines must remain fully visible; reveal may still apply a temporary minimal translation when Tafsir genuinely obscures a selected verse.
2. Memorisation masking becomes true word-level masking using pinned Madinah-Mushaf word coordinates. The random ordering is deterministic for one memorisation unit so 25% is a subset of 50%, 50% of 75%, and 100% hides all eligible words. A new unit/cycle gets a new ordering. Verse-number rosettes and decorative marks are excluded.

Everything else is frozen: 604 Mushaf SVG payloads, Tafsir payloads/UI, audio pack handling, Sabqi/Itqan/Murajaah rules and counts, free-memorisation selection semantics, persistence, navigation, iconography, package id and durable signing identity.

External coordinate source is pinned to `bodoorzahera/Quran-coordinate@ed24b7fbf60a052ac58e694d5728ab4c4d59f96d` (MIT). Its coordinate data is transformed into the generated read-only geometry asset; it does not replace or reflow the shipped Mushaf SVG.
