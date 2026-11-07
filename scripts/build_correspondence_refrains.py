"""Generate offline transitions from frozen Quran/QCF and existing Atlas bounds.
Theme labels and stations are application editorial aids, NOT classical quotations.
"""
import csv,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview'
CAT=ROOT/'docs/correspondances/catalogue-62.json'
SOURCES={
 'R047':[{'titre':'al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 54:17','url':'https://quran.ksu.edu.sa/tafseer/qortobi/sura54-aya17.html','portee':'Retour de l’appel au rappel dans les récits ; ne fournit pas les stations mnémotechniques de cette application.'}],
 'R048':[{'titre':'al-Baghawī, Maʿālim al-tanzīl, 55:13','url':'https://quran.ksu.edu.sa/tafseer/baghawy/sura55-aya13.html','portee':'Répétition pour rappeler et faire reconnaître les bienfaits ; ne fixe pas les bornes des stations éditoriales.'},{'titre':'al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 55:13','url':'https://quran.ksu.edu.sa/tafseer/qortobi/sura55-aya13.html','portee':'Adresse aux humains et aux djinns et sens des bienfaits ; pas une méthode mnémotechnique attribuée.'}],
 'R059':[{'titre':"al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 77:50",'url':"https://quran.ksu.edu.sa/tafseer/qortobi/sura77-aya50.html",'portee':"La répétition renvoie successivement à des objets de dénégation différents ; ne fournit pas les libellés mnémotechniques."},{'titre':"Ibn Kathīr, Tafsīr al-Qurʾān al-ʿAẓīm, 77:15",'url':"https://quran.ksu.edu.sa/tafseer/katheer/sura77-aya15.html",'portee':"Avertissement des négateurs après les signes de la création ; les légendes anglaises restent éditoriales."}]}
# Each pair names the themes at the two IMMEDIATELY adjacent Quran references.
CONTEXT={
 'R047':[(17,'Nūḥ : issue du récit','ʿĀd : début du récit'),(22,'ʿĀd : issue du récit','Thamūd : début du récit'),(32,'Thamūd : issue du récit','Lūṭ : début du récit'),(40,'Lūṭ : issue du récit','La famille de Pharaon')],
 'R048':[(13,'Grains et plantes aromatiques','Création de l’être humain'),(16,'Création des djinns','Deux Orients et deux Occidents'),(18,'Deux Orients et deux Occidents','Deux mers qui se rencontrent'),(21,'Barrière entre les deux mers','Perles et corail'),(23,'Perles et corail','Navires dans la mer'),(25,'Navires dans la mer','Finitude des créatures'),(28,'Permanence du Seigneur','Les créatures Lui demandent'),(30,'Les créatures Lui demandent','Adresse aux deux communautés'),(32,'Adresse aux deux communautés','Limites des humains et des djinns'),(34,'Limites des humains et des djinns','Flammes et métal fondu'),(36,'Flammes et métal fondu','Ciel déchiré'),(38,'Ciel déchiré','Humains, djinns et leurs fautes'),(40,'Humains, djinns et leurs fautes','Les criminels reconnus'),(42,'Les criminels reconnus','La Géhenne déniée'),(45,'Entre Géhenne et eau brûlante','Deux jardins pour qui craint'),(47,'Deux jardins pour qui craint','Des branches'),(49,'Des branches','Deux sources qui coulent'),(51,'Deux sources qui coulent','Fruits par paires'),(53,'Fruits par paires','Couches et fruits à portée'),(55,'Couches et fruits à portée','Regards réservés'),(57,'Regards réservés','Rubis et corail'),(59,'Rubis et corail','Récompense de la bienfaisance'),(61,'Récompense de la bienfaisance','Deux autres jardins'),(63,'Deux autres jardins','Verdure profonde'),(65,'Verdure profonde','Deux sources jaillissantes'),(67,'Deux sources jaillissantes','Fruits, palmiers et grenades'),(69,'Fruits, palmiers et grenades','Compagnes bonnes et belles'),(71,'Compagnes bonnes et belles','Compagnes dans les pavillons'),(73,'Compagnes dans les pavillons','Compagnes préservées'),(75,'Compagnes préservées','Coussins verts et beaux tapis'),(77,'Coussins verts et beaux tapis','Clôture : majesté et générosité')],
 'R059':[(15,'Le Jour de la Décision','Destruction des premiers peuples'),(19,'Sort des criminels','Création humaine'),(24,'Mesure de la création','La terre comme réceptacle'),(28,'Montagnes et eau douce','Départ vers le châtiment dénié'),(34,'Étincelles du brasier','Jour sans parole'),(37,'Aucune excuse permise','Rassemblement au Jour de la Décision'),(40,'Défi aux stratagèmes','Ombres et sources des pieux'),(45,'Rétribution des bienfaisants','Jouissance brève des criminels'),(47,'Jouissance brève des criminels','Refus de s’incliner'),(49,'Refus de s’incliner','Conclusion : quelle parole croiront-ils ?')]}
