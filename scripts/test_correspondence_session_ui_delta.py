#!/usr/bin/env python3
"""Source safety contract for the read-only Sabqi entry, no Android runtime needed."""
from pathlib import Path
import importlib.util
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java'
CALL = ('        CorrespondenceEntryPoints.populateSabqi(this, correspondenceActions, mode,\n'
        '            currentSelection, currentPage, displayedMask(), currentLineIds);\n')


class SessionEntrySourceTest(unittest.TestCase):
    def test_host_is_dedicated_and_initially_hidden(self):
        source = SOURCE.read_text()
        self.assertTrue('    private LinearLayout correspondenceActions;\n' in source, 'dedicated host missing')
        self.assertTrue(('        correspondenceActions = Ui.row(this);\n'
                      '        correspondenceActions.setVisibility(View.GONE);\n'
                      '        controlBar.addView(correspondenceActions);\n') in source, 'hidden controls-row host missing')

    def test_refresh_reads_the_rendered_current_state(self):
        source = SOURCE.read_text()
        self.assertTrue(('        mushaf.show(currentPage,currentSelection,currentLineIds,displayedMask(),contextFocus);\n'
                      + CALL + '    }') in source, 'refresh after rendered current state missing')
        self.assertEqual(1, source.count('CorrespondenceEntryPoints.populateSabqi('))

    def test_exact_delta_and_no_session_writes_in_added_blocks(self):
        path = ROOT / 'scripts/verify_correspondence_session_ui_delta.py'
        self.assertTrue(path.is_file(), 'exact Sabqi UI delta verifier is required')
        spec = importlib.util.spec_from_file_location('sabqi_delta', path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        source = SOURCE.read_bytes()
        module.verify_source(source)
        for addition in module.INSERTIONS:
            block = addition[1].replace(addition[0], b'', 1)
            for forbidden in (b'prefs.', b'metricsStore.', b'speedStore.', b'sessionClock',
                              b'startActivity(', b'currentMask=', b'currentSelection=',
                              b'currentLineIds=', b'currentPage=', b'commit(', b'complete'):
                self.assertNotIn(forbidden, block)
        with self.assertRaises(AssertionError):
            module.verify_source(source + b'\n')
        with self.assertRaises(AssertionError):
            module.verify_source(source.replace(b'hasShown=true;', b'hasShown=false;', 1))
        for old, approved in module.INSERTIONS:
            with self.assertRaises(AssertionError):
                module.verify_source(source.replace(approved, old, 1))


if __name__ == '__main__':
    unittest.main()
