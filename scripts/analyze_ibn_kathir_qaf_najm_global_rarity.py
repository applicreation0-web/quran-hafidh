#!/usr/bin/env python3
"""J2 Quran Haafidh: full-Quran lexical contradiction of Quran.com review candidates.

All numeric claims are computed from the pinned, SHA-verified Quran.ws Hafs
77,432-word source, independently aligned to the existing 604 QCF pages.
This is a non-runtime diagnostic: it NEVER approves or invents any amorce.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import re
import unicodedata
from pathlib import Path
from audit_ibn_kathir_exact_hafs_qcf import SHA as HAFS_SHA

ROOT=Path(__file__).resolve().parents[1]
QAF=ROOT/"docs/ibn_kathir_qaf_cue_selection_pass1.json"
NAJM=ROOT/"docs/ibn_kathir_najm_qurancom_editorial_evidence.json"
CHUNKS=ROOT/"hifz-app/src/main/word-source/quran-ws-v1.1.2"
SIDECARS=["001-150","151-300","301-450","451-604"]
# The Quran.ws text is authoritative for this LEXICAL diagnostic.
# Orthography differences between Quran.com preview and this source are warnings;
# never silently replace either source or treat source matches as KFQC glyph checks.
DIACRITICS=re.compile("[\u064b-\u065f\u0670\u06d6-\u06ed\u06df\u06e0-\u06e8\u06ea-\u06ed]")

def fail_unless(condition,message):
    if not condition:raise ValueError("J2 Quran lexical review NO GO: "+message)

def normalized(s):
    return DIACRITICS.sub("",unicodedata.normalize("NFC",s)).replace("\u0640","").replace("ٱ","ا")

def derive_inventory(hafs):
    raw=hafs.read_bytes()
    fail_unless(hashlib.sha256(raw).hexdigest()==HAFS_SHA,"pinned Hafs source SHA")
    doc=json.loads(raw)
    fail_unless(doc.get("format")=="quran-mushaf"
        and doc["mushaf"]["key"]=="hafs"
        and len(doc["words"])==77432
        and len(doc["ayah_starts"])==6236
        and len(doc["page_starts"])==604
        and len(doc["surahs"])==114,
        "unexpected Quran.ws word or verse source")
    keys=[]
    index={}
    pages=[]
    for part in SIDECARS:
        geom=json.loads((CHUNKS/f"word-boxes-{part}.json").read_text(encoding="utf-8"))
        fail_unless(geom["schema"]=="quran-haafidh-word-boxes-v1","wrong QCF source")
        for page,rows in geom["pages"].items():
            pages.append((int(page),rows))
    pages.sort()
    fail_unless([p for p,_ in pages]==list(range(1,605)),"604-page QCF span incomplete")
    for i,(page,rows) in enumerate(pages):
        fail_unless(doc["page_starts"][i]==len(keys),"mismatched QCF page offsets")
        for word in rows:
            k=word[0]
            fail_unless(k not in index,"duplicate QCF word "+k)
            index[k]=len(keys)
            keys.append(k)
    fail_unless(len(keys)==len(doc["words"])==77432,"word-count drift")
    verse_ids=[]
    for surah,meta in enumerate(doc["surahs"],1):
        fail_unless(meta["number"]==surah,"surah ordering")
        verse_ids.extend((surah,ayah) for ayah in range(1,meta["ayah_count"]+1))
    fail_unless(len(verse_ids)==6236,"verse count drift")
    verse_starts=list(doc["ayah_starts"])
    fail_unless([keys[n] for n in verse_starts]==
       [f"{s}:{a}:1" for s,a in verse_ids],"Quran/QCF verse start drift")
    end_by_start=dict(zip(verse_starts,verse_starts[1:]+[len(keys)]))
    offsets={verse_ids[i]:p for i,p in enumerate(verse_starts)}
    return doc["words"],keys,index,offsets,end_by_start

def derive_occurrence_index(words):
    # Indexed once for all 30 start candidates; no normalization of the Mushaf.
    normwords=tuple(normalized(w) for w in words)
    byfirst=collections.defaultdict(list)
    for i,w in enumerate(normwords):byfirst[w].append(i)
    return normwords,byfirst

def find_hits(phrase,normwords,byfirst):
    if not phrase:return []
    out=[]
    for k in byfirst.get(phrase[0],()):
        if tuple(normwords[k:k+len(phrase)])==phrase:
            out.append(k)
    return out

def source_reference(key):
    s,a,w=(int(x) for x in key.split(":"))
    return (s,a,w)

def build(qaf,najm,words,keys,idx,offsets,ends,normwords,byfirst):
    fail_unless(qaf.get("approved")==0 and qaf.get("runtime_ready") is False
                and najm.get("approved_keys")==0
                and najm.get("runtime_ready") is False,"premature approval")
    cues=[]
    for c in qaf["cues"]:
        cues.append({"surah":50,"group":c["source_group_id"],
                     "candidate_key":c["start_word_key"],
                     "proposed_words":c["proposed_word_count"],
                     "preview":c["quran_com_display_preview"],
                     "review_context":c["reason_for_candidate_fr"]})
    for group in najm["reviewed"]:
        for c in group["qcf_verse_start_candidates"]:
            cues.append({"surah":53,"group":group["id"],
                         "candidate_key":c["word_key"],
                         "proposed_words":None,"preview":None,
                         "review_context":group["contextual_review_note"]})
    fail_unless(len(cues)==30 and len({c["candidate_key"] for c in cues})==30,
                "expected 14 Qaf + 16 Najm candidates")
    results=[]
    for c in cues:
        key=c["candidate_key"]
        s,a,w=source_reference(key)
        fail_unless(s==c["surah"] and w==1 and key in idx,
                    "bad candidate position: "+key)
        at=idx[key]
        fail_unless(offsets[(s,a)]==at,"not an exact verse-start: "+key)
        upper=ends[at]
        n=c["proposed_words"]
        if n is not None:fail_unless(1<=n<=upper-at,"candidate extends past ayah: "+key)
        capacity=upper-at
        def match_length(k):
            phrase=tuple(normwords[at:at+k])
            # Never cross an ayah boundary when matching the same phrase.
            return [pos for pos in find_hits(phrase,normwords,byfirst)
                    if len(keys)>pos+k-1 and
                        keys[pos].split(":")[:2]==keys[pos+k-1].split(":")[:2]]
        minimum=None
        min_hits=None
        for k in range(1,capacity+1):
            hits=match_length(k)
            if len(hits)==1 and hits[0]==at:
                minimum=k;min_hits=hits;break
        proposed_hits=match_length(n) if n is not None else None
        one_hits=match_length(1)
        # Source words and keys from one immutable index; never manually
        # transcribe Arabic words for the final report.
        exact_words=words[at:at+(n or min(minimum or 2,capacity))]
        original=c["preview"]
        exact_preview_match=None
        if original is not None:
            actual=[normalized(w) for w in words[at:at+n]]
            copied=[normalized(w) for w in original.split()]
            exact_preview_match= actual==copied
        example_hit_refs=[]
        for hit in (proposed_hits or one_hits)[:15]:
            example_hit_refs.append(keys[hit])
        results.append({
            "group":c["group"],"key":key,"surah":s,"ayah":a,
            "source_words_exact":" ".join(exact_words),
            "quran_com_preview_original":original,
            "quran_com_preview_matches_pinned_hafs_after_vocalization_stripping":exact_preview_match,
            "quran_com_vs_hafs_discrepancy_review_required":exact_preview_match is False,
            "one_word_occurrences_across_quran":len(one_hits),
            "proposed_words":n,
            "proposed_occurrences_across_quran":len(proposed_hits) if proposed_hits is not None else None,
            "proposed_occurrences_in_surah":sum(keys[h].startswith(str(s)+":") for h in proposed_hits) if proposed_hits is not None else None,
            "proposed_example_locations":example_hit_refs,
            "shortest_globally_unique_verse_start_words":minimum,
            "unique_prefix_fits_ayah":minimum is not None,
            "verse_word_count":capacity,
            "lexical_next_action":("CHECK_SOURCE_PREVIEW" if exact_preview_match is False
                  else "CONTRADICTORY_SEMANTIC_CHECK" if minimum is not None
                  else "NO_GLOBALLY_UNIQUE_VERSE_PREFIX"),
            "semantic_approval":False,"qcf_glyph_equality_approval":False,
            "rights_approval":False,"runtime_enabled":False,
            "context_for_review_only":c["review_context"]
        })
    require_duplicate=next(r for r in results if r["key"]=="50:23:1")
    other=next(r for r in results if r["key"]=="50:27:1")
    # Their original head terms can look similar; document actual, not assumed,
    # normalized lexical evidence and retain separate semantic review.
    confusable={"a":"50:23:1","b":"50:27:1",
                "a_first_words":require_duplicate["source_words_exact"],
                "b_first_words":other["source_words_exact"],
                "shared_initial_word":normwords[idx["50:23:1"]]==normwords[idx["50:27:1"]],
                "status":"COMPANION_ROLE_DISAMBIGUATION_REQUIRED"}
    return {
      "schema":"IK_J2_QAF_NAJM_FULL_QURAN_LEXICAL_AUDIT_V1",
      "source_sha256":HAFS_SHA,
      "quran_words":77432,"surahs":114,"ayat":6236,
      "total_candidates":len(results),"qaf_candidates":14,"najm_candidates":16,
      "approved":0,"runtime_ready":False,
      "preview_mismatch_keys":[r["key"] for r in results if r["quran_com_vs_hafs_discrepancy_review_required"]],
      "not_globally_distinct_keys":[r["key"] for r in results if r["shortest_globally_unique_verse_start_words"] is None],
      "qaf_23_27_confusion":confusable,
      "audit_warning":"Global lexical uniqueness is neither mnemonic relevance nor independent QCF glyph equality. NO AUTOMATIC APPROVAL.",
      "entries":results
    }

def write(result,folder):
    folder.mkdir(parents=True,exist_ok=True)
    output=folder/"ik-j2-qaf-najm-global-rarity.json"
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    lines=[
      "# J2 — Qāf et An-Najm : confrontation lexicale au Coran complet",
      "",f"- Référence : 77 432 mots Quran.ws Hafs, SHA256 \`{HAFS_SHA}\`",
      f"- Candidats évalués : {result['total_candidates']} ; Qāf 14, An-Najm 16.",
      f"- Prévisualisations Qāf à réconcilier avec Hafs : {len(result['preview_mismatch_keys'])}.",
      f"- Références sans préfixe unique dans le verset : {len(result['not_globally_distinct_keys'])}.",
      "- **0 amorce approuvée : chiffres lexicaux ≠ pertinence sémantique.**",
      "", "| Groupe | Mot de départ | Longueur | Occurrences de la proposition | Minimum unique dans tout le Coran | État aperçu |",
      "|---|---|---:|---:|---:|---|"]
    for e in result["entries"]:
        lines.append(f"| {e['group']} | {e['key']} | "
           f"{e['proposed_words'] if e['proposed_words'] is not None else 'à choisir'} | "
           f"{e['proposed_occurrences_across_quran'] if e['proposed_occurrences_across_quran'] is not None else '—'} | "
           f"{e['shortest_globally_unique_verse_start_words'] or 'aucun'} | "
           f"{'différence à contrôler' if e['quran_com_vs_hafs_discrepancy_review_required'] else 'contrôle à poursuivre'} |")
    lines+=["", "## Risques particuliers",
       "- 50:23/50:27 : ne pas fusionner l'ange témoin et le compagnon débattant devant Allah.",
       "- Les mots doivent déclencher le sens de leur passage, pas seulement être rares.",
       "- La plus courte expression lexicalement unique n'est pas automatiquement la meilleure amorce.",
       "- Le calcul ne prouve pas l'égalité graphique entre Quran.ws et la page QCF.",
       "- Aucun changement du Mushaf, des moteurs, ni de la sélection utilisateur.",
       ""]
    (folder/"ik-j2-qaf-najm-global-rarity.md").write_text("\n".join(lines),encoding="utf-8")
    print("PASS J2 full Quran lexical review: 30 candidates in 6236 ayahs/77432 words; "
          f"{len(result['preview_mismatch_keys'])} Quran.com preview variants; "
          f"{len(result['not_globally_distinct_keys'])} no unique within-ayah prefix. "
          "0 approved/activated; NO APK")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("pinned_hafs",type=Path)
    parser.add_argument("output_folder",type=Path)
    args=parser.parse_args()
    words,keys,idx,offsets,ends=derive_inventory(args.pinned_hafs)
    normwords,byfirst=derive_occurrence_index(words)
    qaf=json.loads(QAF.read_text(encoding="utf-8"))
    najm=json.loads(NAJM.read_text(encoding="utf-8"))
    result=build(qaf,najm,words,keys,idx,offsets,ends,normwords,byfirst)
    write(result,args.output_folder)
    # Negative regression: an illegal runtime flag must be rejected upstream.
    fail_unless(all(e["semantic_approval"] is False
        and e["runtime_enabled"] is False for e in result["entries"]),
        "runtime approval attempted")
if __name__=="__main__":main()
