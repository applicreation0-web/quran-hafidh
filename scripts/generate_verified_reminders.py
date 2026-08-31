#!/usr/bin/env python3
import json, os, urllib.request, urllib.parse, time

BASE="https://hadeethenc.com/api/v1"
OUT="app/src/main/assets/reminders/hadeethenc_snapshot.json"
TARGET=127
UA={"User-Agent":"QuranSafeguard-build/0.8"}

def get_json(url):
    req=urllib.request.Request(url,headers=UA)
    with urllib.request.urlopen(req,timeout=30) as r:
        return json.load(r)

def as_list(v):
    if isinstance(v,list): return v
    if isinstance(v,dict):
        for k in ("data","items","results"):
            if isinstance(v.get(k),list): return v[k]
    return []

cats=as_list(get_json(BASE+"/categories/list/?language=fr"))
ids=[]
seen=set()
for cat in cats:
    cid=str(cat.get("id","")).strip()
    if not cid: continue
    for page in (1,2,3):
        url=BASE+"/hadeeths/list/?language=fr&category_id="+urllib.parse.quote(cid)+"&page="+str(page)+"&per_page=100"
        try:
            rows=as_list(get_json(url))
        except Exception:
            break
        if not rows: break
        for row in rows:
            hid=str(row.get("id","")).strip()
            if hid and hid not in seen:
                seen.add(hid); ids.append(hid)
        if len(ids) >= 350: break
    if len(ids) >= 350: break

records=[]
for hid in ids:
    if len(records) >= TARGET: break
    try:
        fr=get_json(BASE+"/hadeeths/one/?language=fr&id="+urllib.parse.quote(hid))
        ar=get_json(BASE+"/hadeeths/one/?language=ar&id="+urllib.parse.quote(hid))
    except Exception:
        continue
    fr_text=str(fr.get("hadeeth") or fr.get("text") or "").strip()
    ar_text=str(ar.get("hadeeth") or ar.get("text") or "").strip()
    if not fr_text or not ar_text: continue
    grade=str(ar.get("grade") or fr.get("grade") or "").strip()
    low=grade.lower()
    if any(x in low for x in ("ضعيف","daif","dai'f","faible","weak")): continue
    ref=ar.get("reference") or ar.get("references") or fr.get("reference") or ""
    if isinstance(ref,(dict,list)):
        ref=json.dumps(ref,ensure_ascii=False,sort_keys=True)
    else:
        ref=str(ref)
    attribution=str(ar.get("attribution") or fr.get("attribution") or "Prophète Muhammad ﷺ").strip()
    records.append({
        "id":"he_"+hid,
        "type":"HADITH",
        "theme":"bonnes_moeurs",
        "arabicText":ar_text,
        "frenchText":fr_text,
        "author":"Prophète Muhammad ﷺ",
        "book":"Encyclopédie des hadiths traduits (HadeethEnc)",
        "reference":("HadeethEnc ID "+hid + (" • "+ref if ref else "")),
        "authenticity":grade or "Hadith authentifié dans HadeethEnc",
        "tags":["bonnes mœurs","comportement"],
        "sourceId":hid,
        "translationSource":"HadeethEnc.com"
    })
    time.sleep(0.03)

if len(records) < TARGET:
    raise SystemExit("Only %d verified records collected; need %d" % (len(records), TARGET))

os.makedirs(os.path.dirname(OUT),exist_ok=True)
with open(OUT,"w",encoding="utf-8") as f:
    json.dump(records[:TARGET],f,ensure_ascii=False,indent=2)
print("generated",len(records[:TARGET]),OUT)
