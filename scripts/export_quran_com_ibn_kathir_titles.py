#!/usr/bin/env python3
"""Export ONLY the original h2 English Ibn Kathir headings served by Quran.com.

Quran.com public legacy content API, resource 169. No spa5k titles, no
paraphrasing, no generating, no inference of Al-Munir/QCF or Hifz boundaries.
Never commit the resulting data files to the public repository. The one-day
Actions artifact is intended only for the owner's private editorial audit.
"""
import argparse
import csv
import json
import os
import pathlib
import time
from html.parser import HTMLParser
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

RESOURCE_ID = 169
BASE = "https://api.quran.com/api/v4/tafsirs/169/by_chapter"
VERSE_COUNTS = [
  7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,
  98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,
  75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,
  13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,
  29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,
  9,5,4,7,3,6,3,5,4,5,6
]
assert len(VERSE_COUNTS)==114 and sum(VERSE_COUNTS)==6236

class H2Parser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.current = None
        self.headings = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() == "h2":
            self.current = []

    def handle_data(self, data):
        if self.current is not None:
            self.current.append(data)

    def handle_endtag(self, tag):
        if tag.lower() == "h2" and self.current is not None:
            title = "".join(self.current).strip()
            if title:
                self.headings.append(title)
            self.current = None

def fetch(url):
    request=Request(url,headers={
        "Accept":"application/json",
        "User-Agent":"QuranHaafidhPersonalEditorialAudit/1.0",
    })
    for attempt in range(6):
        try:
            with urlopen(request,timeout=40) as response:
                if response.status!=200:
                    raise RuntimeError(f"HTTP {response.status}: {url}")
                return json.load(response)
        except HTTPError as exc:
            if exc.code not in (429,500,502,503,504) or attempt==5:
                raise
            pause=min(45,3*(2**attempt))
            print(f"Temporary HTTP {exc.code}; retry in {pause}s",flush=True)
            time.sleep(pause)
        except (URLError,TimeoutError):
            if attempt==5:raise
            time.sleep(min(45,3*(2**attempt)))
    raise RuntimeError(f"Quran.com unavailable for {url}")

def main():
    arg=argparse.ArgumentParser()
    arg.add_argument("--output",required=True)
    ns=arg.parse_args()
    out=pathlib.Path(ns.output)
    out.mkdir(parents=True,exist_ok=True)
    rows=[]
    seen=set()
    verse_total=0
    requests=0
    for surah,expected_count in enumerate(VERSE_COUNTS,1):
        page=1
        chapter_count=0
        while True:
            url=f"{BASE}/{surah}?per_page=50&page={page}"
            response=fetch(url)
            items=response.get("tafsirs")
            if not isinstance(items,list) or not items:
                raise RuntimeError(f"Empty/unparseable Quran.com chapter: {url}")
            for item in items:
                if item.get("resource_id") != RESOURCE_ID:
                    raise RuntimeError(f"Wrong tafsir resource in {url}")
                verse=item.get("verse_key","")
                try:
                    s,a=map(int,verse.split(":"))
                except (ValueError,AttributeError):
                    raise RuntimeError(f"Bad Quran.com verse key: {verse!r}")
                if s!=surah or not(1<=a<=expected_count) or verse in seen:
                    raise RuntimeError(f"Misplaced/repeated Quran.com verse key: {verse}")
                seen.add(verse)
                chapter_count+=1
                body=item.get("text")
                if body is None or not isinstance(body,str):
                    raise RuntimeError(f"Missing textual field for {verse}")
                parsed=H2Parser()
                parsed.feed(body)
                for order,title in enumerate(parsed.headings,1):
                    rows.append({
                      "surah":surah,"ayah":a,"verse_key":verse,
                      "heading_number":order,"quran_com_heading_exact":title,
                      "quran_com_page":f"https://quran.com/{surah}/{a}/tafsirs/en-tafisr-ibn-kathir",
                      "quran_com_api_source":url,
                    })
            verse_total+=len(items)
            requests+=1
            pagination=response.get("pagination",{})
            next_page=pagination.get("next_page")
            if next_page is None:break
            if not isinstance(next_page,int) or next_page!=page+1:
                raise RuntimeError(f"Broken pagination in {url}: {next_page}")
            page=next_page
            time.sleep(0.12)
        if chapter_count!=expected_count:
            raise RuntimeError(f"Incomplete Quran.com chapter {surah}: {chapter_count}/{expected_count}")
        print(f"Sura {surah:03d}/114: {chapter_count} ayahs, "
              f"{sum(r['surah']==surah for r in rows)} original h2 headings",flush=True)
        time.sleep(0.15)
    if verse_total!=6236 or len(seen)!=6236:
        raise RuntimeError(f"Incomplete Quran.com corpus: {verse_total}/{len(seen)}")

    fields=["surah","ayah","verse_key","heading_number","quran_com_heading_exact",
            "quran_com_page","quran_com_api_source"]
    with (out/"QURAN_COM_IBN_KATHIR_TITRES_ANGLAIS_ORIGINAUX.csv").open(
        "w",encoding="utf-8-sig",newline="") as stream:
        writer=csv.DictWriter(stream,fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    summary={
        "source":"Quran.com official Tafsir Ibn Kathir (Abridged)",
        "source_resource_id":169,
        "source_api":BASE,
        "surahs_checked":114,"ayah_records_checked":verse_total,
        "headings_copied_verbatim":len(rows),
        "api_requests":requests,
        "method":"HTML h2 only; title text unmodified beyond incidental surrounding whitespace; "
                 "multiple h2 per verse preserved in original order",
        "no_title_ayah_records":verse_total-len({r["verse_key"] for r in rows}),
        "NOT_PROVEN":"1903 group alignment, editorial contextual relevance, reproduction rights",
        "app_code_modified":False,"hifz_engines_modified":False,
    }
    with (out/"SOURCE_ET_CONTROLES.json").open("w",encoding="utf8") as stream:
        json.dump(summary,stream,ensure_ascii=False,indent=2)
    print("FINISHED: "+json.dumps(summary,ensure_ascii=False),flush=True)

if __name__=="__main__":
    main()
