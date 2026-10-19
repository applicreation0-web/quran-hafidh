#!/usr/bin/env python3
"""Permit only three exact read-only Sabqi UI insertions over the frozen source.

The frozen Git blob is the complete pre-integration HifzSessionActivity. Removing
only the approved byte sequences must recover it byte for byte; no engine,
clock, preferences, credit, lifecycle, selection or renderer changes are allowed.
"""
from pathlib import Path
import hashlib
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PATH = 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java'
FROZEN_BLOB = '4d52f8bc8e5cbd7a5bfa836b4c73c07c52647f55'
INSERTIONS = (
    (b'    private LinearLayout actions, audioHost;\n',
     b'    private LinearLayout actions, audioHost;\n'
     b'    private LinearLayout correspondenceActions;\n'),
    (b'        controlBar.addView(actions);\n',
     b'        controlBar.addView(actions);\n'
     b'        correspondenceActions = Ui.row(this);\n'
     b'        correspondenceActions.setVisibility(View.GONE);\n'
     b'        controlBar.addView(correspondenceActions);\n'),
    (b'        mushaf.show(currentPage,currentSelection,currentLineIds,displayedMask(),contextFocus);\n',
     b'        mushaf.show(currentPage,currentSelection,currentLineIds,displayedMask(),contextFocus);\n'
     b'        CorrespondenceEntryPoints.populateSabqi(this, correspondenceActions, mode,\n'
     b'            currentSelection, currentPage, displayedMask(), currentLineIds);\n'),
)


def frozen_source():
    frozen = subprocess.check_output(['git', 'cat-file', 'blob', FROZEN_BLOB], cwd=ROOT)
    identity = hashlib.sha1(b'blob ' + str(len(frozen)).encode('ascii') + b'\0' + frozen).hexdigest()
    assert identity == FROZEN_BLOB, 'Frozen HifzSessionActivity blob identity mismatch'
    return frozen


def verify_source(actual):
    frozen = frozen_source()
    stripped = actual
    for old, approved in INSERTIONS:
        assert frozen.count(old) == 1, 'Ambiguous frozen Sabqi UI anchor'
        assert stripped.count(approved) == 1, 'Missing or modified exact Sabqi UI insertion'
        stripped = stripped.replace(approved, old, 1)
    assert stripped == frozen, 'Unapproved HifzSessionActivity delta outside exact Sabqi UI insertions'


def main():
    verify_source((ROOT / PATH).read_bytes())
    print('PASS: exact read-only Sabqi UI insertions; complete frozen session source recovered byte for byte')


if __name__ == '__main__':
    main()
