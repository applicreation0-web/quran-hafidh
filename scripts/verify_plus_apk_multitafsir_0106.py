#!/usr/bin/env python3
"""0.10.6 wrapper around the existing exhaustive Plus APK tafsir audit.

Only the corrected Qushayri transport split changes from 0.10.5: the same
logical SQLite corpus now occupies two base64 asset parts. All inherited corpus,
coverage, contamination and Qurtubi provenance gates remain unchanged.
"""
from __future__ import annotations

import verify_plus_apk_multitafsir as legacy

legacy.V2['qushayri']['parts'] = [
    f'assets/tafsir/qushayri_en.sqlite.gz.b64.part{i:02d}' for i in range(2)
]

if __name__ == '__main__':
    legacy.main()
