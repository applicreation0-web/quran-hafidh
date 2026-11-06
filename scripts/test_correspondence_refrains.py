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
 def test_atlas_style_rail_has_six_visible_rahman_points_and_four_qamar_points(self):
  p=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/RefrainTimeline.java'
  s=p.read_text()
  comparison=(ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceComparisonActivity.java').read_text()
  self.assertIn('HorizontalScrollView',s)
  self.assertIn('Math.min(6, nodes.size())',s)
  self.assertIn('scroll.setHorizontalScrollBarEnabled(false)',s)
  self.assertIn('scroll.post(redraw[0])',s)
  self.assertIn('RefrainTimeline.append(this,root,relation.id,origin.toString(),timelineState)',comparison)
  self.assertNotIn('RefrainTimeline.append(this,column',comparison)
  self.assertNotIn('Repères pédagogiques éditoriaux',s)
  self.assertIn('StudyReaderActivity.EXTRA_JUMP_VERSE',s)
  self.assertNotIn('HifzPrefs',s)
  self.assertNotIn('recordRepetition',s)
  for key,expected in [('RAHMAN_BEFORE',31),('RAHMAN_AFTER',31),('QAMAR_BEFORE',4),('QAMAR_AFTER',4)]:
   block=re.search(r'private static final String\\[\\] '+key+r' = \\{(.*?)\\};',s,re.S)
   self.assertIsNotNone(block,key)
   self.assertEqual(expected,len(re.findall(r'"[^"]+"',block.group(1))),key)
if __name__=='__main__':unittest.main()
