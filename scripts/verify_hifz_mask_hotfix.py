#!/usr/bin/env python3
from pathlib import Path

reader = Path('hifz-app/src/main/assets/hifzreader/reader.js').read_text()

# Regression seen on-device in 0.7.1: one min/max bounding rectangle per line masks
# whitespace and adjacent lines merge into a huge slab. The corrected renderer must
# mask actual source-ink groups from right to left and allow a partial boundary cell.
assert 'function hiddenSegmentsForLine' in reader, 'missing per-ink mask segment algorithm'
assert 'function hiddenBandForLine' not in reader, 'single bounding band would recreate the slab regression'
assert 'const totalWidth=' in reader and 'const targetWidth=' in reader, 'mask percentage must be width based'
assert 'remaining' in reader and 'Math.min(cellWidth,remaining)' in reader, 'boundary cell must be clipped to exact remaining width'
assert 'segments.forEach' in reader, 'renderer must draw independent mask segments'
assert 'line.top+0.6' in reader and '(line.bottom-line.top)-1.2' in reader, 'mask rows need a small vertical separation to prevent merging'
print('HIFZ_MASK_HOTFIX_CONTRACT_OK')
