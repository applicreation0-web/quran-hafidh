"""Reproduce the accepted additive catalogue from the frozen documentary register.
No historical verdict, excluded dossier, Quran folio or Hifz state is rewritten.
"""
import hashlib,json,re,subprocess
from build_correspondence_refrains import SOURCES as REFRAIN_SOURCES
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/correspondances'
JAVA=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceCatalogue.java'
TITLES={
'R007':'Nūḥ : récit bref et récit développé','R008':'Du rappel des récits au récit de Yūsuf','R011':'La louange à la jonction de deux sourates','R014':'Pardon et miséricorde : ordre inversé','R015':'Deux ouvertures par la louange','R018':'La révélation du Livre','R019':'Les sept ouvertures Hā-Mīm','R023':'Quand survient le Jour','R025':'Orphelin et pauvre : reproche et bienfaisance','R026':'Jour et nuit : ordre inversé','R027':'Deux rappels des bienfaits','R028':'Protection de la Maison et sécurité','R029':'Deux demandes de refuge','R030':"Rappel aux enfants d’Israël",'R031':'Une communauté passée et ses actes','R032':'La qibla : lecture locale de Farrin','R033':'Āyat al-Kursī : hypothèse interne','R034':'Une même injonction dans quatre récits','R035':'Hypocrites et croyants : conduites et issues','R036':'Le commandement divin dans cinq récits','R037':'La vision de Yūsuf et son accomplissement','R039':'Mentionner dans le Livre','R040':'Huit retours du refrain','R041':'La promesse du retour de Mūsā','R043':'Quatre salutations aux prophètes','R044':'Deux groupes conduits à leur destination','R045':'Deux attitudes envers les parents','R047':"Le refrain d’Al-Qamar",'R048':"Le refrain d’Ar-Raḥmān",'R049':'Les trois catégories, au début et à la fin','R051':'La glorification aux deux extrémités','R053':'Trois conditions de piété','R054':'Contre-exemples et exemples de foi','R055':'Le livre à droite ou à gauche','R057':'La veille nocturne reprise à la fin','R058':'Deux tableaux des visages','R059':"Dix retours de l’avertissement",'R060':'Deux conduites et deux demeures','R061':'Visages réjouis et visages obscurcis','R063':'Deux registres et deux issues','R064':'Deux façons de recevoir le livre','R065':'Châtiment et récompense','R066':'Visages accablés et visages satisfaits','R068':"Purifier ou corrompre l’âme",'R069':'Donner ou retenir','R070':'Des bienfaits aux prescriptions','R071':'Avec la difficulté, la facilité','R073':'Deux groupes et leurs récompenses','R074':"Le poids infime du bien et du mal",'R075':'Balances lourdes ou légères','R078':'Alternance grammaticale : je et vous','R079':'Les vrais croyants et leur récompense','R082':'Dāwūd et Sulaymān : deux reprises','R083':'Le commerce au moment de la prière','R091':'Prière et réussite aux frontières des sourates','R092':'Deux vocatifs du vêtement','R093':"Deux séries de signes, puis l’âme sait",'R094':'Lumière et ténèbres superposées','R096':"La foi déclarée confrontée à l’épreuve",'R098':'Deux groupes et le sort de leurs actes','R102':'Non-hostilité et hostilité','R106':'Deux séjours et leurs rétributions'}
OVERRIDES={
'R007':('Progression','10:71–73 ; 11:25–49','Yūnus condense le récit de Nūḥ : avertissement, refus et salut. Hūd développe la prédication, la construction de l’arche, le déluge et le salut. Al-Suyūṭī rapproche ce récit bref de son développement dans Hūd. La flèche représente résumé → développement, sans dater les sourates ni les événements.'),
'R008':('Parallélisme','11:120 ; 12:3','11:120 explique la fonction des récits racontés au Prophète : affermir son cœur. 12:3 annonce le récit raconté au Prophète par la révélation. Al-Suyūṭī relève ce raccord entre les deux sourates ; les deux versets ne sont pas identiques.'),
'R011':('Début–Fin','17:111 ; 18:1','La dernière injonction de 17:111 et l’ouverture de 18:1 reprennent le noyau de louange « al-ḥamdu li-llāhi alladhī ». En 17, elle écarte les associés et la filiation ; en 18, elle introduit le don du Livre. Reprise locale fin–début : cette formule n’est pas unique dans le Coran et les versets entiers diffèrent.'),
'R027':('Parallélisme','93:6–8 ; 94:1–4','93:6–8 rappelle protection, guidance et enrichissement ; 94:1–4 rappelle ouverture de la poitrine, allègement du fardeau et élévation de la mention. Les deux séries rappellent des bienfaits au même destinataire. Il s’agit d’un parallélisme thématique, sans correspondance terme à terme des membres.'),
'R036':('Parallélisme','11:40 ; 11:58 ; 11:66 ; 11:82 ; 11:94','Le récit de Nūḥ emploie la venue du commandement en 11:40 ; ceux de Hūd, Ṣāliḥ, Lūṭ et Shuʿayb reprennent cette arrivée en 11:58, 66, 82 et 94. Les formes temporelles et les suites varient : embarquement, salut des croyants ou destruction. Le parallèle porte sur ce jalon narratif, pas sur cinq récits identiques.'),
'R049':('Début–Fin','56:7–10 ; 56:88–94','L’ouverture annonce trois catégories : les gens de la droite, ceux de la gauche et les devanciers. La fin reprend les rapprochés, les gens de la droite et les négateurs égarés, avec leurs issues. Al-Suyūṭī décrit cette reprise des trois catégories. 56:11 relie les devanciers aux rapprochés mais demeure un contexte extérieur à la première plage. L’ordre final ne forme pas un miroir strict.'),
'R065':('Opposition','85:10 ; 85:11','85:10 annonce le châtiment de ceux qui persécutent les croyants et les croyantes sans se repentir. 85:11 promet des jardins à ceux qui croient et accomplissent de bonnes œuvres. Les deux annonces adjacentes opposent conditions et issues. 85:4–9 donne le contexte des persécutions, sans être le premier membre de cette comparaison.'),
'R070':('Parallélisme','93:6–8 ; 93:9–11','Lecture croisée proposée : l’orphelin accueilli en 93:6 éclaire l’interdit de l’opprimer en 93:9 ; la guidance de 93:7 éclaire la proclamation du bienfait en 93:11 ; l’enrichissement de 93:8 éclaire l’accueil du demandeur en 93:10. Seule la paire orphelin possède une reprise lexicale directe. Les deux autres appariements sont une lecture argumentée, et non des identités textuelles ; « demandeur » peut aussi concerner une demande de savoir.'),
'R037':('Progression','12:4 ; 12:100','12:4 rapporte la vision de Yūsuf ; 12:100 la désigne expressément comme sa vision antérieure que son Seigneur a rendue vraie. Jalālayn confirme le renvoi à cette vision. La progression se limite à cette annonce et à cet accomplissement, sans imposer une structure globale au récit.'),
'R091':('Parallélisme','22:77 ; 23:1–2','22:77 associe l’adresse aux croyants, la prière (inclinaison et prosternation), le bien et la réussite espérée. 23:1–2 proclame la réussite des croyants puis leur humilité dans la prière. Les deux foyers précis sont la réussite et la prière ; cette proximité entre sourates ne prouve ni identité des prescriptions ni causalité chronologique.')}
BRIEFS={'R030': 'Les versets entiers 2:47 et 2:122 sont identiques dans le témoin arabe vérifié. 2:40 est différent et ne fait pas partie de cette identité.', 'R035': '9:67 et 9:71 opposent les conduites des hypocrites et des croyants : ce qu’ils commandent et interdisent est inversé. Le Feu de 9:68 contraste avec les jardins de 9:72, sans appariement intégral imposé entre chaque verset.', 'R044': '39:71–72 présente le groupe conduit vers le Feu ; 39:73–74 présente celui conduit vers les jardins. Les portes, les gardiens et les issues diffèrent dans ces deux scènes. 39:75 conclut le jugement des deux groupes.', 'R060': '79:37–39 associe rébellion et préférence de la vie présente au Feu. 79:40–41 oppose la crainte du Seigneur et la retenue du désir, avec le Paradis comme séjour.', 'R061': '80:38–39 décrit des visages rayonnants et joyeux. 80:40–42 oppose des visages couverts de poussière et d’obscurité, puis identifie ce second groupe.', 'R063': '83:7 et 83:18 introduisent les registres des débauchés et des pieux avec des constructions voisines. Leurs suites opposent le Feu et les délices.', 'R064': '84:7 et 84:10 reprennent l’introduction du livre remis, à droite ou derrière le dos. Compte facile et joie contrastent avec appel à la destruction et brasier.', 'R066': '88:2–7 décrit des visages accablés, le feu et une boisson brûlante. 88:8–16 présente des visages satisfaits, le jardin, une source courante et le repos.', 'R068': '91:9 associe la réussite à la purification de l’âme. 91:10 oppose l’échec à sa corruption dans une construction parallèle.', 'R069': '92:5–7 associe don, piété et confirmation à la voie facile. 92:8–10 oppose avarice, suffisance et dénégation, avec une issue difficile.', 'R071': '94:5 et 94:6 reprennent le même noyau difficulté et facilité. Le premier verset ajoute un fā initial : les versets entiers ne sont pas identiques.', 'R073': '98:6 et 98:7 qualifient deux groupes de pires et de meilleurs de la création. Le Feu de 98:6 contraste avec les jardins et la récompense de 98:8.', 'R074': '99:7 et 99:8 reprennent la même condition du poids infime d’une action et la même annonce de la voir. Les termes bien et mal sont opposés.', 'R075': '101:6–7 et 101:8–9 opposent balances lourdes et légères, puis vie agréable et abîme. 101:10–11 explicite ce dernier par le feu.', 'R078': '109:2–5 alterne les positions grammaticales de je et vous. 109:3 et 109:5 sont identiques, mais l’ensemble n’est pas un dialogue entre deux locuteurs.', 'R079': '8:4 et 8:74 reprennent vrais croyants, pardon et subsistance généreuse. Cette reprise relie les ensembles initial et final de la sourate selon al-Suyūṭī, sans identité intégrale des deux versets.', 'R083': '62:9 demande de délaisser le commerce à l’appel de la prière. 62:11 décrit la dispersion vers commerce et divertissement en laissant le Prophète debout. 62:10 permet le commerce après la prière.', 'R092': '73:1 et 74:1 commencent par le même vocatif, avec deux épithètes différentes du vêtement. Jalalayn identifie le Prophète dans les deux passages.', 'R094': '24:35 présente la lumière sur lumière dans l’image de la lampe. 24:40 oppose les ténèbres superposées et l’absence de lumière, sans épuiser l’interprétation du premier verset.', 'R096': '29:2 et 29:10 confrontent deux déclarations de foi à l’épreuve. 29:3 et 29:11 annoncent la distinction des groupes avec une formule de connaissance reprise.', 'R098': '47:1 et 47:2 opposent les actions perdues des négateurs à l’effacement des fautes et au redressement des croyants. 47:3 revient aux deux groupes en explicitant faux et vrai.', 'R102': '60:8 décrit le non-combat et la non-expulsion ; 60:9 évoque combat, expulsion et soutien à l’expulsion. Les constructions négative et affirmative portent sur des conduites différentes, sans règle nouvelle déduite du seul rapprochement.', 'R106': '78:21 et 78:31 annoncent deux séjours pour les rebelles et les pieux. Les boissons et les rétributions diffèrent, avec reprise du terme rétribution en 78:26 et 78:36.'}

