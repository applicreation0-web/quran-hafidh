#!/usr/bin/env python3
"""Collect existing emulator evidence. Never build, sign, install or publish an APK."""
import csv, hashlib, json, os, re, statistics, subprocess, time, urllib.request, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

REPO="applicreation0-web/quran-hafidh"
BASE="d08a97cbe307219628125431af701e6cbc40e795"
ROOT=Path("evidence")
def api(path):
    req=urllib.request.Request("https://api.github.com/repos/"+REPO+"/"+path,
        headers={"Authorization":"Bearer "+os.environ["GH_TOKEN"],
                 "Accept":"application/vnd.github+json"})
    with urllib.request.urlopen(req,timeout=60) as response:
        return json.load(response)
def wait_jobs():
    ROOT.mkdir(exist_ok=True)
    for _ in range(160):
        nav=api("actions/runs/41246852263/jobs?per_page=100")["jobs"]
        taf=api("actions/runs/41249576279/jobs?per_page=100")["jobs"]
        wanted=[j for j in nav if "navigation" in j["name"]]+taf
        print("Terminal upstream jobs:",sum(j["status"]=="completed" for j in wanted),"/4",flush=True)
        if len(wanted)==4 and all(j["status"]=="completed" for j in wanted):
            (ROOT/"jobs.json").write_text(json.dumps(wanted,indent=2)+"\n")
            return
        time.sleep(30)
    raise SystemExit("Upstream jobs still unfinished; no completion claim")

def xml_summary(folder):
    cases={}
    for p in (ROOT/folder).rglob("TEST-*.xml"):
        tree=ET.parse(p)
        for case in tree.getroot().findall("testcase"):
            key=case.get("classname","")+"#"+case.get("name","")
            state="FAIL" if case.find("failure") is not None or case.find("error") is not None else (
                  "SKIP" if case.find("skipped") is not None else "PASS")
            cases.setdefault(key,set()).add(state)
    return {"unique_cases":len(cases),
            "passed":sum(v=={"PASS"} for v in cases.values()),
            "skipped":sum(v=={"SKIP"} for v in cases.values()),
            "failed_or_inconsistent":sum(bool(v-{"PASS","SKIP"}) or len(v)>1 for v in cases.values())}

