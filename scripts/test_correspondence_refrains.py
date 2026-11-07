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
  self.assertIn('RefrainTimeline.append(this,root,relation.id,origin.toString(),timelineState,this::selectRefrain)',comparison)
  self.assertIn('private void selectRefrain(String ref)',comparison)
  self.assertIn('if(partnerOffset!=i){partnerOffset=i;changePartner(0);}',comparison)
  self.assertNotIn('RefrainTimeline.append(this,column',comparison)
  self.assertNotIn('Repères pédagogiques éditoriaux',s)
  self.assertIn('StudyReaderActivity.EXTRA_JUMP_VERSE',s)
  self.assertNotIn('HifzPrefs',s)
  self.assertNotIn('recordRepetition',s)
  builder=(ROOT/'scripts/build_correspondence_refrains.py').read_text()
  self.assertIn('VISUAL_CUES=',builder)
  for forbidden in ['RefrainAtlasThemes.load','MawdudiMapData.load','AyahThemesCueIndex.load',
                    'Atlas · Unit (editorial)','atlas.displayUnitTitle']:
   self.assertNotIn(forbidden,s+comparison)
  self.assertIn('class Marks extends View',s)
  self.assertIn('c.drawLine(',s)
  self.assertIn('c.drawCircle(',s)
  self.assertIn('scroll.post(redraw[0])',s)
  self.assertIn('n[15]',s)
  self.assertIn('n[16]',s)
  self.assertIn('n[14]',s)
  self.assertIn('Quran-context refrain station',s)
  self.assertIn('Exégèses · fonction du refrain',comparison)
  self.assertIn('qortobi/sura77-aya50.html',comparison)
  document=json.loads((ROOT/'docs/correspondances/frises-refrains.json').read_text())
  self.assertEqual(2,document['version'])
  observed=[]
  for relation,expected in [('R047',4),('R048',31),('R059',10)]:
   items=document['relations'][relation]['nodes']
   self.assertEqual(expected,len(items))
   for n in items:
    self.assertTrue(n['stage_en'])
    for side in ('before','after'):
     cue=n[side]['cue_en']
     self.assertTrue(cue)
     self.assertLessEqual(len(cue.split()),2)
     self.assertEqual(n[side]['reference'].split(':')[0],n['reference'].split(':')[0])
    observed.append(n['before']['cue_en'])
  self.assertEqual(45,len(observed))
  self.assertNotIn('RAHMAN_EXCERPTS',s)
  self.assertNotIn('MURSALAT_EXCERPTS',s)
if __name__=='__main__':unittest.main()