FOCUS={'R025':['89:17','89:18','90:14','90:15','90:16'], 'R026':['91:3','91:4','92:1','92:2'], 'R035':['9:67','9:71','9:68','9:72'], 'R040':['26:9','26:68','26:104','26:122','26:140','26:159','26:175','26:191'], 'R044':['39:71','39:73'], 'R049':['56:7','56:8','56:9','56:10','56:88','56:91','56:92'], 'R053':['65:2','65:4','65:5'], 'R054':['66:10','66:11','66:12'], 'R063':['83:7','83:18'], 'R070':['93:6','93:9','93:7','93:11','93:8','93:10'], 'R078':['109:2','109:3','109:4','109:5'], 'R079':['8:4','8:74'], 'R082':['38:17','38:30','38:25','38:40'], 'R093':['81:14','82:5'], 'R096':['29:2','29:10','29:3','29:11'], 'R098':['47:1','47:2']}
TYPES={'REPETITION':'Répétition','RESSEMBLANCE':'Parallélisme','OPPOSITION':'Opposition','INVERSION':'Inversion','PROGRESSION':'Progression','DEBUT_FIN_INCLUSIO':'Début–Fin','ALTERNANCE':'Alternance','NON_DEFINI':'Centre'}
CONDITIONS={
'R032':'Référence primaire exacte et tableau des cinq membres de 2:142–152 (bornes, appariements et objections de Sinai) absents des preuves accessibles. Ne pas confondre cette structure locale avec les sept ou neuf sections proposées pour la sourate entière.',
'R033':'Les neuf segments attribués à Farrin/Azaiez, leurs appariements et leurs positions de mots exactes ne sont pas reproduits dans les preuves accessibles. Les polygones du verset sont vérifiables, mais ne certifient pas les foyers infraverset. Admission et représentation interne bloquées ; aucune comparaison artificielle de deux exemplaires du même verset.'}
LIMITS={
'R025':'Contraste des conduites décrites, sans antithèse structurale globale ni récit d’actes déjà accomplis.',
'R026':'Inversion limitée à jour/nuit ; 91:1–2 reste contextuel.',
'R044':'39:75 conclut le jugement des deux groupes ; il n’est pas réservé aux pieux.',
'R053':'La condition répétée est infraverset ; le repère désigne le verset qui la contient et ne certifie pas l’identité du verset entier. 65:3 reste contextuel.',
'R078':'Une seule déclaration, avec alternance des sujets grammaticaux ; pas un dialogue à deux locuteurs.',
'R083':'62:10 permet le commerce après la prière ; aucune condamnation générale du commerce.',
'R094':'Contraste entre les images et leur vocabulaire, sans réduire l’interprétation de 24:35 à ce seul contraste.',
'R102':'Bienveillance/justice et alliance sont des conduites distinctes dans les circonstances décrites ; aucune règle nouvelle n’est déduite de ce rapprochement.'}
BIB={
'R007':('al-Suyūṭī, Asrār tartīb al-Qurʾān, section Hūd','https://www.islamweb.net/ar/library/content/223/12/سورة-هود'),
'R008':('al-Suyūṭī, Asrār tartīb al-Qurʾān, section Yūsuf, pagination du site p.109','https://www.islamweb.net/ar/library/content/223/13/سورة-يوسف'),
'R049':('al-Suyūṭī, Marāṣid al-Maṭāliʿ, éd. al-ʿAskar, Dār al-Minhāj, 2005, p.69','https://quranpedia.app/books/b38158/s4.html'),
'R051':('al-Suyūṭī, Marāṣid al-Maṭāliʿ, éd. al-ʿAskar, 2005, p.70','https://quranpedia.app/books/b38158/s4.html'),
'R057':('al-Suyūṭī, Marāṣid al-Maṭāliʿ, éd. al-ʿAskar, 2005, p.75','https://quranpedia.app/books/b38158/s4.html'),
'R079':('al-Suyūṭī, Marāṣid al-Maṭāliʿ, éd. al-ʿAskar, 2005, p.51','https://quranpedia.app/books/b38158/s1.html')}

