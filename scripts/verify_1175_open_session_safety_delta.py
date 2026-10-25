#!/usr/bin/env python3
"""Allow only the reviewed OPEN persistence/rendering and final closure changes.
Every initialization, default,
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
    if file == 'HifzPrefs.java':
        # Replace the one reviewed closure span (including its new identity guard)
        # by the trusted version before checking the rest of the file exactly.
        old_start = old.index('    /** Atomically closes a frozen 1..3 physical-unit Consolidation group')
        new_start = new.index('    /** A stale Activity cannot close or remove a different persisted finale.')
        marker = '    public String lastLearningConsolidationDate()'
        old_end, new_end = old.index(marker), new.index(marker)
        new = new[:new_start] + old[old_start:old_end] + new[new_end:]
    a, b = new.index(start), new.index(end, new.index(start))
    x, y = old.index(start), old.index(end, old.index(start))
    assert new[:a] + old[x:y] + new[b:] == old, 'Unexpected production delta outside reviewed OPEN safety method: '+file
print('PASS: only reviewed OPEN persistence/rendering and finale closure guards changed; all initializations/migrations/masks/backups byte-identical')
