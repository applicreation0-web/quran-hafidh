#!/usr/bin/env python3
"""Build the private English tafsir DBs from pinned PDFs and stage APK assets.

SQLite file bytes are not a stable cross-platform serialization. Integrity is
checked at two layers: exact source-PDF SHA-256 and deterministic logical
content digests. 0.10.6 also includes Qushayri semantic runs in that logical
digest so poetry tagging cannot drift independently of the commentary text.
"""
from __future__ import annotations
import argparse, base64, gzip, hashlib, json, os, sqlite3, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SHA = {
    'qushayri': 'f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3',
    'qurtubi-v1': 'a791ec1313fa2401abe7ca25ac7ccb4bedb1afcb51f2c779160a71e98a6f04cb',
    'qurtubi-v2': '466e72af70ad6c9c9ddccb418f87df6012c3078ef7947fdc88ab00c86c15645e',
    'qurtubi-v3': 'e69818ce49f79d7de2bb5cef37c82e7e1f4431f7117a550fa33259da7dc6b583',
    'qurtubi-v4': 'eb71cb2ed8c2497cc8a5d3634b3eeb7788fdc7caee9de5d6b50349fb8619965c',
}
EXPECTED_ENTRIES = {'qushayri': 720, 'qurtubi': 432}
EXPECTED_PARTS = {'qushayri': 2, 'qurtubi': 4}
PRESENTATION_REVISION = {
    'qushayri': '0106-qushayri-source-semantics-v1',
    'qurtubi': '0106-qurtubi-hide-verse-labels-v1',
}
EXPECTED_QUSHAYRI_STRUCTURE = {
    'raw_segment_count': '806',
    'translation_only_anchor_count': '86',
    'grouped_source_range_count': '76',
    'source_soft_hyphen_count': '584',
    'soft_hyphen_policy': 'preserve-marker-then-source-driven-join',
    'source_honorific_glyph_policy': 'restore-edition-pua-to-full-source-honorifics-no-name-inference',
    'source_honorific_legend_page': 'xxvi',
    'source_honorific_revision': '0106-source-authoritative-v1',
    'verified_note_call_count': '928',
    'note_call_display_policy': 'remove-only-source-verified-unexposed-footnote-calls',
    'poetry_index_entry_count': '121',
    'poetry_index_occurrence_count': '126',
    'poetry_unique_line_count': '542',
    'poetry_semantics': 'source-poetry-index-v1',
    'paragraph_policy': 'source-geometry-no-pdf-block-breaks',
    'semantic_run_table': 'tafsir_run',
}
PART_CHARS = 500_000


