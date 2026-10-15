"""Pure metadata-only regressions; no Maududi text or private inputs."""
import unittest
from mawdudi_ocr_page_reconcile import classify_case, reader_page_for_epub_member

class ScanLedgerTests(unittest.TestCase):
    def setUp(self):
        self.pages=[{'page':45,'refs':[(2,95),(2,96)]},
                    {'page':46,'refs':[(2,101)]},
                    {'page':47,'refs':[(2,102)]}]
    def test_hocr_hit_still_requires_visual_review(self):
        hit={'status':'EXACT_HOCR_WORD','hits':[{'page':46,'confidence':96}]}
        result=classify_case(2,100,self.pages,hit)
        self.assertEqual('NOT_APPROVED',result['editorial_state'])
        self.assertEqual('NOT_EXECUTED',result['browser_visual_state'])
        self.assertFalse(result['repair_applied'])
        self.assertEqual([46],result['candidate_pages'])
        self.assertEqual([45],result['bookreader_pages'])
        self.assertTrue(result['bookreader_urls'][0].endswith('/page/n45/mode/1up'))
    def test_page_alignment_missing_browser_label_is_not_false_repair(self):
        hit={'status':'EXACT_HOCR_WORD','hits':[{'page':46,'confidence':90}]}
        r=classify_case(2,100,self.pages,hit,{'page':45,'status':'NOT_SEEN_IN_BROWSER_VIEW'})
        self.assertEqual('NOT_SEEN_IN_BROWSER_VIEW',r['browser_visual_state'])
        self.assertEqual('NOT_APPROVED',r['editorial_state'])
    def test_bracket_only_candidate_window(self):
        out=classify_case(2,100,self.pages,{'status':'NOT_EXACT'})
        self.assertEqual([45,46],out['candidate_pages'])
        self.assertEqual('EPUB_BRACKET_REQUIRES_IMAGE',out['index_mode'])
        self.assertEqual([44,45],out['bookreader_pages'])
    def test_exact_epub_never_promoted_to_verified_commentary(self):
        p=[{'page':45,'refs':[(2,95)]},{'page':46,'refs':[(2,100)]}]
        out=classify_case(2,100,p,{'status':'OUTSIDE_CAPTURE_22_8'})
        self.assertEqual('EPUB_EXACT_REQUIRES_IMAGE',out['index_mode'])
        self.assertEqual('NOT_VERIFIED',out['commentary_body'])
        self.assertEqual('NOT_VERIFIED',out['footnotes'])
    def test_visual_evidence_wrong_page_rejected(self):
        with self.assertRaises(ValueError):
            classify_case(2,100,self.pages,{'status':'NOT_EXACT'},{'page':300,'status':'VISIBLE_IN_BROWSER_VIEW'})
    def test_original_epub_page_is_not_reader_display_page(self):
        self.assertEqual(1072,reader_page_for_epub_member(1073))
        self.assertEqual(581,reader_page_for_epub_member(582))
        self.assertEqual(960,reader_page_for_epub_member(961))
        with self.assertRaises(ValueError):
            reader_page_for_epub_member(0)
    def test_reject_unshifted_visual_observation(self):
        with self.assertRaises(ValueError):
            classify_case(2,100,self.pages,
                {'status':'EXACT_HOCR_WORD','hits':[{'page':46,'confidence':96}]},
                {'page':46,'status':'VISIBLE_IN_BROWSER_VIEW'})
    def test_unapproved_status_rejects_injection(self):
        with self.assertRaises(ValueError):
            classify_case(2,100,self.pages,{'status':'NOT_EXACT'},{'page':45,'status':'APPROVED'})

if __name__=='__main__':unittest.main()
