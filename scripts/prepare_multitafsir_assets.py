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
    'qushayri': 'f83d181815a2dd19cc3f598048d503022c6d2827ce353149c03797f63158b6d2',
    'qurtubi': '4f3e890085f8d991818d7b3fe9280d534adcf2242ac1694c9cc985aa842ea87e',
}
ARCHIVE_SHA = {
    'qushayri': 'e38c894b5fe20bb10c8d3a12b5de8c1fc1ed2173709c7ab491faac1cda79f552',
    'qurtubi': 'fff869e3504affa565cfbfaf321a22f4ad47f6108da87bc0ebfa287084a7cad9',
}
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

def package(name: str, db: Path, assets: Path) -> None:
    require_sha(name+' database', db, DB_SHA[name])
    compressed=gzip.compress(db.read_bytes(), compresslevel=9, mtime=0)
    archive_sha=hashlib.sha256(compressed).hexdigest()
    if archive_sha != ARCHIVE_SHA[name]:
        raise SystemExit(f'{name} deterministic archive SHA mismatch: {archive_sha}')
    encoded=base64.b64encode(compressed).decode('ascii')
    for old in assets.glob(f'{name}_en.sqlite.gz.b64.part*'): old.unlink()
    parts=[encoded[i:i+PART_CHARS] for i in range(0,len(encoded),PART_CHARS)]
    for i,part in enumerate(parts):
        (assets/f'{name}_en.sqlite.gz.b64.part{i:02d}').write_text(part,encoding='ascii')
    print(f'{name}: DB {db.stat().st_size} bytes, {len(parts)} packaged part(s), SHA verified')

def main() -> None:
    ap=argparse.ArgumentParser()
    ap.add_argument('--source-dir', type=Path, required=True)
    ap.add_argument('--assets-dir', type=Path, required=True)
    ap.add_argument('--work-dir', type=Path, required=True)
    args=ap.parse_args()
    source=args.source_dir.resolve(); assets=args.assets_dir.resolve(); work=args.work_dir.resolve()
    assets.mkdir(parents=True,exist_ok=True); work.mkdir(parents=True,exist_ok=True)
    qsh=source/'lataif.pdf'
    qpaths=[source/f'qurtubi-v{i}.pdf' for i in range(1,5)]
    require_sha('Qushayri source',qsh,SOURCE_SHA['qushayri'])
    for i,p in enumerate(qpaths,1): require_sha(f'Qurtubi volume {i}',p,SOURCE_SHA[f'qurtubi-v{i}'])
    qsh_db=work/'qushayri_en.sqlite'; qur_db=work/'qurtubi_en.sqlite'
    env=os.environ.copy(); env.update(QUSHAYRI_PDF=str(qsh),QUSHAYRI_OUT=str(qsh_db))
    subprocess.run([sys.executable,str(ROOT/'scripts/build_qushayri.py')],env=env,check=True)
    env=os.environ.copy(); env.update(QURTUBI_BASE=str(source),QURTUBI_OUT=str(qur_db))
    subprocess.run([sys.executable,str(ROOT/'scripts/build_qurtubi.py')],env=env,check=True)
    package('qushayri',qsh_db,assets); package('qurtubi',qur_db,assets)
    print('Pinned private multi-tafsir assets prepared successfully; source PDFs were not copied into assets.')

if __name__=='__main__': main()
