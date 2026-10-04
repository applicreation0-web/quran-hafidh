import fitz,re,sqlite3,hashlib,os,collections,sys
BASE=os.environ.get('QURTUBI_BASE','/mnt/data/tafsir-src')
OUT=os.environ.get('QURTUBI_OUT','/mnt/data/qurtubi_en.sqlite')
SOURCES=[('v1',f'{BASE}/qurtubi-v1.pdf'),('v2',f'{BASE}/qurtubi-v2.pdf'),('v3',f'{BASE}/qurtubi-v3.pdf'),('v4',f'{BASE}/qurtubi-v4.pdf')]
EXPECTED_SOURCE_SHA256={
    'v1':'a791ec1313fa2401abe7ca25ac7ccb4bedb1afcb51f2c779160a71e98a6f04cb',
    'v2':'466e72af70ad6c9c9ddccb418f87df6012c3078ef7947fdc88ab00c86c15645e',
    'v3':'e69818ce49f79d7de2bb5cef37c82e7e1f4431f7117a550fa33259da7dc6b583',
    'v4':'eb71cb2ed8c2497cc8a5d3634b3eeb7788fdc7caee9de5d6b50349fb8619965c',
}
EXPECTED_COVERAGE={1:7,2:286,3:200,4:22}
ARABIC_SCRIPT=re.compile(r'[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]')
NUM_START=re.compile(r'^(\d{1,3})\.?\s+')
INLINE_NUM=re.compile(r'\b(\d{1,3})\.?\s+(?=[A-Za-z\u2018\u201c])')
SYMBOL_FONT='KFGQPCArabicSymbols01'

SOURCE_SYMBOL_MAP={
    'v1':{
        'g':'ﷺ', 'f':'ﷺ', 'n':'عليه السلام', 'p':'عليهم السلام',
        'h':'رضي الله عنه', 'i':'رضي الله عنها', 'k':'رضي الله عنهما',
    },
    'v2':{
        'g':'ﷺ', 'f':'ﷺ', 'c':'سبحانه وتعالى', 'n':'عليه السلام',
        'p':'عليهم السلام', 'h':'رضي الله عنه', 'i':'رضي الله عنها',
    },
    'v3':{
        'g':'ﷺ', 'f':'ﷺ', 'c':'ﷺ', 'n':'عليه السلام',
    },
    'v4':{
        'g':'ﷺ', 'c':'ﷺ', 'n':'عليه السلام', 'h':'رضي الله عنه',
    },
}
RESTORED_HONORIFICS=tuple(sorted({v for m in SOURCE_SYMBOL_MAP.values() for v in m.values()}, key=len, reverse=True))
SYMBOL_COUNTS=collections.Counter()

