"""Freeze the source corpus, text rendering and protection engine during UI work."""
from pathlib import Path
import subprocess
BASE = '23bf3b56319bf6fc745509248bd84f27d8f49cc9'
allowed = {
 'app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt',
 'app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt',
 'app/src/light/java/com/quranunlock/guard/TafsirEdition.kt',
 'app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt',
 'app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt',
 'docs/SOBER_UX_0.10.8_FOLLOWUP.md',
 'scripts/test_reader_verse_gestures.cjs',
 'scripts/verify_sober_ux_boundary.py',
 '.github/workflows/sober-reader-ux.yml',
}
changed = set(subprocess.check_output(['git', 'diff', '--name-only', BASE, 'HEAD'], text=True).splitlines())
assert changed <= allowed, f'Unexpected edits: {changed - allowed}'
p = 'app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt'
before = subprocess.check_output(['git', 'show', f'{BASE}:{p}'], text=True)
after = Path(p).read_text()
anchor = 'private fun runsToAnnotatedString('
assert before[before.index(anchor):] == after[after.index(anchor):], 'Source text rendering changed'
for anchor, end in [('    val blocks = remember(runs)', '                onClick = { offset ->')]:
 assert before[before.index(anchor):before.index(end, before.index(anchor))] == after[after.index(anchor):after.index(end, after.index(anchor))], 'Source block styling changed'
for path in Path('app/src').rglob('*.kt'):
 text = path.read_text()
 for token in ['FLAG_FULLSCREEN', 'SYSTEM_UI_FLAG_FULLSCREEN', 'hide(WindowInsets.Type.statusBars', 'hide(WindowInsetsCompat.Type.statusBars']:
  assert token not in text, f'Status-bar hiding found: {path}'
print('PASS: unchanged corpus, source rendering, protection and counters; no status-bar hiding')
