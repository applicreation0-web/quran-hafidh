import fitz,re,sqlite3,json,hashlib,gzip,os,collections
PDF=os.environ.get('QUSHAYRI_PDF','/mnt/data/tafsir-src/lataif.pdf')
OUT=os.environ.get('QUSHAYRI_OUT','/mnt/data/qushayri_en.sqlite')
anchor_re=re.compile(r'^\[(\d+):(\d+)(?:[\u2013\u2014-](\d+))?\]\s*')
header_re=re.compile(r'^(Subtle Allusions\s+\[|Laṭāʾif al-ishārāt\s+\[|\d+\s*\|\s*•|•\s*Laṭāʾif)')

def line_info(line):
    spans=line['spans']; text=''.join(s['text'] for s in spans).replace('\u00ad','')
    fonts=[s['font'] for s in spans if s['text'].strip()]
    sizes=[s['size'] for s in spans if s['text'].strip()]
    return text,fonts,sizes

def is_arabic_line(fonts,text):
    if any('KFGQPC' in f or 'Arabic' in f for f in fonts):
        return not anchor_re.match(text.strip())
    chars=[c for c in text if not c.isspace()]
    if chars:
        ar=sum('\u0600'<=c<='\u06ff' or '\u0750'<=c<='\u077f' for c in chars)
        if ar/len(chars)>.55: return True
    return False

def starts_italic(fonts):
    return bool(fonts) and 'Italic' in fonts[0]

def normalize(parts):
    out=[]; buf=[]
    for p in parts:
        if p is None:
            if buf:
                out.append(' '.join(buf).strip()); buf=[]
            continue
        t=p.strip()
        if not t: continue
        buf.append(t)
    if buf: out.append(' '.join(buf).strip())
    text='\n\n'.join(x for x in out if x)
    text=re.sub(r'([A-Za-z])\-\s+([a-z])',r'\1\2',text)
    text=re.sub(r'[ \t]+',' ',text)
    text=re.sub(r' *\n\n *','\n\n',text)
    return text.strip()

doc=fitz.open(PDF)
segments=[]
current=None
for pi in range(37,506):
    page=doc[pi]
    pd=page.get_text('dict')
    page_lines=[]
    for bi,b in enumerate(pd['blocks']):
        if 'lines' not in b: continue
        for li,line in enumerate(b['lines']):
            text,fonts,sizes=line_info(line)
            x0,y0,x1,y1=line['bbox']
            st=text.strip()
            if not st: continue
            if re.fullmatch(r'\d+',st): continue
            if st.startswith('Subtle Allusions ') or st.startswith('Laṭāʾif al-ishārāt '): continue
            if is_arabic_line(fonts,st): continue
            if sizes and max(sizes) <= 9.4: continue
            page_lines.append((bi,li,st,fonts,sizes,y0))
    prev_block=None
    for bi,li,st,fonts,sizes,y0 in page_lines:
        m=anchor_re.match(st)
        if m:
            if current:
                current['commentary']=normalize(current['body'])
                current['translation']=normalize(current['translation_parts'])
                del current['body']; del current['translation_parts']
                segments.append(current)
            s=int(m.group(1)); a1=int(m.group(2)); a2=int(m.group(3) or a1)
            rest=st[m.end():].strip()
            current={'surah':s,'start':a1,'end':a2,'translation_parts':[],'body':[],'page':pi+1}
            if rest: current['translation_parts'].append(rest)
            prev_block=bi
            continue
        if not current: continue
        if not current['body'] and starts_italic(fonts):
            current['translation_parts'].append(st)
        else:
            if prev_block is not None and bi != prev_block and current['body']:
                current['body'].append(None)
            current['body'].append(st)
        prev_block=bi
if current:
    current['commentary']=normalize(current['body']); current['translation']=normalize(current['translation_parts'])
    del current['body']; del current['translation_parts']; segments.append(current)

