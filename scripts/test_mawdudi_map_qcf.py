"""Numerical mapping contract; fixtures contain no protected editorial content."""
import copy
import importlib.util
from pathlib import Path
import unittest

MODULE = Path(__file__).with_name('mawdudi_map_qcf.py')

class MappingTests(unittest.TestCase):
    def setUp(self):
        self.assertTrue(MODULE.exists(), 'mapping implementation does not exist yet')
        spec = importlib.util.spec_from_file_location('qcf_mapping', MODULE)
        self.m = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.m)
        self.passages = [{'id':'s001a001b002','surah':1,'start':1,'end':2,'displayTitle':'DO NOT EXPORT'}]
        self.geometry = {'pages':{'1':{'viewBox':[-10,-20,345,550], 'lines':[{'id':'1:0','page':1,'top':0,'bottom':30,'verses':['1:1'],'cells':[[0,100]]}]},'2':{'viewBox':[0,0,345,550], 'lines':[{'id':'2:0','page':2,'top':0,'bottom':40,'verses':['1:1','1:2'],'cells':[[0,100]]}]}}, 'verses':{'1:1':[1,2],'1:2':[2]}}
        self.chunks = [{'schema':'quran-haafidh-word-boxes-v1','source':'quran-ws/quran-svg-elements','source_release':'v1.1.2','box_space':'viewBox 0 0 345 550','pages':{'1':[['1:1:1',20,25,30,35]],'2':[['1:1:2',20,10,30,20],['1:2:1',40,10,50,20]]}}]
    def build(self):
        return self.m.build_mapping(self.passages,self.geometry,self.chunks, expected_counts=[2],expected_pages=2)
    def test_preserves_all_ayat_continuity_exact_marker_spaces_and_inputs(self):
        before = copy.deepcopy((self.passages,self.geometry,self.chunks))
        rows,report = self.build()
        self.assertEqual(len(rows),1)
        self.assertEqual([a['ayah'] for a in rows[0]['ayat']],[1,2])
        self.assertEqual(rows[0]['pages'],[1,2])
        self.assertEqual(rows[0]['first_word']['box_normalized'],[20,25,30,35])
        self.assertEqual(rows[0]['first_word']['box_svg'],[10,5,20,15])
        self.assertEqual(rows[0]['first_word']['line_id'],'1:0')
        self.assertEqual(len(rows[0]['ayat'][0]['words']),2)
        self.assertNotIn('DO NOT EXPORT',str(rows))
        self.assertEqual(report['covered_ayat'],2)
        self.assertEqual((self.passages,self.geometry,self.chunks),before)
    def test_rejects_overlap_gap_bad_order_and_duplicate_id(self):
        for passages in [self.passages*2,[dict(self.passages[0],start=2,end=1)],[dict(self.passages[0],end=1)]]:
            with self.subTest(passages=passages),self.assertRaises(ValueError):
                self.m.build_mapping(passages,self.geometry,self.chunks,expected_counts=[2],expected_pages=2)
    def test_rejects_invalid_page_domain(self):
        self.geometry['pages']['3'] = self.geometry['pages'].pop('2')
        with self.assertRaises(ValueError): self.build()
    def test_missing_boxes_are_explicit_and_marker_not_invented(self):
        self.chunks[0]['pages']['1']=[]
        rows,report=self.build()
        self.assertIsNone(rows[0]['first_word'])
        self.assertGreater(report['issue_count'],0)
        self.assertIn('missing_first_word',str(rows[0]['issues']))
    def test_invalid_box_is_flagged_and_not_exported_as_verified(self):
        self.chunks[0]['pages']['1'][0][3]=346
        rows,report=self.build()
        self.assertIsNone(rows[0]['first_word'])
        self.assertIn('invalid_box',str(report['issues']))
    def test_wrong_line_and_noncontiguous_word_indices_flagged(self):
        self.chunks[0]['pages']['1'][0][2]=200
        self.chunks[0]['pages']['1'][0][4]=210
        self.chunks[0]['pages']['2'][0][0]='1:1:3'
        rows,report=self.build()
        self.assertIsNone(rows[0]['first_word'])
        self.assertIn('unverified_line',str(report['issues']))
        self.assertIn('word_sequence_gap',str(report['issues']))
    def test_rejects_untrusted_word_source(self):
        self.chunks[0]['source']='different/source'
        with self.assertRaises(ValueError): self.build()
    def test_flags_source_word_order_instead_of_silently_sorting_it(self):
        self.chunks[0]['pages']['2'].reverse()
        rows,report=self.build()
        self.assertIn('source_word_order',str(report['issues']))
    def test_export_batches_never_exceed_ten_surahs(self):
        self.assertEqual(self.m.surah_batches(114),[(i,min(i+9,114)) for i in range(1,115,10)])

if __name__ == '__main__': unittest.main()
