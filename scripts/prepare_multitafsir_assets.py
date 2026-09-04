#!/usr/bin/env python3
"""Build the two private English tafsir DBs from pinned source PDFs and package assets deterministically."""
from __future__ import annotations
import argparse, base64, gzip, hashlib, os, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SHA = {
    'qushayri': 'f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3',
    'qurtubi-v1': 'a791ec1313fa2401abe7ca25ac7ccb4bedb1afcb51f2c779160a71e98a6f04cb',
    'qurtubi-v2': '466e72af70ad6c9c9ddccb418f87df6012c3078ef7947fdc88ab00c86c15645e',
    'qurtubi-v3': 'e69818ce49f79d7de2bb5cef37c82e7e1f4431f7117a550fa33259da7dc6b583',
    'qurtubi-v4': 'eb71cb2ed8c2497cc8a5d3634b3eeb7788fdc7caee9de5d6b50349fb8619965c',
}
DB_SHA = {
    'qushayri': '4356e836e8e14818f6b4f5007eeac12454cb388e6aa59759a926bdc560569c5b',
    'qurtubi': '4f3e890085f8d991818d7b3fe9280d534adcf2242ac1694c9cc985aa842ea87e',
}
ARCHIVE_SHA = {
    'qushayri': '56b1e78ad6e8fea5302b6ca9773f65b8ba48cb3f1cf9475ed8b57b93f9309a68',
    'qurtubi': 'fff869e3504affa565cfbfaf321a22f4ad47f6108da87bc0ebfa287084a7cad9',
}
EXPECTED_PARTS = {'qushayri': 1, 'qurtubi': 4}
PART_CHARS = 500_000

def sha(path: Path) -> str:
    h=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024), b''): h.update(chunk)
    return h.hexdigest()

def require_sha(label: str, path: Path, expected: str) -> None:
    if not path.is_file(): raise SystemExit(f'Missing pinned source: {path}')
    actual=sha(path)
    if actual != expected: raise SystemExit(f'{label} SHA-256 mismatch: {actual}')

def prepare_package(name: str, db: Path) -> list[str]:
    require_sha(name+' database', db, DB_SHA[name])
    compressed=gzip.compress(db.read_bytes(), compresslevel=9, mtime=0)
    archive_sha=hashlib.sha256(compressed).hexdigest()
    if archive_sha != ARCHIVE_SHA[name]:
        raise SystemExit(f'{name} deterministic archive SHA mismatch: {archive_sha}')
    encoded=base64.b64encode(compressed).decode('ascii')
    parts=[encoded[i:i+PART_CHARS] for i in range(0,len(encoded),PART_CHARS)]
    if len(parts) != EXPECTED_PARTS[name]:
        raise SystemExit(f'{name} packaged part count changed: {len(parts)} != {EXPECTED_PARTS[name]}')
    return parts

def publish_packages(payloads: dict[str,list[str]], assets: Path, staging: Path) -> None:
    staged=staging/'asset-stage'
    if staged.exists():
        for old in staged.iterdir(): old.unlink()
    else:
        staged.mkdir(parents=True)

    expected_names=set()
    for name,parts in payloads.items():
        for i,part in enumerate(parts):
            filename=f'{name}_en.sqlite.gz.b64.part{i:02d}'
            expected_names.add(filename)
            (staged/filename).write_text(part,encoding='ascii')

    # No final asset is touched until both DBs, both archives and every staged
    # part have been verified successfully.
    for name in payloads:
        for old in assets.glob(f'{name}_en.sqlite.gz.b64.part*'): old.unlink()
    for filename in sorted(expected_names):
        os.replace(staged/filename, assets/filename)

    actual_names={p.name for name in payloads for p in assets.glob(f'{name}_en.sqlite.gz.b64.part*')}
    if actual_names != expected_names:
        raise SystemExit(f'Published tafsir asset set mismatch: {sorted(actual_names)}')

def main() -> None:
    ap=argparse.ArgumentParser()
    ap.add_argument('--source-dir', type=Path, required=True)
    ap.add_argument('--assets-dir', type=Path, required=True)
    ap.add_argument('--work-dir', type=Path, required=True)
    args=ap.parse_args()
    source=args.source_dir.resolve(); assets=args.assets_dir.resolve(); work=args.work_dir.resolve()
    assets.mkdir(parents=True,exist_ok=True); work.mkdir(parents=True,exist_ok=True)
    leaked_pdfs=list(assets.rglob('*.pdf'))
    if leaked_pdfs:
        raise SystemExit(f'Source PDF already present in Plus assets: {leaked_pdfs[:5]}')
    qsh=source/'lataif.pdf'
    qpaths=[source/f'qurtubi-v{i}.pdf' for i in range(1,5)]
    require_sha('Qushayri source',qsh,SOURCE_SHA['qushayri'])
    for i,p in enumerate(qpaths,1): require_sha(f'Qurtubi volume {i}',p,SOURCE_SHA[f'qurtubi-v{i}'])
    qsh_db=work/'qushayri_en.sqlite'; qur_db=work/'qurtubi_en.sqlite'
    env=os.environ.copy(); env.update(QUSHAYRI_PDF=str(qsh),QUSHAYRI_OUT=str(qsh_db))
    subprocess.run([sys.executable,str(ROOT/'scripts/build_qushayri.py')],env=env,check=True)
    env=os.environ.copy(); env.update(QURTUBI_BASE=str(source),QURTUBI_OUT=str(qur_db))
    subprocess.run([sys.executable,str(ROOT/'scripts/build_qurtubi.py')],env=env,check=True)

    # Prepare and verify both payloads first; only then publish either one.
    payloads={
        'qushayri': prepare_package('qushayri',qsh_db),
        'qurtubi': prepare_package('qurtubi',qur_db),
    }
    publish_packages(payloads,assets,work)
    for name,parts in payloads.items():
        print(f'{name}: {len(parts)} packaged part(s), DB/archive SHA verified')
    print('Pinned private multi-tafsir assets prepared successfully; source PDFs were not copied into assets.')

if __name__=='__main__': main()