if not any(s['surah']==2 and s['start']<=68<=s['end'] for s in segments):
    p68=doc[118]
    lines=[]
    for b in p68.get_text('dict')['blocks']:
        if 'lines' not in b: continue
        for line in b['lines']:
            text,fonts,sizes=line_info(line)
            st=text.replace('\u00ad','').strip()
            if not st or is_arabic_line(fonts,st): continue
            if sizes and max(sizes)<=9.4: continue
            lines.append((st,fonts))
    start=next(i for i,(t,_) in enumerate(lines) if 'When He said: “She is a cow neither old' in t)
    end=next(i for i,(t,_) in enumerate(lines[start:],start) if 'yet retains some of the vigor of his youth.' in t)
    raw=' '.join(t for t,_ in lines[start:end+1])
    raw=re.sub(r'([A-Za-z])-\s+([a-z])',r'\1\2',raw)
    raw=re.sub(r'\s+',' ',raw).strip()
    m=re.search(r'When He said: “(?P<tr>.+?)” \[2:68\], it meant (?P<com>.+)$',raw)
    if not m: raise RuntimeError('Could not extract Qushayri 2:68 source exception')
    segments.append({'surah':2,'start':68,'end':68,'translation':m.group('tr').strip(),'commentary':('It meant '+m.group('com').strip()),'page':119})

segments=[s for s in segments if 1<=s['surah']<=4 and s['translation']]
segments.sort(key=lambda s:(s['surah'],s['start'],s['page']))
counts=collections.Counter(); rows=[]
for s in segments:
    key=(s['surah'],s['start'],s['end']); counts[key]+=1
    s['segment_no']=counts[key]; rows.append(s)

if os.path.exists(OUT): os.remove(OUT)
con=sqlite3.connect(OUT)
con.executescript('''
PRAGMA journal_mode=OFF;
PRAGMA synchronous=OFF;
CREATE TABLE source_metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE tafsir_entry(
 id INTEGER PRIMARY KEY,
 surah INTEGER NOT NULL,
 verse_start INTEGER NOT NULL,
 verse_end INTEGER NOT NULL,
 segment_no INTEGER NOT NULL,
 verse_translation TEXT NOT NULL,
 commentary TEXT NOT NULL,
 source_page INTEGER NOT NULL,
 UNIQUE(surah,verse_start,verse_end,segment_no)
);
CREATE INDEX idx_tafsir_lookup ON tafsir_entry(surah,verse_start,verse_end,segment_no);
''')
meta={
 'schema_version':'2','edition_id':'qushayri','display_name':'Qushayri',
 'author':'Abu l-Qasim Abd al-Karim al-Qushayri','work':'Lataif al-Isharat / Subtle Allusions',
 'translator':'Kristin Zahra Sands','language':'English','coverage':'Suras 1-4','arabic_included':'false',
 'english_verse_translation_included':'true','source_pdf_sha256':hashlib.sha256(open(PDF,'rb').read()).hexdigest(),
 'entry_count':str(len(rows))
}
con.executemany('INSERT INTO source_metadata(key,value) VALUES (?,?)',meta.items())
con.executemany('INSERT INTO tafsir_entry(surah,verse_start,verse_end,segment_no,verse_translation,commentary,source_page) VALUES (?,?,?,?,?,?,?)',[(r['surah'],r['start'],r['end'],r['segment_no'],r['translation'],r['commentary'],r['page']) for r in rows])
con.commit(); con.execute('VACUUM'); con.close()

cov=collections.defaultdict(set)
for r in rows:
 for a in range(r['start'],r['end']+1): cov[r['surah']].add(a)
expected={1:7,2:286,3:200,4:176}
print('segments',len(rows))
for s,n in expected.items():
 missing=[a for a in range(1,n+1) if a not in cov[s]]
 print('sura',s,'covered',len(cov[s]),'missing',len(missing),missing[:25])
print('repeated exact anchors',sum(1 for v in counts.values() if v>1),'max segments',max(counts.values()))
print('db bytes',os.path.getsize(OUT),'sha256',hashlib.sha256(open(OUT,'rb').read()).hexdigest())
con=sqlite3.connect(OUT)
for s,a in [(2,36),(2,68),(3,55),(4,167),(4,176)]:
 rs=con.execute('SELECT verse_start,verse_end,segment_no,substr(verse_translation,1,180),substr(commentary,1,220),source_page FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=? ORDER BY id',(s,a,a)).fetchall()
 print('\n',s,a,'rows',len(rs))
 for x in rs[:3]: print(x)
con.close()
