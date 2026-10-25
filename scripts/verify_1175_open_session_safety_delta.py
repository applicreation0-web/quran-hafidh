#!/usr/bin/env python3
"""Allow precisely two reviewed method changes. Every initialization, default,
backup codec, migration, mask and all other production bytes must stay exact."""
from pathlib import Path
import subprocess
BASE = '9049d620c6be88e3ae45bf2d5d79c6538e000a49'
ROOT = 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/'
for file, start, end in [
    ('HifzPrefs.java', '    boolean persistConsolidationSession(', '    /** Restore the exact frozen OPEN grouped session'),
    ('HifzSessionActivity.java', '    private void renderGroupedCycle(', '    /**\n     * A multi-page unit'),
]:
    path = ROOT + file
    old = subprocess.check_output(['git','show',BASE+':'+path],text=True)
    new = Path(path).read_text()
    a, b = new.index(start), new.index(end, new.index(start))
    x, y = old.index(start), old.index(end, old.index(start))
    assert new[:a] + old[x:y] + new[b:] == old, 'Unexpected production delta outside reviewed OPEN safety method: '+file
print('PASS: only persistence conflict guards and non-destructive OPEN rendering changed; all initializations/migrations/masks/backups byte-identical')