def comparisons(id, refs, typ, passages):
    # Explicit semantic edges prevent a multi-member opposition/beginning-end
    # from turning into an all-to-all comparison of unrelated same-side anchors.
    edges={
        'R025':[('89:17','90:15'),('89:18','90:14'),('89:18','90:16')],
        'R026':[('91:3','92:2'),('91:4','92:1')],
        'R035':[('9:67','9:71'),('9:68','9:72')],
        'R049':[('56:7','56:88'),('56:7','56:91'),('56:7','56:92'),('56:8','56:91'),('56:9','56:92'),('56:10','56:88')],
        'R054':[('66:10','66:11'),('66:10','66:12')],
        'R070':[('93:6','93:9'),('93:7','93:11'),('93:8','93:10')],
        'R082':[('38:17','38:30'),('38:25','38:40')],
        'R096':[('29:2','29:10'),('29:3','29:11')]
    }
    if id in edges:
        result={r:[] for r in refs}
        for a,b in edges[id]:
            result[a].append(b);result[b].append(a)
        return result
    groups={}
    for n, span in enumerate(passages.split(';')):
        match=re.search(r'(\d+):(\d+)(?:[–-](\d+))?',span)
        if match:
            surah,first,last=map(int,[match[1],match[2],match[3] or match[2]])
            for a in range(first,last+1):groups[str(surah)+':'+str(a)]=n
    partition=typ in ('Opposition','Inversion','Début–Fin','Parallélisme') and len(set(groups.get(r) for r in refs))>1
    return {r:[refs[(i+n)%len(refs)] for n in range(1,len(refs))
               if not partition or groups[r]!=groups[refs[(i+n)%len(refs)]]]
            for i,r in enumerate(refs)}