VISUAL_CUES={
 'R047':[
  ("Prophetic accounts", "Nūḥ's warning", "ʿĀd begins"),
  ("Prophetic accounts", "ʿĀd's warning", "Thamūd begins"),
  ("Prophetic accounts", "Thamūd's fate", "Lūṭ begins"),
  ("Prophetic accounts", "Lūṭ's warning", "Pharaoh's kin"),
 ],
 'R048':[
  ("Creation", "Grain, herbs", "Clay-born man"),
  ("Creation", "Jinn's fire", "Two Easts"),
  ("Creation", "Two Easts", "Two seas"),
  ("Sea gifts", "Sea barrier", "Pearls, coral"),
  ("Sea gifts", "Pearls, coral", "Great ships"),
  ("Sea gifts", "Great ships", "All perish"),
  ("Dependence", "Lord endures", "All petition"),
  ("Dependence", "All petition", "Two peoples"),
  ("Reckoning", "Two peoples", "No escape"),
  ("Reckoning", "No escape", "Flames, copper"),
  ("Reckoning", "Flames, copper", "Sky splits"),
  ("Reckoning", "Sky splits", "No questioning"),
  ("Reckoning", "No questioning", "Sinners marked"),
  ("Reckoning", "Sinners marked", "Hellfire"),
  ("Reckoning", "Boiling water", "Two gardens"),
  ("First gardens", "Two gardens", "Branches"),
  ("First gardens", "Branches", "Flowing springs"),
  ("First gardens", "Flowing springs", "Paired fruits"),
  ("First gardens", "Paired fruits", "Silken couches"),
  ("First gardens", "Silken couches", "Modest gaze"),
  ("First gardens", "Modest gaze", "Rubies, coral"),
  ("First gardens", "Rubies, coral", "Good repaid"),
  ("First gardens", "Good repaid", "Other gardens"),
  ("Other gardens", "Other gardens", "Deep green"),
  ("Other gardens", "Deep green", "Gushing springs"),
  ("Other gardens", "Gushing springs", "Dates, pomegranates"),
  ("Other gardens", "Dates, pomegranates", "Beautiful companions"),
  ("Other gardens", "Beautiful companions", "Pavilions"),
  ("Other gardens", "Pavilions", "Untouched companions"),
  ("Other gardens", "Untouched companions", "Green cushions"),
  ("Other gardens", "Green cushions", "Majestic Lord"),
 ],
 'R059':[
  ("Decision", "Decision Day", "Ancient peoples"),
  ("Creation", "Sinners' fate", "Human creation"),
  ("Creation", "Creation measured", "Earth's embrace"),
  ("Creation", "Mountains, water", "Punishment ahead"),
  ("Judgement", "Blazing sparks", "Silent day"),
  ("Judgement", "No excuses", "Gathering"),
  ("Judgement", "Vain schemes", "Shade, springs"),
  ("Reward", "Good rewarded", "Brief enjoyment"),
  ("Final warning", "Brief enjoyment", "Refused bowing"),
  ("Final warning", "Refused bowing", "Which message?"),
 ],
}
NAMES={'R047':'Al-Qamar','R048':'Ar-Raḥmān','R059':'Al-Mursalāt'}
def station(id,a):
 if id=='R047':return {17:'Nūḥ',22:'ʿĀd',32:'Thamūd',40:'Lūṭ'}[a]
 if id=='R048':
  return next(t for end,t in [(18,'Création et ordre'),(25,'Mers et bienfaits'),(30,'Finitude et dépendance'),(45,'Interpellation et jugement'),(61,'Deux jardins'),(77,'Deux autres jardins')] if a<=end)
 return next(t for end,t in [(28,'Décision, peuples et création'),(40,'Scènes du jugement'),(45,'Séjour des pieux'),(49,'Derniers avertissements')] if a<=end)
