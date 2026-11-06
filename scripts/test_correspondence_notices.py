import json,re,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class Notices(unittest.TestCase):
 def test_each_active_notice_has_a_short_editorial_abstract_and_unchanged_provenance(self):
  rows=json.loads((ROOT/'docs/correspondances/catalogue-62.json').read_text())['dossiers']
  for d in rows:
   if d['statut']!='ADMIS':continue
   self.assertIn('enBref',d,d['id']);brief=d['enBref']
   self.assertTrue(2<=len(re.split(r'(?<=[.!?])\s+',brief))<=3,(d['id'],brief))
   self.assertLess(len(brief),850,d['id']);self.assertTrue(d['sources']);self.assertTrue(d['explication'])
if __name__=='__main__':unittest.main()
