"""Synthetic-only tests of OCR loss measurement; no protected book text."""
import unittest
from mawdudi_hocr_epub_retention import normalize,measure_words
class RetentionTests(unittest.TestCase):
    def test_identical_ocr_pages_are_complete_metadata_only(self):
        words=['ordinary']*200
        x=measure_words(words,words)
        self.assertEqual('NO_MAJOR_DIFFERENCE_DETECTED',x['risk'])
        self.assertEqual(1.0,x['retention'])
    def test_lost_large_text_fails_strictly(self):
        original=['normal']*300;epub=original[:50]
        x=measure_words(original,epub)
        self.assertEqual('CRITICAL_OCR_LOSS_CANDIDATE',x['risk'])
        self.assertEqual(250,x['missing_hocr_tokens'])
    def test_moderate_loss_is_high_risk(self):
        original=['normal']*250;epub=original[:160]
        self.assertEqual('HIGH_OCR_LOSS_CANDIDATE',measure_words(original,epub)['risk'])
    def test_punctuation_and_numbers_not_used_for_retention(self):
        self.assertEqual('commentary',normalize('Commentary.'))
        self.assertEqual('',normalize('2'))
        self.assertEqual('',normalize('100'))
    def test_small_ocr_page_is_low_evidence(self):
        self.assertEqual('LOW_EVIDENCE',measure_words(['a']*20,[])['risk'])
if __name__=='__main__':unittest.main()
