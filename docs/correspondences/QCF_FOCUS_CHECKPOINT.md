# CP04A — exact verse focus, staged display adapter

Latest mandate: 18 October 2026, eight relation types. This is one implementation unit, not completion of CP04 or approval of a relation.

`CorrespondenceSvgFocus` appends an SVG mask and a gray overlay using only the authentic page's existing `ayahPolygon` path data. Original glyphs and transforms are unchanged. Focus may contain multiple physical path components. A separately selected neighboring verse remains normal; it is never gray or hatched. On a continuation page containing only that neighbor, the neighbor remains readable and the rest fades. If neither focus nor neighbor geometry exists on the page, the source is returned intact. Non-QCF coordinates and stacking transformed pages are rejected.

There is deliberately no centre/word marking API. R033 remains visually unvalidated; whole-verse geometry cannot establish its central segment.

Validation: eight JUnit behavior tests pass under the local ECJ/JRE17 harness, following an initial missing-feature RED. A separate, non-native rasterization of authentic QCF page529 (54:17 focus, 54:18 normal) was inspected on paper background. Its transparent SVG background must be composited over the viewer's paper background; transparency by itself is not an opaque black neighbor. This preview is not an Android capture or BOOX test.

Not yet wired into a screen. Two-panel legibility, Android/WebView SVG masks, page transitions, complete dialog navigation and device behavior remain NON_EXECUTE. No approved relation catalogue, source text, APK or acquired Hifz state was added. Atlas renderer unchanged. The two engine freeze scripts allow only this exact additional display adapter filename; protected engines remain excluded.


## CP04A — read-only model and independent review (19 October)

The new `CorrespondenceModel` supplies exactly eight types, direct approved-anchor lookup, five external approval gates, immutable original bounds, two-page/series/internal modes and a five-position rail window. CENTRE requires INTERNAL plus an attested segment backed by exact geometry; an ayah-only centre cannot become displayable. No real approved relation corpus is supplied.

An independent code review found a series viewport bug: the next verse on the next physical page was rejected. A targeted regression reproduced the failure. The corrected viewport adds only display context; originals and indexed anchors remain unchanged. Reader and pane snapshots now retain scrollX/scrollY alongside page, zoom and selection. The model's 21 JUnit tests pass. The earlier eight SVG tests remain green. Whole-application return behavior, Android page/zoom/pan, lifecycle restoration and BOOX remain unverified; a pure snapshot is not native UI evidence.


## CP04B — authentic surface and isolated dialog

The staged screen uses two authentic local QCF SVG folios for ordinary pairs, retains each surface when enlarging, and uses one folio plus a maximum-five Atlas-style rail for series. Internal structures use one folio. The source text, Atlas and Hifz engines are not modified. Brotili decoding is off the main thread with generation/cancellation guards; the WebViews disable JavaScript, file/content access and network loads. Paper backgrounds preserve transparent SVG glyph surfaces. No production entry point or approved corpus is provided.

`CorrespondenceGeometry` reads the existing line and exact word sidecars. Seven tests cover real 6236-verse/77432-word data and missing/malformed segments. No actual ayah continuation is present in this Madinah index; a future continuation is tested only with an explicitly synthetic fixture.

Seven document tests cover focus/neighbor context and refusals of fake centre or noncontiguous highlights. Pure focus/model/document tests total36; geometry adds7. Local harnesses use Android/dependency stubs, so Android compilation is still pending new-head CI. Native gesture/zoom/pan, half-page legibility, BOOX physical behavior, centre hatching and lifecycle restoration are NON_EXECUTE. Source gates and geometric existence are not scientific certification.