def collect():
    subprocess.run(["git","diff","--exit-code",BASE,"HEAD","--",
       "hifz-core/src","hifz-app/src/main","hifz-app/src/private114","hifz-app/build.gradle.kts"],check=True)
    subprocess.run(["python3","scripts/verify_1179_invariants.py"],check=True)
    jobs=json.loads((ROOT/"jobs.json").read_text())
    rows=[]
    for surface,phase,run,group,expected in [
        ("phone","navigation",41246852263,"navigation-run",300),
        ("boox","navigation",41246852263,"navigation-run",300),
        ("phone","tafsir",41249576279,"final-tafsir-run",200),
        ("boox","tafsir",41249576279,"final-tafsir-run",200)]:
        folder=ROOT/group/("endurance-"+surface+"-"+phase+"-NO-APK")
        logs=list(folder.rglob("logcat.txt"))
        events=list(folder.rglob("events.txt"))
        summaries=list(folder.rglob("summary.json"))
        log=logs[0].read_text(errors="replace") if logs else ""
        event=events[0].read_text(errors="replace") if events else ""
        tag="navigation" if phase=="navigation" else "tafsir-close"
        parsed=[tuple(map(int,x)) for x in re.findall(
           "phase="+tag+r" completed=(\d+) uptime_ms=(\d+) pss_kib=(\d+) native_alloc_bytes=(\d+) java_used_bytes=(\d+)",log)]
        distinct={r[0]:r for r in parsed}
        ordered=[distinct[k] for k in sorted(distinct)]
        job=next(j for j in jobs if j["name"]=="endurance ("+surface+", "+phase+")")
        data=json.loads(summaries[0].read_text()) if summaries else {}
        valid_events="am_proc_start" in event and "com.quransafeguard.hifz.ibnkathirtest" in event
        bad=[line for line in event.splitlines()
             if re.search(r"am_(crash|anr)",line) and "com.quransafeguard.hifz.ibnkathirtest" in line]
        complete=set(distinct)==set(range(1,expected+1))
        result={"surface":surface,"phase":phase,"run_id":run,"job_id":job["id"],
                "job_conclusion":job["conclusion"],"completed":len(distinct),"expected":expected,
                "complete_sequence":complete,"usable_events":valid_events,
                "application_crash_anr_events":len(bad),"production_sha":BASE,
                "result":"PASS" if job["conclusion"]=="success" and complete and valid_events and not bad else "FAIL",
                "input_delivery":"WebView-local Android MotionEvent; no OS-injection endurance claim" if phase=="tafsir" else "Existing nested session navigation test"}
        if ordered:
            result.update(first_pss_kib=ordered[0][2],last_pss_kib=ordered[-1][2],
                peak_pss_kib=max(r[2] for r in ordered),
                first_native_bytes=ordered[0][3],last_native_bytes=ordered[-1][3],
                first_50_median_pss_kib=statistics.median(r[2] for r in ordered[:50]),
                last_50_median_pss_kib=statistics.median(r[2] for r in ordered[-50:]))
        if phase=="tafsir":
            result.update(tafsir_open_close=data.get("tafsir_open_close"),
                          rotations=data.get("rotations"),qcf_side_selections=data.get("qcf_side_selections"))
        with (ROOT/(surface+"-"+phase+"-memory.csv")).open("w") as out:
            writer=csv.writer(out);writer.writerow(["completed","uptime_ms","pss_kib","native_alloc_bytes","java_used_bytes"])
            writer.writerows(ordered)
        rows.append(result)
    baseline={"jvm":xml_summary("baseline-jvm"),"preserved_android":xml_summary("baseline-preserved")}
    summary={"status":"NO_GO_DELIVERY","production_sha":BASE,"rows":rows,"baseline":baseline,
             "note":"Collection success is not another independent test. Failed OS-injection attempts remain in the raw bundle.",
             "delivery_blockers":["System-injected touch endurance not certified",
                  "No genuine signed 1.17.9 in-place update proof","Physical BOOX acceptance not executed"],
             "private_assets":"7/7 exact SHA-256 locally verified before workspace became unavailable; no private asset uploaded",
             "signer_sha256":"66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14",
             "memory_limit":"PSS and heap observations do not prove universal absence of renderer/native leaks"}
    (ROOT/"final-summary.json").write_text(json.dumps(summary,indent=2)+"\n")
    files=[]
    for p in sorted(ROOT.rglob("*")):
        if not p.is_file(): continue
        assert p.suffix.lower() not in {".apk",".jks",".p12",".keystore"},p
        assert not any(part in {"atlas_private","ayah_themes_private"} for part in p.parts),p
        files.append({"path":str(p.relative_to(ROOT)),"sha256":hashlib.sha256(p.read_bytes()).hexdigest(),"bytes":p.stat().st_size})
    (ROOT/"MANIFEST_SHA256.json").write_text(json.dumps(files,indent=2)+"\n")
    bundle=Path("QURAN_HAAFIDH_1179_RAW_PROOFS_NO_APK.zip")
    with zipfile.ZipFile(bundle,"w",zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in sorted(ROOT.rglob("*")):
            if p.is_file():z.write(p,str(p.relative_to(ROOT)))
    print("HAAFIDH_PROOF_JSON="+json.dumps(summary,separators=(",",":")),flush=True)
    print("RAW_PROOF_BUNDLE_SHA256="+hashlib.sha256(bundle.read_bytes()).hexdigest(),flush=True)

if __name__=="__main__":
    import sys
    wait_jobs() if sys.argv[1]=="wait" else collect()
