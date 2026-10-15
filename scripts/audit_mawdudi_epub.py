#!/usr/bin/env python3
"""Read a privately supplied OCR EPUB; emit metadata and counts, never prose.

Recognized labels are NOT a proof of commentary completeness. Missing labels
are an editorial queue, not an automatic finding of missing Quranic verses.
No inferred label, title, note, or commentary is written back to the source.
"""
import argparse
from collections import Counter
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path, PurePosixPath
import posixpath
import re
import xml.etree.ElementTree as ET
from zipfile import ZipFile


class BodyText(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_body = False
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag == 'body':
            self.in_body = True

    def handle_endtag(self, tag):
        if tag == 'body':
            self.in_body = False

    def handle_data(self, data):
        if self.in_body:
            self.parts.append(data)


def detect_heading(text):
    match = re.match(r'(\d{1,3})[.,]?\s+(?:Sura(?:h)?\s+|(?=An Nahl\b))', text, re.I)
    return int(match[1]) if match else None


def audit_region(surah, expected, pages):
    labels = []
    invalid = []
    for text, _ in pages:
        for match in re.finditer(r'(?:\((\d{1,3}):(\d{1,3})\)|\[(\d{1,3}):(\d{1,3})\])', text):
            source_surah, ayah = map(int, match.groups()[:2] if match[1] else match.groups()[2:])
            if source_surah != surah:
                continue
            if 1 <= ayah <= expected:
                labels.append(ayah)
            else:
                invalid.append(ayah)
    missing = sorted(set(range(1, expected + 1)) - set(labels))
    duplicates = {str(k): v for k, v in sorted(Counter(labels).items()) if v > 1}
    return {
        'surah': surah, 'expected_ayahs': expected,
        'first_member': pages[0][1], 'last_member': pages[-1][1],
        'recognized_verse_labels': len(set(labels)),
        'unresolved_verse_labels': missing,
        'duplicate_verse_labels': duplicates,
        'invalid_verse_labels': invalid,
        'label_status': 'REQUIRES_REVIEW' if missing or invalid or duplicates else 'LABELS_PRESENT_NOT_TEXT_VALIDATED',
        'editorial_status': 'NON VERIFIE',
    }


def load_pages(path):
    ns = {'c': 'urn:oasis:names:tc:opendocument:xmlns:container', 'o': 'http://www.idpf.org/2007/opf'}
    with ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError('EPUB CRC check failed')
        container = ET.fromstring(archive.read('META-INF/container.xml'))
        opf_name = container.find('c:rootfiles/c:rootfile', ns).get('full-path')
        opf = ET.fromstring(archive.read(opf_name))
        manifest = {x.get('id'): x.get('href') for x in opf.findall('o:manifest/o:item', ns)}
        pages = []
        seen = set()
        issues = []
        for item in opf.findall('o:spine/o:itemref', ns):
            if item.get('idref') not in manifest:
                issues.append('UNDEFINED_SPINE_ID:' + str(item.get('idref')))
                continue
            member = posixpath.normpath(str(PurePosixPath(opf_name).parent / manifest[item.get('idref')]))
            if not re.fullmatch(r'EPUB/page_[0-9]+\.html', member):
                continue
            if member in seen:
                raise ValueError('Duplicate page in EPUB spine')
            seen.add(member)
            body = BodyText()
            body.feed(archive.read(member).decode('utf-8'))
            pages.append((re.sub(r'\s+', ' ', ' '.join(body.parts)).strip(), member))
        numbered = {n for n in archive.namelist() if re.fullmatch(r'EPUB/page_[0-9]+\.html', n)}
        if seen != numbered:
            raise ValueError('EPUB spine does not enumerate all numbered page members')
        dc = '{http://purl.org/dc/elements/1.1/}'
        metadata = opf.find('o:metadata', ns)
        bibliographic_fields_present = {k: bool(metadata.findall(dc + k)) for k in ['title', 'creator', 'publisher', 'language', 'rights', 'source', 'date']}
        return pages, bibliographic_fields_present, len(archive.namelist()), issues


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--epub', type=Path, required=True)
    parser.add_argument('--canonical-bounds', type=Path, default=Path('app/src/main/java/com/quranunlock/guard/HifzCursor.kt'))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    bounds = args.canonical_bounds.read_text()
    match = re.search(r'ayahsPerSurah = intArrayOf\((.*?)\)', bounds, re.S)
    counts = list(map(int, re.findall(r'\d+', match[1])))
    if len(counts) != 114 or sum(counts) != 6236:
        raise ValueError('Unrecognized canonical bounds')
    pages, fields, members, issues = load_pages(args.epub)
    headings = [(detect_heading(text), i) for i, (text, _) in enumerate(pages) if detect_heading(text) is not None]
    if [s for s, _ in headings] != list(range(1, 115)):
        raise ValueError('114 ordered source openings not all recognized; no inferred headings permitted')
    rows = []
    for j, (surah, start) in enumerate(headings):
        end = headings[j + 1][1] if j + 1 < len(headings) else len(pages)
        rows.append(audit_region(surah, counts[surah - 1], pages[start:end]))
    report = {
        'schema': 'mawdudi-epub-intake-v1',
        'source_sha256': hashlib.sha256(args.epub.read_bytes()).hexdigest(),
        'source_bytes': args.epub.stat().st_size,
        'canonical_bounds_sha256': hashlib.sha256(args.canonical_bounds.read_bytes()).hexdigest(),
        'zip_crc': 'PASS', 'zip_members': members, 'numbered_pages_in_spine': len(pages),
        'epub_structure_issues': issues,
        'surah_headings_recognized': len(headings),
        'bibliographic_fields_present': fields,
        'expected_ayahs': sum(counts),
        'recognized_verse_labels': sum(r['recognized_verse_labels'] for r in rows),
        'unresolved_verse_labels': sum(len(r['unresolved_verse_labels']) for r in rows),
        'surahs_requiring_label_review': sum(r['label_status'] == 'REQUIRES_REVIEW' for r in rows),
        'editorially_validated_surahs': 0,
        'warning': 'Label recognition is neither text validation nor proof of complete commentary, notes, titles, or 556 web sections.',
        'surahs': rows,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k != 'surahs'}, indent=2))


if __name__ == '__main__':
    main()
