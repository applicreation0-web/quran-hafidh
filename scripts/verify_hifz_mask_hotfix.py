#!/usr/bin/env python3
from pathlib import Path

reader = Path('hifz-app/src/main/assets/hifzreader/reader.js').read_text()

# Regression seen on-device in 0.7.1: one min/max bounding rectangle per line masks
# whitespace and adjacent lines merge into a huge slab. The final renderer randomizes
# existing source-ink cells, measures the requested percentage by ink width, clips only
# the boundary cell, and redraws verse-number rosettes above the masks. The rosette
# fallback also receives the masked line geometry so numbers remain visible when a
# line-mask is rendered without ayah selection polygons.
assert 'function randomOrderKeys' in reader, 'missing non-deterministic cell ordering'
assert 'function seededRandom' in reader and 'maskEntropy' in reader, 'random draw must survive WebView page reloads within one session'
assert 'function randomSegmentsForCells' in reader, 'missing source-ink segment renderer'
assert 'function hiddenBandForLine' not in reader, 'single bounding band would recreate the slab regression'
assert 'totalWidth*fraction' in reader, 'mask percentage must be source-ink-width based'
assert 'Math.min(cellWidth,remaining)' in reader, 'boundary source-ink cell must be clipped to exact remaining width'
assert 'cell.top+0.6' in reader and '(cell.bottom-cell.top)-1.2' in reader, 'mask rows need vertical separation to prevent merging'
assert 'markerLayer(svg,polys,lines)' in reader, 'verse-number rosette layer must receive line geometry fallback'
assert 'layer.appendChild(markerLayer(svg,polys,lines))' in reader, 'verse-number rosettes must always be redrawn above masks'
assert 'line.words' not in reader and 'maskedWordIds' not in reader, 'linguistic word mapping must not leak into final renderer'
print('HIFZ_RANDOM_MASK_SLAB_REGRESSION_OK')
