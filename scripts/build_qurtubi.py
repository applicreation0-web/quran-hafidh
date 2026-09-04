import fitz,re,sqlite3,hashlib,os,collections,sys
BASE=os.environ.get('QURTUBI_BASE','/mnt/data/tafsir-src')
OUT=os.environ.get('QURTUBI_OUT','/mnt/data/qurtubi_en.sqlite')
SOURCES=[('v1',f'{BASE}/qurtubi-v1.pdf'),('v2',f'{BASE}/qurtubi-v2.pdf'),('v3',f'{BASE}/qurtubi-v3.pdf'),('v4',f'{BASE}/qurtubi-v4.pdf')]
PURE_AR=re.compile(r'[\u0600-\u06ff\u0750-\u077f]')
NUM_START=re.compile(r'^(\d{1,3})\.?\s+')
INLINE_NUM=re.compile(r'\b(\d{1,3})\.?\s+(?=[A-Za-z\u2018\u201c])')

def norm_text(t): return t.replace('\u00ad','').replace('\uf0d6','').replace('\uf096','').replace('\uf0b7','').strip()
def is_arabic(t):
    chars=[c for c in t if c.isalpha()]
    return bool(chars) and sum(bool(PURE_AR.match(c)) for c in chars)/len(chars)>.45

def is_junk(t,y):
    s=t.strip()
    if not s:return True
    if re.fullmatch(r'\d+',s) and (y>540 or y<65): return True
    low=s.lower()
    if 'downloaded via sunniconnect' in low: return True
    if low.startswith('vol. ') and ('sūrat' in low or 'sūrah' in low): return True
    if low.startswith('vol. ') and 'āyah' in low: return True
    if 'ˈ˂' in s or s.startswith('ʤʢ') or s.startswith('ĒŀĠĨ') or 'ĴģĠŏĕ' in s: return True
    if any('\u0600' <= ch <= '\u06ff' for ch in s): return True
    if s in {'CONTENTS','TRANSLATOR’S NOTE','TRANSLATOR\'S NOTE'}: return True
    return False

def lines(doc):
    out=[]
    for pi,p in enumerate(doc):
        for b in p.get_text('dict')['blocks']:
            if 'lines' not in b:continue
            for line in b['lines']:
                spans=[x for x in line['spans'] if x['text'].strip()]
                if not spans: continue
                t=norm_text(''.join(x['text'] for x in line['spans']))
                if not t: continue
                x0,y0,*_=line['bbox']
                if is_junk(t,y0):continue
                out.append({'page':pi+1,'x':x0,'y':y0,'text':t,'font':spans[0]['font'],'size':spans[0]['size'],'bold':('Helvetica' in spans[0]['font'] and 'Bold' in spans[0]['font'])})
    return out

def is_translation_line(L, threshold=69): return L['bold'] and L['x']>=threshold and 8.7<=L['size']<=11.5

def normalize_lines(seq):
    if not seq:return ''
    paras=[]; buf=[]; prev=None
    for L in seq:
        t=L['text']
        if is_arabic(t): continue
        new_para=False
        if prev is not None and L['page']==prev['page'] and L['y']-prev['y']>22: new_para=True
        if new_para and buf: paras.append(' '.join(buf));buf=[]
        buf.append(t); prev=L
    if buf:paras.append(' '.join(buf))
    text='\n\n'.join(paras)
    text=re.sub(r'([A-Za-z])-\s+([a-z])',r'\1\2',text)
    text=re.sub(r'[ \t]+',' ',text); text=re.sub(r' *\n\n *','\n\n',text)
    return text.strip()

def parse_volume(tag,path):
    doc=fitz.open(path); ls=lines(doc)
    appendix_start_page={"v1":445,"v2":634,"v3":373,"v4":381}[tag]
    ls=[line for line in ls if line['page']<appendix_start_page]
    starts=[]
    for i,L in enumerate(ls):
        if is_translation_line(L) and NUM_START.match(L['text']): starts.append(i)
    proto=[]; stage=2 if tag in ('v1','v2') else (2 if tag=='v3' else 3); prev_num=None
    for si,idx in enumerate(starts):
        L=ls[idx]; n=int(NUM_START.match(L['text']).group(1))
        if tag=='v3' and prev_num is not None and prev_num>=254 and n<100: stage=3
        if tag=='v4' and prev_num is not None and prev_num>=96 and n<50: stage=4
        prev_num=n; next_idx=starts[si+1] if si+1<len(starts) else len(ls)
        j=idx; trans=[]
        while j<next_idx and is_translation_line(ls[j]): trans.append(ls[j]); j+=1
        translation=normalize_lines(trans); nums=[int(x) for x in INLINE_NUM.findall(translation)]
        if not nums or nums[0]!=n: nums=[n]+nums
        clean=[n]
        for x in nums[1:]:
            if x==clean[-1]+1: clean.append(x)
            elif x==clean[-1]: continue
        commentary=normalize_lines(ls[j:next_idx])
        proto.append({'tag':tag,'surah':stage,'start':clean[0],'end':clean[-1],'translation':translation,'commentary':commentary,'page':L['page']})

    rows=[]; pending=[]
    for r in proto:
        pending.append(r)
        if not r['commentary']: continue
        first=pending[0]; last=pending[-1]
        if any(x['surah']!=first['surah'] for x in pending):
            for x in pending[:-1]:
                if x['commentary']: rows.append(x)
            pending=[last]; first=last
        startv=first['start']; endv=last['end']; expected=startv; contiguous=True
        for x in pending:
            if x['start']!=expected: contiguous=False; break
            expected=x['end']+1
        if contiguous:
            rows.append({'tag':tag,'surah':first['surah'],'start':startv,'end':endv,'translation':' '.join(x['translation'] for x in pending),'commentary':last['commentary'],'page':first['page']})
        else: rows.append(last)
        pending=[]

    if tag=='v1':
        p8=doc[7].get_text('text').splitlines(); tr=[]
        for t in p8:
            t=norm_text(t)
            if re.match(r'^[1-7]\s+',t): tr.append(t)
        fat_lines=[x for x in ls if 9<=x['page']<=56]
        rows.insert(0,{'tag':'v1','surah':1,'start':1,'end':7,'translation':' '.join(tr),'commentary':normalize_lines(fat_lines),'page':8})

    if tag=='v3' and not any(r['surah']==3 and r['start']<=1<=r['end'] for r in rows):
        p=doc[209]; trans=[]
        for b in p.get_text('dict')['blocks']:
            if 'lines' not in b: continue
            for line in b['lines']:
                sp=[x for x in line['spans'] if x['text'].strip()]
                if not sp: continue
                t=norm_text(''.join(x['text'] for x in line['spans']))
                if 'Helvetica' in sp[0]['font'] and 'Bold' in sp[0]['font'] and ('1 Alif' in t or 'Sustaining' in t): trans.append(t)
        body=[x for x in ls if (x['page']>210 and x['page']<214)]
        rows.append({'tag':'v3','surah':3,'start':1,'end':2,'translation':' '.join(trans),'commentary':normalize_lines(body),'page':210})
        rows.sort(key=lambda r:(r['surah'],r['start'],r['page']))
    return rows