def build():
    raw=(BASE/'registre-documentaire-110.json').read_bytes(); old=json.loads(raw)
    geometry=json.loads((ROOT/'app/src/main/assets/reader109/geometry.json').read_text())
    # Original SVG path geometry is measured, never synthesized from text lengths.
    js=r'''const fs=require('fs'),z=require('zlib');let result={};for(let p=1;p<=604;p++){let s=z.brotliDecompressSync(fs.readFileSync('app/src/main/assets/mushaf/hafs/kfqc/svg-br/'+String(p).padStart(3,'0')+'.svg.br')).toString();for(const tag of s.match(/<path\b[^>]*class="ayahPolygon"[^>]*>/g)||[]){const a=k=>(tag.match(new RegExp('(?:^|\\s)'+k+'="([^"]*)"'))||[])[1];const ref=a('surah')+':'+a('ayah');(result[ref]??=[]).push({folio:p,viewBox:'0 0 345 550',path:a('d')});}}process.stdout.write(JSON.stringify(result));'''
    polygons=json.loads(subprocess.check_output(['node','-e',js],cwd=ROOT))
    dossiers=[]
    for d in old['dossiers']:
        if d['decisionDocumentaire']=='EXCLURE':continue
        id=d['id'];typ=TYPES[d['typeHistorique']];passages=d['versets'];explanation=d['justification']
        if id in OVERRIDES:typ,passages,explanation=OVERRIDES[id]
        refs=FOCUS.get(id) or re.findall(r'(\d+:\d+)',passages)
        refs=list(dict.fromkeys(refs))
        sources=[{'titre':'Texte coranique arabe, '+ref, 'url':'https://quran.com/'+ref.replace(':','/'), 'portee':'Passage primaire ; ne constitue pas une attribution littéraire savante.'} for ref in refs]
        if id in BIB:
            title,url=BIB[id];sources.insert(0,{'titre':title,'url':url,'portee':'Rapprochement précis consigné dans les preuves J3 existantes ; pagination imprimée seulement pour Marāṣid.'})
        if 'Jalalayn' in explanation or id in ('R030','R037','R047','R048'):
            sources.append({'titre':'al-Maḥallī / al-Suyūṭī, Tafsīr al-Jalālayn, commentaires des références indiquées','url':'https://rissc.jo/wp-content/uploads/2018/11/Tafsir-al-Jalalayn-ebook.pdf','portee':'Traduction Feras Hamza, 2008, ISBN 9781891785160. Appui exégétique des foyers explicitement cités dans la fiche.'})
        if id=='R070':sources.append({'titre':'Synthèse adaptée d’Ibn ʿĀshūr, contexte des bienfaits et de la gratitude','url':'https://quran.com/surah/93/info','portee':'Contexte général seulement ; ne démontre pas les trois appariements proposés.'})
        if id in CONDITIONS:
            sources.append({'titre':'Attribution historique Farrin 2010 ; référence détaillée à compléter','url':'https://doi.org/10.1111/j.1478-1913.2009.01299.x','portee':'R032 : Farrin, Surat al-Baqara: A Structural Analysis, The Muslim World 100 (2010). Ne certifie pas les bornes locales ni l’attribution de R033.'})
        if id in REFRAIN_SOURCES:
            sources.extend(REFRAIN_SOURCES[id])
            extra={
              'R047':' Al-Qurṭubī explique le retour de cet appel au rappel à travers les récits. La frise éditoriale distingue le récit précédent et le début du suivant, jusqu’à Pharaon après 54:40.',
              'R048':' Al-Baghawī explique le rappel renouvelé des bienfaits. La frise éditoriale situe les 31 retours parmi les thèmes successifs, sans attribuer ses stations au commentateur.',
              'R059':' Al-Qurṭubī explique le retour de l’avertissement selon les dénégations. La frise éditoriale distingue les dix contextes et conserve la clôture 77:50.'
            }
            explanation+=extra[id]
        anchors=[]
        for ref in refs:
            assert ref in polygons,(id,ref)
            for poly in polygons[ref]:
                assert poly['folio'] in geometry['verses'][ref]
                anchors.append({'verset':ref,**poly,'polygonSha256':hashlib.sha256(poly['path'].encode()).hexdigest()})
        limit=LIMITS.get(id,'La conclusion se limite au mécanisme décrit et aux références indiquées ; aucune structure globale ni identité intégrale supplémentaire n’est déduite.')
        if id in OVERRIDES:limit=explanation.split('. ')[-1]
        dossiers.append({'id':id,'titre':TITLES[id],'categorie':typ,'passages':passages,'bornesHistoriques':d['bornesHistoriques'],'reperes':refs,'comparaisons':comparisons(id,refs,typ,passages),'correspondance':explanation,'explication':explanation,'sources':sources,'enBref':BRIEFS.get(id,' '.join(re.split(r'(?<=[.!?])\s+',explanation)[:3])),'limites':limit,'ancresQcf':anchors,'statut':'CONDITIONNEL' if id in CONDITIONS else 'ADMIS','conditionManquante':CONDITIONS.get(id,''),'preuveAnterieure':d['justification'],'statutValidationVisuelle':'À vérifier sur la matrice Android du commit intégré. Les polygones sont contrôlés par programme.'})
    doc={'version':1,'date':'2026-11-05','portee':'Surcouche éditoriale additive autorisée : observation textuelle ou attribution limitée, sans certification académique collective. Les statuts ADMIS portent sur le contenu ; la clôture technique exige les tests Android du commit intégré.','registreHistoriqueSha256':hashlib.sha256(raw).hexdigest(),'sourcesReutilisees':['registre-documentaire-110.json','pilotes-preuves-20261105.json','docs/work-checkpoints/DELIVERY_2026_10_20/CORRESPONDANCES_J3_TRIAGE_COMPLET_110.md','docs/work-checkpoints/DELIVERY_2026_10_20/CORRESPONDANCES_J3_14_REPETITIONS_DEUX_TEMOINS.md','docs/work-checkpoints/DELIVERY_2026_10_20/CORRESPONDANCES_J3_59_REPETITIONS_OPPOSITIONS_PROGRESSIONS.md','docs/work-checkpoints/DELIVERY_2026_10_20/CORRESPONDANCES_J3_83_CLASSIFICATIONS_CRITIQUES.md','docs/work-checkpoints/DELIVERY_2026_10_20/CORRESPONDANCES_J3_MARASID_8_SOURCE_COLLATED.md'],'exclusionsPreservees':[d['id'] for d in old['dossiers'] if d['decisionDocumentaire']=='EXCLURE'],'bilan':{'fiches':62,'admises':60,'conditionnelles':2,'excluesPreservees':48},'dossiers':dossiers}
    (BASE/'catalogue-62.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n')
    quote=lambda s:json.dumps(s,ensure_ascii=False)
    lines=['package com.quransafeguard.hifz.preview;','','import com.quransafeguard.hifz.core.VerseRef;','import java.util.Arrays;','import java.util.Collections;','import java.util.List;','','/** Generated from catalogue-62.json. Conditional and excluded records are never runtime entries. */','final class CorrespondenceCatalogue {','    private CorrespondenceCatalogue() {}','    static final List<CorrespondenceCases.Entry> ADMITTED=Collections.unmodifiableList(Arrays.asList(']
    entries=[]
    for d in dossiers:
        if d['statut']!='ADMIS':continue
        source='\n\n'.join(s['titre']+'\n'+s['url']+'\n'+s['portee'] for s in d['sources'])
        mechanism='Passages : '+d['passages']+'\n\n'+d['explication']+'\n\nLimites : '+d['limites']+'\n\nLes repères QCF désignent les versets cités, sans surlignage des seuls mots du motif.'
        args=[d['id'],d['categorie'],d['titre'],mechanism,source,d['enBref']]
        refs=','.join('new VerseRef('+ref.replace(':',',')+')' for ref in d['reperes'])
        matrix='new int[][]{'+','.join('{'+','.join(str(d['reperes'].index(r)) for r in d['comparaisons'][ref])+'}' for ref in d['reperes'])+'}'
        entries.append('        new CorrespondenceCases.Entry('+','.join(map(quote,args))+','+matrix+','+refs+')')
    # Explicitly reviewed 1.17.12 addenda are isolated from the frozen 110 registry.
    addendum=json.loads((BASE/'additions-11712.json').read_text())
    existing_ids={d['id'] for d in old['dossiers']}
    for extra in addendum['dossiers']:
        id=extra['id']
        assert id not in existing_ids and id not in {x['id'] for x in dossiers},id
        assert extra['statut']=='ADMIS',id
        refs=extra['reperes']
        assert len(refs)>=2 and len(refs)==len(set(refs)),id
        for ref in refs:
            assert ref in polygons and extra['folioQcfAttendu'] in geometry['verses'][ref],(id,ref)
            assert extra['folioQcfAttendu'] in {p['folio'] for p in polygons[ref]},(id,ref)
            assert extra['comparaisons'][ref] and all(x in refs and ref in extra['comparaisons'][x] for x in extra['comparaisons'][ref])
        assert all(source['url'].startswith('https://') for source in extra['sources'])
        source='\n\n'.join(src['titre']+'\n'+src['url']+'\n'+src['portee'] for src in extra['sources'])
        mechanism='Passages : '+extra['passages']+'\n\n'+extra['explication']+'\n\nLimites : '+extra['limites']+'\n\nLes repères QCF désignent les versets cités, sans surlignage des seuls mots du motif.'
        args=[id,extra['categorie'],extra['titre'],mechanism,source,extra['enBref']]
        partners='new int[][]{'+','.join('{'+','.join(str(refs.index(x)) for x in extra['comparaisons'][ref])+'}' for ref in refs)+'}'
        refs_code=','.join('new VerseRef('+ref.replace(':',',')+')' for ref in refs)
        entries.append('        new CorrespondenceCases.Entry('+','.join(map(quote,args))+','+partners+','+refs_code+')')
    lines+= [',\n'.join(entries),'    ));','}','']
    JAVA.write_text('\n'.join(lines))
    print('62 historical fiches, 60 historical admissions, 2 conditions, 48 historical exclusions; '+str(len(addendum['dossiers']))+' independently sourced additions')
if __name__=='__main__':build()