def file_sha256(path):
    h=hashlib.sha256()
    with open(path,'rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''): h.update(chunk)
    return h.hexdigest()

def norm_text(t): return t.replace('\u00ad','').replace('\u00a0',' ').replace('\uf0d6','').replace('\uf096','').replace('\uf0b7','').strip()

def decode_source_spans(tag,spans):
    out=[]
    mapping=SOURCE_SYMBOL_MAP[tag]
    for span in spans:
        text=span['text']
        if span.get('font')!=SYMBOL_FONT:
            out.append(text)
            continue
        restored=[]
        for ch in text:
            if ch.isspace():
                restored.append(ch)
                continue
            value=mapping.get(ch)
            if value is None:
                raise RuntimeError(f'Qurtubi {tag} unknown source symbol-font key {ch!r}')
            restored.append(value)
            SYMBOL_COUNTS[(tag,ch,value)]+=1
        out.append(''.join(restored))
    return norm_text(''.join(out))

def is_arabic(t):
    chars=[c for c in t if c.isalpha()]
    return bool(chars) and sum(bool(ARABIC_SCRIPT.match(c)) for c in chars)/len(chars)>.45

def without_restored_honorifics(text):
    value=text
    for mark in RESTORED_HONORIFICS:
        value=value.replace(mark,'')
    return value

def is_junk(t,y):
    s=t.strip()
    if not s:return True
    if re.fullmatch(r'\d+',s) and (y>540 or y<65): return True
    low=s.lower()
    if 'downloaded via sunniconnect' in low: return True
    if low.startswith('vol. ') and ('sūrat' in low or 'sūrah' in low): return True
    if low.startswith('vol. ') and 'āyah' in low: return True
    if 'ˈ˂' in s or s.startswith('ʤʢ') or s.startswith('ĒŀĠĨ') or 'ĴģĠŏĕ' in s: return True
    if is_arabic(without_restored_honorifics(s)): return True
    if s in {'CONTENTS','TRANSLATOR’S NOTE','TRANSLATOR\'S NOTE'}: return True
    return False

def lines(doc,tag):
    out=[]
    for pi,p in enumerate(doc):
        for b in p.get_text('dict')['blocks']:
            if 'lines' not in b:continue
            for line in b['lines']:
                spans=[x for x in line['spans'] if x['text'].strip()]
                if not spans: continue
                t=decode_source_spans(tag,spans)
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
        if is_arabic(without_restored_honorifics(t)): continue
        new_para=False
        if prev is not None and L['page']==prev['page'] and L['y']-prev['y']>22: new_para=True
        if new_para and buf: paras.append(' '.join(buf));buf=[]
        buf.append(t); prev=L
    if buf:paras.append(' '.join(buf))
    text='\n\n'.join(paras)
    text=re.sub(r'([A-Za-z])-\s+([a-z])',r'\1\2',text)
    text=re.sub(r'[ \t]+',' ',text); text=re.sub(r' *\n\n *','\n\n',text)
    return text.strip()

def strip_display_verse_markers(text, verse_numbers, *, strict=True):
    """Remove only source verse-number labels already used for row indexing.

    Genuine numeric content and commentary references are untouched. The exact
    sequential labels supplied by the printed translation are removed once,
    in order, solely from the displayed English verse translation.
    """
    expected=list(verse_numbers)
    if not expected:
        return text
    marker_spans=[]
    next_index=0
    for match in INLINE_NUM.finditer(text):
        if next_index>=len(expected): break
        if int(match.group(1))==expected[next_index]:
            marker_spans.append(match.span())
            next_index+=1
    if strict and next_index!=len(expected):
        raise RuntimeError(
            f'Qurtubi could not locate every indexed verse label in translation: '
            f'expected={expected} found={next_index} text={text[:180]!r}'
        )
    value=text
    for start,end in reversed(marker_spans):
        value=value[:start]+value[end:]
    value=re.sub(r'[ \t]+',' ',value)
    value=re.sub(r' *\n\n *','\n\n',value)
    return value.strip()

def parse_volume(tag,path):
    doc=fitz.open(path); ls=lines(doc,tag)
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
        raw_translation=normalize_lines(trans)
        nums=[int(x) for x in INLINE_NUM.findall(raw_translation)]
        if not nums or nums[0]!=n: nums=[n]+nums
        clean=[n]
        for x in nums[1:]:
            if x==clean[-1]+1: clean.append(x)
            elif x==clean[-1]: continue
        translation=strip_display_verse_markers(raw_translation,clean)
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
        p8=doc[7].get_text('dict'); tr=[]
        for b in p8['blocks']:
            if 'lines' not in b: continue
            for line in b['lines']:
                sp=[x for x in line['spans'] if x['text'].strip()]
                if not sp: continue
                t=decode_source_spans(tag,sp)
                if re.match(r'^[1-7]\s+',t): tr.append(t)
        fat_lines=[x for x in ls if 9<=x['page']<=56]
        raw_fatiha=' '.join(tr)
        rows.insert(0,{'tag':'v1','surah':1,'start':1,'end':7,'translation':strip_display_verse_markers(raw_fatiha,range(1,8)),'commentary':normalize_lines(fat_lines),'page':8})

    if tag=='v3' and not any(r['surah']==3 and r['start']<=1<=r['end'] for r in rows):
        p=doc[209]; trans=[]
        for b in p.get_text('dict')['blocks']:
            if 'lines' not in b: continue
            for line in b['lines']:
                sp=[x for x in line['spans'] if x['text'].strip()]
                if not sp: continue
                t=decode_source_spans(tag,sp)
                if 'Helvetica' in sp[0]['font'] and 'Bold' in sp[0]['font'] and ('1 Alif' in t or 'Sustaining' in t): trans.append(t)
        body=[x for x in ls if (x['page']>210 and x['page']<214)]
        raw_special=' '.join(trans)
        rows.append({'tag':'v3','surah':3,'start':1,'end':2,'translation':strip_display_verse_markers(raw_special,[1,2],strict=False),'commentary':normalize_lines(body),'page':210})
        rows.sort(key=lambda r:(r['surah'],r['start'],r['page']))
    return rows

for tag,path in SOURCES:
    if not os.path.isfile(path):
        raise RuntimeError(f'Missing required Qurtubi source volume: {tag} ({path})')
    actual=file_sha256(path)
    if actual!=EXPECTED_SOURCE_SHA256[tag]:
        raise RuntimeError(f'Qurtubi {tag} source SHA-256 mismatch: {actual}')

allrows=[]
for tag,path in SOURCES:
    rr=parse_volume(tag,path)
    if not rr:
        raise RuntimeError(f'Qurtubi {tag} produced no Tafsir rows')
    print(tag,'rows',len(rr)); allrows.extend(rr)

if not SYMBOL_COUNTS:
    raise RuntimeError('Qurtubi source-symbol restoration found no KFGQPC symbols')
print('restored source symbols',sum(SYMBOL_COUNTS.values()),sorted(SYMBOL_COUNTS.items()))

for r in allrows:
    last=EXPECTED_COVERAGE.get(r['surah'])
    if last is None or r['start']<1 or r['end']<r['start'] or r['end']>last:
        raise RuntimeError(f'Qurtubi row outside approved volumes 1-4 scope: {r}')
    if not r['translation'].strip() or not r['commentary'].strip():
        raise RuntimeError(f'Qurtubi empty translation/commentary row: {r["surah"]}:{r["start"]}-{r["end"]} {r["tag"]}')
    if NUM_START.match(r['translation']):
        raise RuntimeError(f'Qurtubi visible leading verse label survived: {r["surah"]}:{r["start"]}-{r["end"]}')
    joined=r['translation']+' '+r['commentary']
    if '\u00a0' in joined:
        raise RuntimeError(f'Qurtubi non-breaking-space extraction debris: {r["surah"]}:{r["start"]}-{r["end"]}')
    if is_arabic(without_restored_honorifics(joined)):
        raise RuntimeError(f'Qurtubi Arabic source text leaked into row: {r["surah"]}:{r["start"]}-{r["end"]}')
    if 'sunniconnect' in joined.lower():
        raise RuntimeError(f'Qurtubi scan contamination leaked into row: {r["surah"]}:{r["start"]}-{r["end"]}')

cov=collections.defaultdict(set)
for r in allrows:
    for a in range(r['start'],r['end']+1): cov[r['surah']].add(a)
for s,n in EXPECTED_COVERAGE.items():
    miss=[a for a in range(1,n+1) if a not in cov[s]]
    if miss:
        raise RuntimeError(f'Qurtubi volumes 1-4 coverage gap in sura {s}: {miss[:140]}')

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
sha={tag:file_sha256(path) for tag,path in SOURCES}
symbol_meta={f'{tag}:{key}':count for (tag,key,_value),count in sorted(SYMBOL_COUNTS.items())}
meta={'schema_version':'2','edition_id':'qurtubi','display_name':'Qurtubi','author':'Abu Abdallah Muhammad ibn Ahmad al-Qurtubi','work':'al-Jami li-Ahkam al-Quran / The General Judgments of the Quran','translator':'Aisha Abdurrahman Bewley','language':'English','coverage_target':'Volumes 1-4: Al-Fatihah; Al-Baqarah 1-286; Ali Imran 1-200; An-Nisa 1-22. Quran 4:23 begins volume 5.','arabic_included':'false','source_pdf_sha256_json':str(sha),'source_honorific_glyphs_restored':'true','source_symbol_key_counts':str(symbol_meta),'entry_count':str(len(allrows)),'volume2_status':'full volume 2 materialized and parsed','verse_marker_display_policy':'indexed-source-verse-labels-hidden-in-translation'}
con.executemany('INSERT INTO source_metadata VALUES (?,?)',meta.items())
con.executemany('''INSERT INTO tafsir_entry(surah,verse_start,verse_end,segment_no,verse_translation,commentary,source_page,source_volume) VALUES (?,?,?,?,?,?,?,?)''',[(r['surah'],r['start'],r['end'],r['segment_no'],r['translation'],r['commentary'],r['page'],r['tag']) for r in allrows])
con.commit();con.execute('VACUUM');con.close()

for s,n in EXPECTED_COVERAGE.items():
    miss=[a for a in range(1,n+1) if a not in cov[s]]
    print('sura',s,'covered',len(cov[s]),'/',n,'missing',len(miss),miss[:140])
print('ranges>',[(r['surah'],r['start'],r['end'],r['tag']) for r in allrows if r['end']>r['start']][:30])
print('sunniconnect rows',sum('sunniconnect' in (r['translation']+' '+r['commentary']).lower() for r in allrows))
print('arabic source rows',sum(bool(is_arabic(without_restored_honorifics(r['translation']+' '+r['commentary']))) for r in allrows))
print('visible leading translation labels',sum(bool(NUM_START.match(r['translation'])) for r in allrows))
print('db bytes',os.path.getsize(OUT),'sha256',hashlib.sha256(open(OUT,'rb').read()).hexdigest())
for s,a in [(1,1),(2,21),(2,61),(2,142),(2,143),(2,254),(2,275),(3,96),(4,11),(4,23)]:
    rs=[r for r in allrows if r['surah']==s and r['start']<=a<=r['end']]
    print('\ncheck',s,a,'rows',len(rs))
    for r in rs[:2]:print(r['start'],r['end'],r['tag'],r['page'],r['translation'][:180],'=>',r['commentary'][:180])