def build():
 g=json.loads((ROOT/'app/src/main/assets/reader109/geometry.json').read_text())
 refs=sorted(g['verses'],key=lambda r:tuple(map(int,r.split(':'))));text=dict(zip(refs,(ROOT/'scripts/data/quran-uthmani-tanzil.txt').read_text().splitlines()))
 cat={d['id']:d for d in json.loads(CAT.read_text())['dossiers']}
 atlas=list(csv.DictReader((ROOT/'hifz-app/src/main/assets/mawdudi_public/passages_v1.csv').read_text().splitlines()))
 result={'version':2,'methodologie':"Reprise VISUELLE de la frise Atlas (axe, jalons circulaires et halo de sélection), sans reprendre ses titres. Les 45 repères anglais sont des mots-clés mnémotechniques éditoriaux issus des versets coraniques immédiatement avant/après chaque refrain, avec références, texte arabe et preuve. Les sources classiques documentent la fonction des répétitions, sans être présentées comme auteurs des mots-clés. Atlas, QCF et Hifz restent inchangés.",'relations':{}}
 rows=[]
 for id,contexts in CONTEXT.items():
  nodes=[];surah=int(cat[id]['reperes'][0].split(':')[0]);assert [f'{surah}:{a}' for a,_,_ in contexts]==cat[id]['reperes']
  for i,(a,before,after) in enumerate(contexts):
   ref=f'{surah}:{a}';p=g['verses'][ref][0]
   node={'id':f'{id}-{i+1:02d}','rank':i+1,'reference':ref,'page':p,'station':station(id,a),'atlasPassage':next(r['id'] for r in atlas if int(r['surah'])==surah and int(r['start'])<=a<=int(r['end']))}
   for key,delta,label in [('before',-1,before),('after',1,after)]:
    neighbour=f'{surah}:{a+delta}';node[key]={'reference':neighbour,'page':g['verses'][neighbour][0],'theme':label,'arabic':text[neighbour],'preuve':f'https://quran.com/{surah}/{a+delta}'}
   node['refrainSha256']=hashlib.sha256(text[ref].encode()).hexdigest()
   stage,beforeCue,afterCue=VISUAL_CUES[id][i]
   assert len(beforeCue.split())<=2 and len(afterCue.split())<=2
   node['stage_en']=stage;node['before']['cue_en']=beforeCue;node['after']['cue_en']=afterCue
   nodes.append(node)
   fields=[id,NAMES[id],node['id'],str(i+1),str(len(contexts)),ref,str(p),node['station'],node['before']['reference'],before,str(node['before']['page']),node['after']['reference'],after,str(node['after']['page']),node['stage_en'],node['before']['cue_en'],node['after']['cue_en']]
   rows.append('        new String[]{'+','.join(json.dumps(x,ensure_ascii=False) for x in fields)+'}')
  result['relations'][id]={'titre':NAMES[id],'sources':SOURCES[id],'nodes':nodes}
 (ROOT/'docs/correspondances/frises-refrains.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 (J/'RefrainTimelineData.java').write_text('package com.quransafeguard.hifz.preview;\n\n/** Quran/QCF neighbours; visual mnemonic cues are EDITORIAL, never Atlas titles. */\nfinal class RefrainTimelineData {\n    private RefrainTimelineData() {}\n    // Append-only columns 14..16: stage, prior verse English cue, following verse English cue.\n    static final String[][] NODES={\n'+',\n'.join(rows)+'\n    };\n}\n')
 lines=["# Frises visuelles — 45 refrains, trois sourates","","Graphisme repris de la frise Atlas : axe horizontal, points et halo de sélection. Aucun TITRE textuel d'Atlas ne sert de mot-clé. Les repères anglais viennent des versets coraniques immédiatement voisins de chaque refrain. Les mots-clés sont éditoriaux, non des citations des commentateurs.","","Bibliographie de la fonction des refrains : al-Qurṭubī, *al-Jāmiʿ*, 54:17 et 77:50 ; al-Baghawī, *Maʿālim al-tanzīl*, 55:13 ; Ibn Kathīr, *Tafsīr*, 77:15. La source primaire de chaque mot-clé est le verset référencé ci-dessous.","","| Dossier | Rang | Étape visuelle | Avant (verset) | Refrain / QCF | Après (verset) | Passage Atlas (navigation uniquement) |","|---|---|---|---|---|---|---|"]
 for id,relation in result['relations'].items():
  for n in relation['nodes']:
   lines.append('| '+' | '.join([id,f"{n['rank']}/{len(relation['nodes'])}",n['stage_en'],f"{n['before']['reference']} — **{n['before']['cue_en']}**",f"{n['reference']} · p.{n['page']}",f"{n['after']['reference']} — **{n['after']['cue_en']}**",n['atlasPassage']])+' |')
 (ROOT/'docs/correspondances/frises-45-ancres-20261105.md').write_text('\n'.join(lines)+'\n')
 print('45 transitions generated; exact adjacent anchors and existing Atlas passage ids.')
if __name__=='__main__':build()
