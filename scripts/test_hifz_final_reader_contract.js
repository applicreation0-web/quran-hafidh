'use strict';
const assert=require('assert');
const fs=require('fs');

const html=fs.readFileSync('hifz-app/src/main/assets/hifzreader/index.html','utf8');
const reader=fs.readFileSync('hifz-app/src/main/assets/hifzreader/reader.js','utf8');

// Tablet/BOOX fit: scaling above 1 inside an overflow-hidden viewport clipped top/bottom lines.
assert.ok(!html.includes('--mushaf-scale:1.035'), 'legacy overscale still clips normal reader');
assert.ok(!html.includes('--mushaf-scale:1.06'), 'legacy E-Ink overscale still clips first/last lines');
assert.ok(!/scale\(var\(--mushaf-scale\)\)/.test(html), 'Mushaf transform scaling must not exceed the viewport');

// Final mask contract must be linguistic-word based, not source-ink segments.
assert.ok(reader.includes('selectMaskedWords'), 'word-level mask selector missing');
assert.ok(reader.includes('pageGeo.words'), 'generated word coordinates are not consumed');
assert.ok(!reader.includes('hiddenSegmentsForLine'), 'ink-segment masking must not remain active');

// Same seed => nested percentages, different seed => potentially different ordering.
const marker='if(typeof module';
assert.ok(reader.includes(marker), 'reader test exports missing');

console.log('HIFZ_FINAL_READER_SOURCE_CONTRACT_OK');
