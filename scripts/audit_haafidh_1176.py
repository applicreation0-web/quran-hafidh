#!/usr/bin/env python3
"""Release 1.17.6 source hygiene and E-Ink surface audit.
Scan production Java/Android XML, never erase historical persistence or serialized enum values.
No internet, repo mutations, or access to secret/private assets.
"""
import argparse, json, pathlib, re, collections, sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"hifz-app/src/main/java"
RES=ROOT/"hifz-app/src/main/res"
APP=ROOT/"hifz-app/src/main"
def java_literals(source):
    # String literals exclude comments (including source documentation) and escaped quotes.
    source=re.sub(r"/\*.*?\*/"," ",source,flags=re.S)
    source=re.sub(r"(?m)//[^\n]*$","",source)
    return re.findall(r'"(?:\\.|[^"\\])*"',source)
def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--strict",action="store_true")
    ap.add_argument("--report",default="")
    args=ap.parse_args()
    findings=[]
    files=sorted(JAVA.rglob("*.java"))
    allsource="\n".join(p.read_text(encoding="utf-8",errors="replace") for p in files)
    for path in files:
        raw=path.read_text(encoding="utf-8",errors="replace")
        for literal in java_literals(raw):
            if re.search(r"boule de neige",literal,re.I) and path.name!="HifzDisplayVocabulary.java":
                findings.append(dict(level="ERROR",kind="visible_legacy_label",path=str(path.relative_to(ROOT)),value=literal[:220]))
            if re.search(r"m[eé]morisation libre|\bentretien\b",literal,re.I):
                findings.append(dict(level="REVIEW",kind="ambiguous_vocabulary",path=str(path.relative_to(ROOT)),value=literal[:220]))
    # Neither delete a drawable referenced by XML, nor replace signed frozen small icon control assets.
    drawables={p.stem for d in ("drawable","drawable-anydpi","drawable-nodpi","drawable-hdpi","drawable-xhdpi")
               for p in (RES/d).glob("*") if p.is_file()}
    drawable_refs=set(re.findall(r"R\.drawable\.([A-Za-z0-9_]+)",allsource))
    missing=drawable_refs-drawables
    for name in sorted(missing):
        findings.append(dict(level="REVIEW",kind="drawable_generated_or_missing",name=name))
    # Dead code candidates are *only* candidates: JNI/reflection/XML tests may refer to a file.
    # No code deletion is permitted on lexical evidence alone.
    for path in files:
        symbol=path.stem
        references=len(re.findall(r"\b"+re.escape(symbol)+r"\b",allsource))
        if references<=1:
            findings.append(dict(level="REVIEW",kind="possibly_unused_java_class",path=str(path.relative_to(ROOT)),references=references))
    # Distinct finale pictograms and six standard BOOX Hifz route icons remain mandatory.
    main_java=(JAVA/"com/quransafeguard/hifz/preview/MainActivity.java").read_text(encoding="utf-8")
    for literal in ["ic_hifz_final_sabqi","ic_hifz_final_itqan","Révision cumulative Sabqi","Révision cumulative Itqān","Texte masqué","Texte visible"]:
        if literal not in main_java:
            findings.append(dict(level="ERROR",kind="missing_boox_semantic_contract",literal=literal))
    gradle=(ROOT/"hifz-app/build.gradle.kts").read_text(encoding="utf-8")
    for literal in ['versionCode = 44','versionName = "1.17.6"']:
        if literal not in gradle:
            findings.append(dict(level="ERROR",kind="invalid_release_version",literal=literal))
    # Post-final review MUST NOT masquerade as complete when new functional delivery is absent.
    has_postfinal="PostFinalRecallStore" in allsource and "renderPostFinalRecall()" in allsource
    findings.append(dict(level="INFO",kind="postfinal_masked_20_pages_implemented",value=has_postfinal))
    report={
        "version":"1.17.6","production_java_files":len(files),"drawable_refs":len(drawable_refs),
        "drawable_resource_names":len(drawables),"counts":dict(collections.Counter(f["level"] for f in findings)),
        "findings":findings
    }
    if args.report:
        target=pathlib.Path(args.report);target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False,indent=2))
    if args.strict and any(f["level"]=="ERROR" for f in findings):sys.exit(1)
if __name__=="__main__": main()
