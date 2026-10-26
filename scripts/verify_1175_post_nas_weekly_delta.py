#!/usr/bin/env python3
"""Fail-closed additional user-authorized 26 October BOOX/Final icons/post-Nas weekly delta.

This never widens 1.17.1 freeze or removes previous validation contracts.
Review requires exact original/new Git blobs, only five listed Java files, and
byte identity of all historical finale and Itqan credit paths.
"""
from pathlib import Path
import subprocess

BASE="bb9483d3d1338d0a69d4a680913400de3c6100e6"
ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
PINS={
    "HifzPrefs.java":("3c8a9b63c3f8835b57ea80a2059539d50469d1fe","2179399558c89a5f5c8db896cbdb6f81b147e065"),
    "HifzSessionActivity.java":("521ba4db317b18e696e34a340dbdf11147682ea6","20eea00f1c8213c8ce84a8894c7ae2d4acc6185e"),
    "ConsolidationCycleEngine.java":("f5836c553436494249febf9415be9f966f060197","40b871eabb0b9dde10dd2e4e2a8ee1bca2fdd45c"),
    "MainActivity.java":("8e329bb967da7d69bf92d99f24aad98d88376e69","792d78924d87abd839b0cfb3830b1a9061db4a5b"),
    "WeeklyDashboardPlanner.java":("8f004d4d08de7aa8f0307e826d91f3e4217dde10","659cff9d702474081fdd21df1d25e4b3dc88d924"),
}
def git(*args):
    return subprocess.check_output(["git",*args],text=True).strip()
changed=set(git("diff","--name-only",BASE,"HEAD","--",ROOT).splitlines())
expected={ROOT+file for file in PINS}
assert changed==expected, "Unauthorized main Hifz sources: "+str(changed^expected)
for name,(before,after) in PINS.items():
    path=ROOT+name
    assert git("rev-parse",BASE+":"+path)==before,"Untrusted baseline: "+name
    assert git("hash-object",path)==after,"Unreviewed additional Hifz edit: "+name

def unchanged(name,start,end):
    path=ROOT+name
    old=git("show",BASE+":"+path)
    current=Path(path).read_text(encoding="utf-8")
    assert old.count(start)==1 and old.count(end)==1
    assert current.count(start)==1 and current.count(end)==1
    one=old[old.index(start):old.index(end,old.index(start))]
    two=current[current.index(start):current.index(end,current.index(start))]
    assert one==two,"Historic frozen method modified: "+name+" "+start

for start,end in [
    ("    private void renderLearningFinalReview() {","    private void validateLearningFinalReview() {"),
    ("    private void renderConsolidationFinalReview() {","    private void validateConsolidationFinalReview() {"),
    ("    private void validateItqan(){","    private void restartItqanAfterAssistance() {"),
    ("    private void showSundayFinalLectureMaskPage() {","    /** True when an anchored post-An-Nās page"),
]:
    unchanged("HifzSessionActivity.java",start,end)
for start,end in [
    ("    boolean completeConsolidationSessionV6(","    boolean completeLearningConsolidationSessionV6("),
    ("    boolean completeLearningConsolidationSessionV6(","    public String lastLearningConsolidationDate()"),
]:
    unchanged("HifzPrefs.java",start,end)
prefs=Path(ROOT+"HifzPrefs.java").read_text(encoding="utf-8")
session=Path(ROOT+"HifzSessionActivity.java").read_text(encoding="utf-8")
engine=Path(ROOT+"ConsolidationCycleEngine.java").read_text(encoding="utf-8")
ui=Path(ROOT+"MainActivity.java").read_text(encoding="utf-8")
for source,items in [
    (prefs,["postNasMaintainedWeekAnchorV1","postNasMaintainedHizbsV1",
        "postNasConsolidationCompletedWeekSnapshotV1","postNasConsolidationPending(date)",
        "postNasConsolidationUnits(today)","POST_NAS_WEEKLY_MASKED",
        "Maintien units changed during weekly consolidation",
        "consolidationFinalStateKey(ConsolidationCycleEngine.Family.LEARNING)",
        "consolidationFinalStateKey(ConsolidationCycleEngine.Family.STABILIZATION)"]),
    (session,["Consolidation Itqān · hizb ","POST_NAS_WEEKLY_MASKED",
        "showSundayFinalLectureMaskPage()","sundayFinalMaskedPageReady",
        "applyItqanAnchors()"]),
    (engine,["case POST_NAS_WEEKLY_MASKED:","return copy(1, 0, 0, 0, 0);",
        "Protocol.SUNDAY_PAGE","Protocol.SNOWBALL_EXTENDED"]),
    (ui,["ic_hifz_final_sabqi","ic_hifz_final_itqan",
        "AUJOURD’HUI · ","Révision cumulative Sabqi","Révision cumulative Itqān"]),
]:
    for phrase in items: assert phrase in source, "Missing user-authorized contract "+phrase

for name in ["ic_hifz_final_sabqi.xml","ic_hifz_final_itqan.xml"]:
    path="hifz-app/src/main/res/drawable/"+name
    assert Path(path).is_file(),"Missing final icon "+name
print("PASS: precisely SHA-pinned new BOOX UI and post-Nas masked-weekly delta")
print("PASS: previous Sabqi/Itqan finale and credit closure methods byte-identical")
