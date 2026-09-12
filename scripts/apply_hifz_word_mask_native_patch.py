#!/usr/bin/env python3
from pathlib import Path

path = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java')
text = path.read_text(encoding='utf-8')
replacements = [
    (
        '        if(currentMask!=oldMask)mushaf.setMask(currentMask);',
        '        mushaf.setMask(currentMask, maskDrawKey());'
    ),
    (
        '        currentMask=PreviewConfig.itqanMaskForNextRep(rep);if(currentMask!=oldMask)mushaf.setMask(currentMask);',
        '        currentMask=PreviewConfig.itqanMaskForNextRep(rep);mushaf.setMask(currentMask, maskDrawKey());'
    ),
    (
        '    @Override public void onPageSwipe(int delta){goPage(delta);}\n    private void showCurrent(){hasShown=true;mushaf.show(currentPage,currentSelection,currentLineIds,currentMask);}',
        '    @Override public void onPageSwipe(int delta){goPage(delta);}\n'
        '    private String maskDrawKey(){\n'
        '        if(SABQI.equals(mode))return "SABQI|rep:"+prefs.sabqiRep();\n'
        '        if(ITQAN.equals(mode))return "ITQAN|rep:"+prefs.itqanRep();\n'
        '        return "MURAJAAH";\n'
        '    }\n'
        '    private void showCurrent(){hasShown=true;mushaf.show(currentPage,currentSelection,currentLineIds,currentMask,maskDrawKey());}'
    ),
]
for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'expected exactly one occurrence, found {count}: {old[:80]}')
    text = text.replace(old, new)
path.write_text(text, encoding='utf-8')
print('HIFZ_NATIVE_WORD_MASK_PATCH_OK')