allrows=[]
for tag,path in SOURCES:
    if os.path.exists(path):
        rr=parse_volume(tag,path); print(tag,'rows',len(rr)); allrows.extend(rr)
counts=collections.Counter()
for r in allrows:
    key=(r['surah'],r['start'],r['end']);counts[key]+=1;r['segment_no']=counts[key]
if os.path.exists(OUT):os.remove(OUT)
con=sqlite3.connect(OUT)
con.executescript('''
PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF;
CREATE TABLE source_metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL);
CREATE TABLE tafsir_entry(
 id INTEGER PRIMARY KEY,
 surah INTEGER NOT NULL,
 verse_start INTEGER NOT NULL,
 verse_end INTEGER NOT NULL,
 segment_no INTEGER NOT NULL,
 verse_translation TEXT NOT NULL,
 commentary TEXT NOT NULL,
 source_page INTEGER NOT NULL,
 source_volume TEXT NOT NULL,
 UNIQUE(surah,verse_start,verse_end,segment_no)
);
CREATE INDEX idx_tafsir_lookup ON tafsir_entry(surah,verse_start,verse_end,segment_no);
''')
sha={tag:hashlib.sha256(open(path,'rb').read()).hexdigest() for tag,path in SOURCES if os.path.exists(path)}
meta={'schema_version':'2','edition_id':'qurtubi','display_name':'Qurtubi','author':'Abu Abdallah Muhammad ibn Ahmad al-Qurtubi','work':'al-Jami li-Ahkam al-Quran / The General Judgments of the Quran','translator':'Aisha Abdurrahman Bewley','language':'English','coverage_target':'Volumes 1-4: Al-Fatihah; Al-Baqarah 1-286; Ali Imran 1-200; An-Nisa 1-22. Quran 4:23 begins volume 5.','arabic_included':'false','source_pdf_sha256_json':str(sha),'entry_count':str(len(allrows)),'volume2_status':'full volume 2 materialized and parsed'}
con.executemany('INSERT INTO source_metadata VALUES (?,?)',meta.items())
con.executemany('''INSERT INTO tafsir_entry(surah,verse_start,verse_end,segment_no,verse_translation,commentary,source_page,source_volume) VALUES (?,?,?,?,?,?,?,?)''',[(r['surah'],r['start'],r['end'],r['segment_no'],r['translation'],r['commentary'],r['page'],r['tag']) for r in allrows])
con.commit();con.execute('VACUUM');con.close()

expected={1:7,2:286,3:200,4:22}; cov=collections.defaultdict(set)
for r in allrows:
    for a in range(r['start'],r['end']+1): cov[r['surah']].add(a)
for s,n in expected.items():
    miss=[a for a in range(1,n+1) if a not in cov[s]]
    print('sura',s,'covered',len(cov[s]),'/',n,'missing',len(miss),miss[:140])
print('ranges>',[(r['surah'],r['start'],r['end'],r['tag']) for r in allrows if r['end']>r['start']][:30])
print('sunniconnect rows',sum('sunniconnect' in (r['translation']+' '+r['commentary']).lower() for r in allrows))
print('arabic rows',sum(is_arabic(r['translation']) or is_arabic(r['commentary']) for r in allrows))
print('db bytes',os.path.getsize(OUT),'sha256',hashlib.sha256(open(OUT,'rb').read()).hexdigest())
for s,a in [(1,1),(2,61),(2,142),(2,143),(2,254),(2,275),(3,96),(4,11),(4,23)]:
    rs=[r for r in allrows if r['surah']==s and r['start']<=a<=r['end']]
    print('\ncheck',s,a,'rows',len(rs))
    for r in rs[:2]:print(r['start'],r['end'],r['tag'],r['page'],r['translation'][:140],'=>',r['commentary'][:180])