def sha(path: Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def require_sha(label: str, path: Path, expected: str) -> None:
    if not path.is_file():
        raise SystemExit(f'Missing pinned source: {path}')
    actual = sha(path)
    if actual != expected:
        raise SystemExit(f'{label} SHA-256 mismatch: {actual}')


def stamp_presentation_revision(name: str, db: Path) -> None:
    con = sqlite3.connect(db)
    try:
        con.execute(
            'INSERT OR REPLACE INTO source_metadata(key,value) VALUES (?,?)',
            ('presentation_revision', PRESENTATION_REVISION[name]),
        )
        con.commit()
    finally:
        con.close()


def logical_digest(name: str, db: Path) -> str:
    """Digest database meaning, including Qushayri semantic presentation runs."""
    con = sqlite3.connect(f'file:{db}?mode=ro', uri=True)
    try:
        if con.execute('PRAGMA quick_check').fetchone()[0] != 'ok':
            raise SystemExit(f'{name} SQLite quick_check failed')
        meta = dict(con.execute('SELECT key,value FROM source_metadata ORDER BY key'))
        if meta.get('schema_version') != '2' or meta.get('edition_id') != name:
            raise SystemExit(f'{name} source metadata mismatch')
        if meta.get('arabic_included') != 'false':
            raise SystemExit(f'{name} must contain no Arabic source text outside source-restored honorifics')
        if meta.get('presentation_revision') != PRESENTATION_REVISION[name]:
            raise SystemExit(f'{name} 0.10.6 presentation revision mismatch')
        expected = EXPECTED_ENTRIES[name]
        if int(meta.get('entry_count', '-1')) != expected:
            raise SystemExit(f'{name} metadata entry count mismatch')
        if name == 'qushayri':
            for key, expected_value in EXPECTED_QUSHAYRI_STRUCTURE.items():
                if meta.get(key) != expected_value:
                    raise SystemExit(
                        f'qushayri source-structure metadata mismatch: '
                        f'{key}={meta.get(key)!r}, expected {expected_value!r}'
                    )
        if name == 'qurtubi' and meta.get('verse_marker_display_policy') != (
            'indexed-source-verse-labels-hidden-in-translation'
        ):
            raise SystemExit('qurtubi verse-marker display policy mismatch')

        columns = [row[1] for row in con.execute('PRAGMA table_info(tafsir_entry)')]
        rows = con.execute(
            'SELECT ' + ','.join(columns) + ' FROM tafsir_entry ORDER BY id'
        ).fetchall()
        if len(rows) != expected:
            raise SystemExit(f'{name} row count mismatch: {len(rows)} != {expected}')
        payload = {
            'metadata': sorted(meta.items()),
            'columns': columns,
            'rows': rows,
        }
        if name == 'qushayri':
            tables = {
                row[0]
                for row in con.execute(
                    "SELECT name FROM sqlite_master WHERE type='table'"
                )
            }
            if 'tafsir_run' not in tables:
                raise SystemExit('qushayri semantic tafsir_run table missing')
            run_columns = [row[1] for row in con.execute('PRAGMA table_info(tafsir_run)')]
            run_rows = con.execute(
                'SELECT ' + ','.join(run_columns) + ' FROM tafsir_run ORDER BY entry_id,run_no'
            ).fetchall()
            if not run_rows:
                raise SystemExit('qushayri semantic tafsir_run table empty')
            payload['semantic_run_columns'] = run_columns
            payload['semantic_runs'] = run_rows

        encoded = json.dumps(
            payload, ensure_ascii=False, separators=(',', ':'), sort_keys=True
        ).encode('utf-8')
        return hashlib.sha256(encoded).hexdigest()
    finally:
        con.close()


def prepare_package(name: str, db: Path) -> tuple[list[str], str]:
    digest = logical_digest(name, db)
    compressed = gzip.compress(db.read_bytes(), compresslevel=9, mtime=0)
    encoded = base64.b64encode(compressed).decode('ascii')
    parts = [encoded[i:i + PART_CHARS] for i in range(0, len(encoded), PART_CHARS)]
    if len(parts) != EXPECTED_PARTS[name]:
        raise SystemExit(
            f'{name} packaged part count changed: {len(parts)} != {EXPECTED_PARTS[name]}'
        )
    return parts, digest


def publish_packages(payloads: dict[str, list[str]], assets: Path, staging: Path) -> None:
    staged = staging / 'asset-stage'
    if staged.exists():
        for old in staged.iterdir():
            old.unlink()
    else:
        staged.mkdir(parents=True)

    expected_names = set()
    for name, parts in payloads.items():
        for i, part in enumerate(parts):
            filename = f'{name}_en.sqlite.gz.b64.part{i:02d}'
            expected_names.add(filename)
            (staged / filename).write_text(part, encoding='ascii')

    for name in payloads:
        for old in assets.glob(f'{name}_en.sqlite.gz.b64.part*'):
            old.unlink()
    for filename in sorted(expected_names):
        os.replace(staged / filename, assets / filename)

    actual_names = {
        p.name
        for name in payloads
        for p in assets.glob(f'{name}_en.sqlite.gz.b64.part*')
    }
    if actual_names != expected_names:
        raise SystemExit(f'Published tafsir asset set mismatch: {sorted(actual_names)}')


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--source-dir', type=Path, required=True)
    ap.add_argument('--assets-dir', type=Path, required=True)
    ap.add_argument('--work-dir', type=Path, required=True)
    args = ap.parse_args()
    source = args.source_dir.resolve()
    assets = args.assets_dir.resolve()
    work = args.work_dir.resolve()
    assets.mkdir(parents=True, exist_ok=True)
    work.mkdir(parents=True, exist_ok=True)

    leaked_pdfs = list(assets.rglob('*.pdf'))
    if leaked_pdfs:
        raise SystemExit(f'Source PDF already present in Plus assets: {leaked_pdfs[:5]}')

    qsh = source / 'lataif.pdf'
    qpaths = [source / f'qurtubi-v{i}.pdf' for i in range(1, 5)]
    require_sha('Qushayri source', qsh, SOURCE_SHA['qushayri'])
    for i, p in enumerate(qpaths, 1):
        require_sha(f'Qurtubi volume {i}', p, SOURCE_SHA[f'qurtubi-v{i}'])

    qsh_db = work / 'qushayri_en.sqlite'
    qur_db = work / 'qurtubi_en.sqlite'
    env = os.environ.copy()
    env.update(QUSHAYRI_PDF=str(qsh), QUSHAYRI_OUT=str(qsh_db))
    subprocess.run([sys.executable, str(ROOT / 'scripts/build_qushayri.py')], env=env, check=True)
    stamp_presentation_revision('qushayri', qsh_db)

    env = os.environ.copy()
    env.update(QURTUBI_BASE=str(source), QURTUBI_OUT=str(qur_db))
    subprocess.run([sys.executable, str(ROOT / 'scripts/build_qurtubi.py')], env=env, check=True)
    stamp_presentation_revision('qurtubi', qur_db)

    prepared = {
        'qushayri': prepare_package('qushayri', qsh_db),
        'qurtubi': prepare_package('qurtubi', qur_db),
    }
    payloads = {name: value[0] for name, value in prepared.items()}
    publish_packages(payloads, assets, work)
    for name, (parts, digest) in prepared.items():
        print(
            f'{name}: {len(parts)} packaged part(s); '
            f'logical_sha256={digest}; sqlite_bytes_sha256={sha(qsh_db if name == "qushayri" else qur_db)}'
        )
    print(
        'Pinned private multi-tafsir assets prepared successfully; '
        'source PDFs were not copied into assets.'
    )


if __name__ == '__main__':
    main()
