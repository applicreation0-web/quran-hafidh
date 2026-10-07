"""Synthetic fixtures only: these are not Quranic interpretations."""
import copy
import unittest
from validate import validate


class CorpusValidationTest(unittest.TestCase):
    def setUp(self):
        self.geometry = {'1:1': [1], '1:2': [1, 2], '1:3': [2]}
        self.section = dict(id='fixture', titleAr='اختبار', titleEn='Test',
                            englishStatus='EDITORIAL_VERIFIED', source={'volume': 1, 'page': 1},
                            startAyah=1, endAyah=3, startPage=1, endPage=2)
        self.data = {'surahs': [dict(number=1, axisAr='اختبار', axisEn='Test',
                      axisEnglishStatus='EDITORIAL_VERIFIED', axisSource={'volume': 1, 'page': 1},
                      sections=[self.section])]}

    def codes(self):
        return {x['code'] for x in validate(self.data, self.geometry)}

    def test_valid_fixture_is_accepted_and_never_mutated(self):
        original = copy.deepcopy(self.data)
        self.assertEqual(set(), self.codes())
        self.assertEqual(original, self.data)

    def test_invalid_surah_and_verse(self):
        self.data['surahs'][0]['number'] = 115
        self.assertIn('INVALID_SURAH', self.codes())
        self.data['surahs'][0]['number'] = 1
        self.section['endAyah'] = 4
        self.assertIn('INVALID_AYAH', self.codes())

    def test_reversed_bounds(self):
        self.section.update(startAyah=3, endAyah=1)
        self.assertIn('REVERSED_BOUNDS', self.codes())

    def test_split_verse_uses_last_page(self):
        self.section.update(endAyah=2, endPage=1)
        self.assertIn('PAGE_MAPPING', self.codes())
        self.section['endPage'] = 2
        self.assertNotIn('PAGE_MAPPING', self.codes())

    def test_gap_overlap_order_and_duplicate_are_signalled(self):
        a = copy.deepcopy(self.section)
        b = copy.deepcopy(self.section)
        a.update(startAyah=2, endAyah=3)
        b.update(startAyah=1, endAyah=2)
        self.data['surahs'][0]['sections'] = [a, b]
        self.assertTrue({'GAP', 'OVERLAP', 'UNORDERED', 'DUPLICATE_ID'} <= self.codes())

    def test_missing_text_source_and_status(self):
        self.section.update(titleAr=None, source=None, englishStatus=None)
        self.assertTrue({'MISSING_ARABIC', 'MISSING_SOURCE', 'INVALID_ENGLISH_STATUS'} <= self.codes())

    def test_unresolved_is_not_publishable(self):
        self.section.update(titleEn=None, englishStatus='SOURCE_UNRESOLVED')
        self.assertIn('UNRESOLVED_ENGLISH', self.codes())

    def test_duplicate_surah_and_empty_sections(self):
        self.data['surahs'].append(copy.deepcopy(self.data['surahs'][0]))
        self.assertIn('DUPLICATE_SURAH', self.codes())
        self.data['surahs'][0]['sections'] = []
        self.assertIn('NO_SECTIONS', self.codes())


if __name__ == '__main__':
    unittest.main()
