import csv,json,re,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class RefrainTest(unittest.TestCase):
 def test_forty_five_exact_ordered_nodes_and_real_neighbours(self):
  path=ROOT/'docs/correspondances/frises-refrains.json'
  self.assertTrue(path.exists(),'45 transitions must be documented before runtime activation')
  d=json.loads(path.read_text());g=json.loads((ROOT/'app/src/main/assets/reader109/geometry.json').read_text())
  catalogue={x['id']:x for x in json.loads((ROOT/'docs/correspondances/catalogue-62.json').read_text())['dossiers']}
  atlas=list(csv.DictReader((ROOT/'hifz-app/src/main/assets/mawdudi_public/passages_v1.csv').read_text().splitlines()))
  refs=sorted(g['verses'],key=lambda r:tuple(map(int,r.split(':'))));texts=dict(zip(refs,(ROOT/'scripts/data/quran-uthmani-tanzil.txt').read_text().splitlines()))
  for id,count in [('R047',4),('R048',31),('R059',10)]:
   nodes=d['relations'][id]['nodes'];self.assertEqual(count,len(nodes));self.assertEqual(catalogue[id]['reperes'],[n['reference'] for n in nodes])
   for i,n in enumerate(nodes):
    s,a=map(int,n['reference'].split(':'));self.assertEqual(i+1,n['rank']);self.assertEqual(g['verses'][n['reference']][0],n['page'])
    for key,delta in [('before',-1),('after',1)]:
     x=n[key];self.assertEqual(f'{s}:{a+delta}',x['reference']);self.assertEqual(texts[x['reference']],x['arabic']);self.assertIn(x['page'],g['verses'][x['reference']])
    self.assertTrue(any(r['id']==n['atlasPassage'] and int(r['start'])<=a<=int(r['end']) for r in atlas))
  self.assertEqual('54:41',d['relations']['R047']['nodes'][-1]['after']['reference'])
  self.assertIn('Pharaon',d['relations']['R047']['nodes'][-1]['after']['theme'])
  self.assertEqual('55:78',d['relations']['R048']['nodes'][-1]['after']['reference'])
  self.assertEqual('77:50',d['relations']['R059']['nodes'][-1]['after']['reference'])
 def test_view_is_collapsible_and_only_in_sources(self):
  p=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/RefrainTimeline.java'
  self.assertTrue(p.exists())
  s=p.read_text();self.assertIn('Frise des refrains',s);self.assertIn('View.GONE',s)
  self.assertNotIn('HifzPrefs',s);self.assertNotIn('recordRepetition',s)
if __name__=='__main__':unittest.main()
