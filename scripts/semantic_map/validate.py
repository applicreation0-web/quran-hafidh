"""Read-only corpus checks. Findings never repair scientific source data."""
import argparse
import json
from pathlib import Path

STATUSES = {'PUBLISHED_EXACT', 'PUBLISHED_PARAPHRASE',
            'EDITORIAL_VERIFIED', 'SOURCE_UNRESOLVED'}


def validate(data, geometry):
    findings = []
    seen_surahs = set()
    seen_ids = set()

    def flag(code, path):
        findings.append({'code': code, 'path': path})

    def text_fields(row, path, axis=False):
        ar, en, status, source = (('axisAr', 'axisEn', 'axisEnglishStatus', 'axisSource')
                                  if axis else ('titleAr', 'titleEn', 'englishStatus', 'source'))
        if not isinstance(row.get(ar), str) or not row[ar].strip():
            flag('MISSING_ARABIC', path)
        ref = row.get(source)
        if not isinstance(ref, dict) or not ref.get('volume') or not ref.get('page'):
            flag('MISSING_SOURCE', path)
        if row.get(status) not in STATUSES:
            flag('INVALID_ENGLISH_STATUS', path)
        elif row[status] == 'SOURCE_UNRESOLVED':
            flag('UNRESOLVED_ENGLISH', path)
        if not isinstance(row.get(en), str) or not row[en].strip():
            flag('MISSING_ENGLISH', path)
        if row.get(status) in {'PUBLISHED_EXACT', 'PUBLISHED_PARAPHRASE'}:
            if not row.get('axisEnglishReference' if axis else 'englishReference'):
                flag('MISSING_ENGLISH_REFERENCE', path)

    for index, surah in enumerate(data.get('surahs', [])):
        path = f'surahs[{index}]'
        number = surah.get('number')
        if type(number) is not int or not 1 <= number <= 114:
            flag('INVALID_SURAH', path)
            continue
        if number in seen_surahs:
            flag('DUPLICATE_SURAH', path)
        seen_surahs.add(number)
        count = max((int(k.split(':')[1]) for k in geometry if k.startswith(f'{number}:')), default=0)
        text_fields(surah, path, axis=True)
        sections = surah.get('sections') or []
        if not sections:
            flag('NO_SECTIONS', path)
        covered = set()
        previous_start = 0
        ranges = set()
        for pos, section in enumerate(sections):
            sp = f'{path}.sections[{pos}]'
            sid = section.get('id')
            if not isinstance(sid, str) or not sid:
                flag('MISSING_ID', sp)
            elif sid in seen_ids:
                flag('DUPLICATE_ID', sp)
            seen_ids.add(sid)
            text_fields(section, sp)
            start, end = section.get('startAyah'), section.get('endAyah')
            if type(start) is not int or type(end) is not int or not (1 <= start <= count and 1 <= end <= count):
                flag('INVALID_AYAH', sp)
                continue
            if start > end:
                flag('REVERSED_BOUNDS', sp)
                continue
            if start < previous_start:
                flag('UNORDERED', sp)
            previous_start = start
            if (start, end) in ranges:
                flag('DUPLICATE_RANGE', sp)
            ranges.add((start, end))
            current = set(range(start, end + 1))
            if current & covered:
                flag('OVERLAP', sp)
            # Report an ordering gap even if an out-of-order section fills it later.
            if start > (max(covered, default=0) + 1):
                flag('GAP', sp)
            covered |= current
            first = geometry.get(f'{number}:{start}', [])
            last = geometry.get(f'{number}:{end}', [])
            if not first or not last:
                flag('MISSING_GEOMETRY', sp)
            elif section.get('startPage') != min(first) or section.get('endPage') != max(last):
                flag('PAGE_MAPPING', sp)
        if covered != set(range(1, count + 1)):
            flag('GAP', path)
    return findings


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('corpus', type=Path)
    parser.add_argument('geometry', type=Path)
    args = parser.parse_args()
    data = json.loads(args.corpus.read_text())
    geometry = json.loads(args.geometry.read_text())['verses']
    findings = validate(data, geometry)
    print(json.dumps({'schema': 1, 'publishable': not findings,
                      'findings': findings}, ensure_ascii=False, indent=2))
    return 1 if findings else 0


if __name__ == '__main__':
    raise SystemExit(main())
