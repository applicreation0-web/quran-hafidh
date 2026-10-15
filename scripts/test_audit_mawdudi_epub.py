"""Synthetic inputs only: no third-party commentary in the public test suite."""
import unittest
import tempfile
from pathlib import Path
from zipfile import ZipFile
from audit_mawdudi_epub import audit_region, detect_heading, load_pages


class StructuralAuditTests(unittest.TestCase):
    def test_square_brackets_are_valid_labels(self):
        result = audit_region(9, 3, [('[9:1] A [9:2] B [9:3] C', 'a.html')])
        self.assertEqual(result['recognized_verse_labels'], 3)
        self.assertEqual(result['unresolved_verse_labels'], [])

    def test_foreign_surah_citation_cannot_fill_a_gap(self):
        result = audit_region(2, 2, [('(2:1) A (3:2) B', 'a.html')])
        self.assertEqual(result['unresolved_verse_labels'], [2])

    def test_malformed_label_is_never_silently_repaired(self):
        result = audit_region(2, 2, [('(2:1) A (2;2) B', 'a.html')])
        self.assertEqual(result['unresolved_verse_labels'], [2])

    def test_out_of_bounds_label_is_reported(self):
        result = audit_region(2, 2, [('(2:1) A (2:2) B (2:287) C', 'a.html')])
        self.assertEqual(result['invalid_verse_labels'], [287])

    def test_duplicate_label_cannot_inflate_coverage(self):
        result = audit_region(2, 2, [('(2:1) A (2:1) B', 'a.html')])
        self.assertEqual(result['recognized_verse_labels'], 1)
        self.assertEqual(result['duplicate_verse_labels'], {'1': 2})
        self.assertEqual(result['unresolved_verse_labels'], [2])

    def test_perfect_labels_do_not_mean_editorial_pass(self):
        result = audit_region(1, 1, [('(1:1) corrupted prose', 'a.html')])
        self.assertEqual(result['editorial_status'], 'NON VERIFIE')
        self.assertEqual(result['label_status'], 'LABELS_PRESENT_NOT_TEXT_VALIDATED')

    def test_ocr_heading_variants_are_inventory_only(self):
        for text, number in [('44, Surah Example', 44), ('92. Sura Example', 92), ('16. An Nahl (The Honey Bee)', 16)]:
            self.assertEqual(detect_heading(text), number)
        self.assertIsNone(detect_heading('85. See Surah Example'))

    def test_undefined_cover_is_recorded_without_inventing_source_content(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'source.epub'
            with ZipFile(path, 'w') as z:
                z.writestr('META-INF/container.xml', '<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="EPUB/content.opf"/></rootfiles></container>')
                z.writestr('EPUB/content.opf', '<package xmlns="http://www.idpf.org/2007/opf"><metadata/><manifest><item id="a" href="page_2.html"/><item id="b" href="page_10.html"/></manifest><spine><itemref idref="cover"/><itemref idref="a"/><itemref idref="b"/></spine></package>')
                z.writestr('EPUB/page_2.html', '<html><head><title>Page</title></head><body>Second</body></html>')
                z.writestr('EPUB/page_10.html', '<html><body>Tenth</body></html>')
            pages, _, _, issues = load_pages(path)
            self.assertEqual(issues, ['UNDEFINED_SPINE_ID:cover'])
            self.assertEqual([text for text, _ in pages], ['Second', 'Tenth'])


if __name__ == '__main__':
    unittest.main()
