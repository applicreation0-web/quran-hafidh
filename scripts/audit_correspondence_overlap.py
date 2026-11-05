"""Trace all 62 dossiers against the actually active Similitudes pairs, not a target count."""
import hashlib,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/correspondances'
JAVA=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview'
def audit():
 source=(JAVA/'AtlasDiscriminantCases.java').read_text()
 # The runtime PILOT is the approved CATALOGUE, not a four-item debug fixture.
 assert 'PILOT = CATALOGUE' in source or 'PILOT=CATALOGUE' in source
 pairs=[(f'{a}:{b}',f'{c}:{d}') for a,b,c,d in re.findall(r'new Entry\((\d+),(\d+),(\d+),(\d+),',source)]
 cat=json.loads((BASE/'catalogue-62.json').read_text());rows=[]
 motives={
  'R036':'Similitudes compare une variation locale de 11:66 / 11:82. Correspondances situe cinq retours du commandement et leurs issues différentes dans cinq récits ; aucun récit intégralement identique n’est prétendu.',
  'R044':'Similitudes compare les variations lexicales de 39:71 / 39:73. Correspondances expose les deux groupes et les issues des scènes 71–74, avec la clôture commune du jugement en 75 ; ce n’est pas une simple différence de mot.',
  'R053':'Similitudes compare les variantes entre deux conditions de piété. Correspondances rassemble les trois conditions 65:2/4/5 et leurs suites distinctes ; les versets entiers ne sont pas annoncés identiques.',
  'R079':'Similitudes compare la formule de 8:4 / 8:74. Correspondances documente la reprise aux frontières de la sourate, avec al-Suyūṭī, Marāṣid p.51 ; le mécanisme est l’encadrement et non la variation lexicale.'}
 ownership={}
 for d in cat['dossiers']:
  links={tuple(sorted((a,b))) for a,targets in d['comparaisons'].items() for b in targets}
  overlap=sorted({tuple(sorted((a,b))) for a,b in pairs if tuple(sorted((a,b))) in links})
  shared=[]
  for link in sorted(links):
   if link in ownership:shared.append({'paire':link,'autreDossier':ownership[link]})
   ownership[link]=d['id']
  assert not shared,(d['id'],shared)
  if overlap:assert d['id'] in motives,('Undecided overlap',d['id'],overlap)
  admitted=d['statut']=='ADMIS'
  reason=motives.get(d['id'],'Aucun même couple actif dans Similitudes ; mécanisme et partenaires limités à la notice documentée.')
  if d['id'] in ['R047','R048','R059']:reason='Une seule fiche structurelle, frise de contextes avant/après sourcée ; aucun même couple dans Similitudes actif. Les occurrences restent des nœuds et des marqueurs, pas des dossiers dupliqués.'
  if not admitted:reason=d['conditionManquante']
  # This is a documentary scope check, not a new external authentication of every work.
  scholars=[s for s in d['sources'] if not s['titre'].startswith('Texte coranique')]
  row={'id':d['id'],'partenaires':d['comparaisons'],'nature':d['categorie'],'explication':d['explication'],'enBref':d['enBref'],'sources':d['sources'],'porteeAttribution':[s['portee'] for s in scholars] or ['Observation éditoriale du texte primaire ; aucune attribution savante.'],'pairesSimilitudes':overlap,'mecanismeComplementaire':reason,'verdict':'CONSERVER' if admitted else 'INACTIF_CONDITIONNEL','destination':'Correspondances' if admitted else 'Absence de l’interface active','motif':reason}
  rows.append(row)
 result={'date':'2026-11-05','base':'a266b279e3f0c1a9684861aa703ca5e2125ef3d9','similitudesPairs':len(pairs),'similitudesSha256':hashlib.sha256(source.encode()).hexdigest(),'methode':'Comparaison exacte des arêtes autorisées, puis lecture du mécanisme ; les seuls versets partagés ne suffisent pas. Les portées reprennent les notices existantes et ne valent pas nouvelle collation externe.','bilan':{'activesAvant':60,'activesApres':sum(r['verdict']=='CONSERVER' for r in rows),'desactiveesPourDoublon':0,'conditionnelles':2,'exclusionsHistoriques':48},'dossiers':rows}
 (BASE/'dedoublonnage-20261105.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 lines=['# Notices, sources et dédoublonnage — 5 novembre 2026','','Contrôle des 60 explications françaises initiales et des 2 fiches inactives. Les attributions sont limitées à la portée déjà documentée ; aucune nouvelle certification bibliographique collective.','','| ID | Verdict | Similitudes : mêmes paires | Mécanisme / justification | Sources et portée |','|---|---|---|---|---|']
 for r in rows:
  sources='<br>'.join(s['titre']+' — '+s['portee']+' '+s['url'] for s in r['sources'])
  lines.append('| '+' | '.join([r['id'],r['verdict'],str(r['pairesSimilitudes']),r['enBref']+' '+r['motif'],sources]).replace('\n',' ')+' |')
 (BASE/'notices-et-dedoublonnage-20261105.md').write_text('\n'.join(lines)+'\n')
 print(json.dumps(result['bilan']),len(pairs),'active Similitudes pairs; overlap decisions',[(r['id'],len(r['pairesSimilitudes'])) for r in rows if r['pairesSimilitudes']])
if __name__=='__main__':audit()
