#!/usr/bin/env python3
"""Semantic-source freeze of verified official 1.17.5 motors, independent of UI wording."""
import pathlib, subprocess, sys
BASE="611144547a3b186042faabebfcee9b6441b343fb"
ROOT=pathlib.Path(__file__).resolve().parents[1]
P="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
def git(*args):return subprocess.check_output(["git",*args],cwd=ROOT,text=True)
def check_same(path,begin,end):
    old=git("show",BASE+":"+path)
    now=(ROOT/path).read_text(encoding="utf-8")
    for src in (old,now):
        assert src.count(begin)==1 and src.count(end)>=1,(path,begin,"boundaries missing")
    before=old[old.index(begin):old.index(end,old.index(begin)+len(begin))]
    after=now[now.index(begin):now.index(end,now.index(begin)+len(begin))]
    assert before==after,("FROZEN MOTOR MODIFIED",path,begin)
for f,b,e in [
 ("HifzPrefs.java","    boolean completeLearningConsolidationSessionV6(","    public String lastLearningConsolidationDate()"),
 ("HifzPrefs.java","    boolean completeConsolidationSessionV6(","    /**\n     * Atomically closes a frozen 1..3 unit Sabqi"),
 ("HifzPrefs.java","    private void requireCurrentFinalSession(","    /** Atomically closes a frozen"),
 ("ConsolidationCycleEngine.java","    Session recordRepetition(","    Session closeSession("),
]:
    check_same(P+f,b,e)
for path in [
 P+"GeometryRepository.java",P+"HifzBackup.java",P+"MushafView.java",
 P+"ConsolidationPhysicalUnitPolicy.java",P+"ItqanMaintenancePolicy.java",
 P+"HifzBacklog.java",P+"SundayFinalPagePolicy.java",
 P+"ItqanRegimeStore.java"]:
    assert git("rev-parse",BASE+":"+path).strip()==git("hash-object",path).strip(),("FROZEN FILE MODIFIED",path)
changed=set(git("diff","--name-only",BASE,"HEAD","--",P).splitlines())
allowed={P+x for x in [
"MainActivity.java","HifzSessionActivity.java","WeeklyDashboardPlanner.java",
"PostFinalCumulativePolicy.java"]}
assert changed<=allowed,("UNAUTHORIZED MOTOR FILE CHANGED",changed-allowed)
print("PASS: frozen credit transitions, final receipts, rotation, backup, Quran geometry and masks")
print("PASS: limited 1.17.6 code changes",sorted(changed))
