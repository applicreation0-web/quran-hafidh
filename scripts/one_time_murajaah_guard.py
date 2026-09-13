from pathlib import Path

path = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java')
s = path.read_text()
old = '''    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        LinkedHashSet<String> ids = new LinkedHashSet<>();
'''
new = '''    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        if (MurajaahTraversalPolicy.endpointAmbiguous(murajaahPlan.traversalVerses, through)) return 0;
        LinkedHashSet<String> ids = new LinkedHashSet<>();
'''
if s.count(old) != 1:
    raise SystemExit(f'countMurajaahLinesThrough guard expected one match, got {s.count(old)}')
path.write_text(s.replace(old, new, 1))
