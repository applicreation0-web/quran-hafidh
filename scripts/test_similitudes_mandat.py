import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class SimilitudesMandat(unittest.TestCase):
 def test_common_conclusion_is_not_presented_as_a_replacement_word(self):
  s=(ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantActivity.java').read_text()
  self.assertIn('entry.auditId==168',s)
  self.assertIn('Conclusion commune',s)
 def test_frozen_correct_reference_and_real_omission_are_preserved(self):
  p=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview'
  self.assertIn('new Entry(2,232,65,2,',(p/'AtlasDiscriminantCases.java').read_text())
  self.assertIn('new int[]{18,19,20}',(p/'AtlasDiscriminantOmissions.java').read_text())
if __name__=='__main__':unittest.main()
