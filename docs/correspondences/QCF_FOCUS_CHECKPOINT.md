# CP04A — exact verse focus, staged display adapter

Latest mandate: 18 October 2026, eight relation types. This is one implementation unit, not completion of CP04 or approval of a relation.

`CorrespondenceSvgFocus` appends an SVG mask and a gray overlay using only the authentic page's existing `ayahPolygon` path data. Original glyphs and transforms are unchanged. Focus may contain multiple physical path components. A separately selected neighboring verse remains normal; it is never gray or hatched. On a continuation page containing only that neighbor, the neighbor remains readable and the rest fades. If neither focus nor neighbor geometry exists on the page, the source is returned intact. Non-QCF coordinates and stacking transformed pages are rejected.

There is deliberately no centre/word marking API. R033 remains visually unvalidated; whole-verse geometry cannot establish its central segment.

Validation: eight JUnit behavior tests pass under the local ECJ/JRE17 harness, following an initial missing-feature RED. A separate, non-native rasterization of authentic QCF page529 (54:17 focus, 54:18 normal) was inspected on paper background. Its transparent SVG background must be composited over the viewer's paper background; transparency by itself is not an opaque black neighbor. This preview is not an Android capture or BOOX test.

Not yet wired into a screen. Two-panel legibility, Android/WebView SVG masks, page transitions, complete dialog navigation and device behavior remain NON_EXECUTE. No approved relation catalogue, source text, APK or acquired Hifz state was added. Atlas renderer unchanged. The two engine freeze scripts allow only this exact additional display adapter filename; protected engines remain excluded.
