#!/usr/bin/env python3
from __future__ import annotations
import argparse, collections, json, re, sqlite3
from pathlib import Path

ATTACHED_CALL_RE = re.compile(r"(?<!\d\.)(?<=[A-Za-zÀ-ÖØ-öø-ÿ\.,;!?…\)\]”’\"])(\d{1,3})(?![\d:%])")
SPECIAL_NUMBERS = {30,72,90,133,149,167,176,191,201,246,374}


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('db',type=Path); ap.add_argument('--report',type=Path,required=True); args=ap.parse_args()
    con=sqlite3.connect(args.db)
    rows=con.execute('SELECT id,surah,verse_start,verse_end,verse_translation,commentary FROM tafsir_entry ORDER BY id').fetchall()
    con.close()
    calls=[]; counts=collections.Counter(); target=[]
    for rid,s,a,b,tr,com in rows:
        for field,text in [('translation',tr),('commentary',com)]:
            for m in ATTACHED_CALL_RE.finditer(text):
                n=int(m.group(1)); counts[n]+=1
                calls.append({'id':rid,'surah':s,'start':a,'end':b,'field':field,'number':n,'context':text[max(0,m.start()-65):m.end()+65]})
        if 'We have not been firm' in com or 'tribulation.49' in com:
            target.append({'id':rid,'surah':s,'start':a,'end':b,'commentary':com})
    report={'attached_total':len(calls),'distinct_numbers':len(counts),'counts':dict(sorted(counts.items())),'special_samples':[c for c in calls if c['number'] in SPECIAL_NUMBERS][:80],'target':target,'all_calls_sample':calls[:100]}
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('attached_total',len(calls),'distinct_numbers',len(counts))
    print('special count',sum(1 for c in calls if c['number'] in SPECIAL_NUMBERS))
    for t in target:
        print('\nTARGET',t['surah'],t['start'],t['end'])
        print(t['commentary'])

if __name__=='__main__':main()
