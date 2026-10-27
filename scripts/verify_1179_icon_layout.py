#!/usr/bin/env python3
"""The user's correction freezes sizes, placement and artwork outside the calendar."""
from pathlib import Path
import subprocess,re
root=Path(__file__).resolve().parents[1]
path='hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java'
base=subprocess.check_output(['git','show','ae68ee1a:'+path],cwd=root,text=True)
now=(root/path).read_text()
def method(src,name):
    m=re.search(r'(?m)^    (?:@Override )?(?:private|protected|public) [^\n]*\b'+name+r'\(',src);assert m,name
    mask=re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda x:' '*len(x.group()),src,flags=re.S)
    i=mask.index('{',m.start())+1;depth=1
    while depth:depth+=(mask[i]=='{')-(mask[i]=='}');i+=1
    return src[m.start():i]
for name in ['page','scrollOf','todayLine','largeIconFor','wideBoox','pathSectionTitle','booxIconDp','primaryTile','pathTile','pathSubtitle']:
    assert method(now,name)==method(base,name),'Icon/layout contract changed: '+name
for start,end in [('// 1.17.5 CP1:','// --- Parcours Hifz:'),('        LinearLayout path = page();','        dashboardTitle = Ui.bookText')]:
    assert now[now.index(start):now.index(end)]==base[base.index(start):base.index(end)],'Home or six-mode placement changed'
print('PASS: original Home and six Hifz icon sizes, positioning, artwork and layout byte-identical to pre-refactor 1.17.8')
