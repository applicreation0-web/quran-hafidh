#!/usr/bin/env python3
"""0.10.6 wrapper around the exhaustive Plus APK tafsir audit.

Qushayri's corrected transport uses two base64 asset parts and restores only the
honorifics proved by the pinned edition legend. The legacy corpus/coverage gates
remain unchanged; this wrapper adds an edition-scoped Arabic allow-list plus an
exact final honorific inventory so the APK verifier stays fail-closed.
"""
from __future__ import annotations

import base64
import collections
import gzip
import re
import sqlite3
import tempfile

import audit_0106_tafsir_honorifics as honorifics
import verify_plus_apk_multitafsir as legacy

legacy.V2['qushayri']['parts'] = [
    f'assets/tafsir/qushayri_en.sqlite.gz.b64.part{i:02d}' for i in range(2)
]

_original_audit_v2 = legacy.audit_v2


def _verify_qushayri_honorifics_in_apk(z, spec) -> None:
    encoded = b''.join(z.read(path) for path in spec['parts'])
    try:
        compressed = base64.b64decode(encoded, validate=True)
        db = gzip.decompress(compressed)
    except Exception as exc:
        raise SystemExit(f'Qushayri honorific payload decode failed: {exc}')

    tmp = tempfile.NamedTemporaryFile(suffix='.sqlite')
    tmp.write(db)
    tmp.flush()
    con = sqlite3.connect(f'file:{tmp.name}?mode=ro', uri=True)
    try:
        meta = dict(con.execute('SELECT key,value FROM source_metadata ORDER BY key'))
        if meta.get('source_honorific_glyph_policy') != 'restore-edition-pua-to-full-source-honorifics-no-name-inference':
            raise SystemExit(f"Qushayri honorific policy drifted: {meta.get('source_honorific_glyph_policy')!r}")
        if meta.get('source_honorific_legend_page') != 'xxvi':
            raise SystemExit('Qushayri source legend page metadata missing')
        if meta.get('source_honorific_revision') != '0106-source-authoritative-v1':
            raise SystemExit('Qushayri source honorific revision metadata missing')

        payload = '\n'.join(
            value or ''
            for row in con.execute(
                'SELECT verse_translation,commentary FROM tafsir_entry ORDER BY id'
            )
            for value in row
        )
        if honorifics.PUA.search(payload) or '\ufffd' in payload:
            raise SystemExit('Qushayri PUA/replacement glyph survived in Plus APK')
        for token in honorifics.QUSHAYRI_OLD_SHORTHAND:
            if token in payload:
                raise SystemExit(f'Qushayri obsolete honorific shorthand survived in Plus APK: {token}')

        counts = honorifics.non_overlapping_counts(payload)
        actual = {
            mark: counts.get(mark, 0)
            for mark in honorifics.QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS
        }
        if actual != honorifics.QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS:
            raise SystemExit(f'Qushayri final honorific inventory changed in Plus APK: {actual!r}')
        if sum(actual.values()) != honorifics.QUSHAYRI_EXPECTED_PAYLOAD_MARK_TOTAL:
            raise SystemExit('Qushayri final honorific total changed in Plus APK')
        unexpected = set(counts) - set(honorifics.QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS)
        if unexpected:
            raise SystemExit(f'Qushayri unexpected honorific spelling(s) in Plus APK: {sorted(unexpected)!r}')

        residual = payload
        for mark in sorted(honorifics.QUSHAYRI_ALLOWED_MARKS, key=len, reverse=True):
            residual = residual.replace(mark, '')
        match = legacy.ARABIC.search(residual)
        if match:
            sample = residual[max(0, match.start()-24):match.end()+24].replace('\n', ' ')
            raise SystemExit(f'Qushayri Arabic outside closed source-honorific allow-list in Plus APK: {sample!r}')
    finally:
        con.close()
        tmp.close()


def _audit_v2_0106(z, names, edition, spec):
    if edition != 'qushayri':
        return _original_audit_v2(z, names, edition, spec)

    # The inherited verifier still rejects every Arabic code point. For
    # Qushayri only, replace that regex with the same closed allow-list used by
    # the source audit, then restore it immediately before Qurtubi is checked.
    original_arabic = legacy.ARABIC
    legacy.ARABIC = honorifics.QushayriArabicAllowList()
    try:
        digest = _original_audit_v2(z, names, edition, spec)
    finally:
        legacy.ARABIC = original_arabic

    _verify_qushayri_honorifics_in_apk(z, spec)
    return digest


legacy.audit_v2 = _audit_v2_0106

if __name__ == '__main__':
    legacy.main()
