#!/usr/bin/env python3
"""1178 source freeze: preserve byte-exact completed Hifz motors, allow only family post-final read model."""
import pathlib, subprocess, sys
BASE="611144547a3b186042faabebfcee9b6441b343fb"
ROOT=pathlib.Path(__file__).resolve().parents[1]
P="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
def git(*args):return subprocess.check_output(["git",*args],cwd=ROOT,text=True)
# Reproduced on the real schema-6 backup: chronological learned IDs can be
# noncanonical, breaking passive revision outside the optional post-final flow.
# Permit ONLY the read-time ordering adapter; reconstruct every other byte of
# the protected 1.17.7 preference file, including defaults and credit engines.
prefs=(ROOT/(P+"HifzPrefs.java")).read_text(encoding="utf-8")
adapter='''        // Persisted learning order may cross sourahs or resume an older page.
        // Geometry requires physical order; canonicalize only this read view.
        ArrayList<String> physicalAppris=new ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++)
            if(appris.contains(geometry.line(i).id))physicalAppris.add(geometry.line(i).id);
        if(physicalAppris.size()!=appris.size())
            throw new IllegalStateException("Unknown learned physical line id");
'''
assert prefs.count(adapter)==1,"Missing exact passive read adapter"
assert prefs.count("            physicalAppris));")==1
reconstructed=prefs.replace(adapter,"").replace(
    "            physicalAppris));","            new ArrayList<>(appris)));")
assert reconstructed==git("show","c42bc5deca80012b982ec8ec6e5a658cd20484d6:"+P+"HifzPrefs.java"),"Unexpected protected preference change"
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
"PostFinalCumulativePolicy.java","PostFinalRecallStore.java","PostFinalFamilyCreditLedger.java","DashboardLedger.java","HifzDisplayVocabulary.java","Ui.java",
"SettingsActivity.java"]}
allowed.add(P+"HifzPrefs.java") # exact reconstruction enforced above
assert changed<=allowed,("UNAUTHORIZED MOTOR FILE CHANGED",changed-allowed)
print("PASS: frozen credit transitions, final receipts, rotation, backup, Quran geometry and masks")
print("PASS: HifzPrefs exact 1.17.7 reconstruction except reproduced passive read-order adapter")
print("PASS: limited 1.17.8 read-model changes",sorted(changed))
